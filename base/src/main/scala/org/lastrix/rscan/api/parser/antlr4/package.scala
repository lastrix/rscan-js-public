/*
 * Copyright (C) 2019-2020.  RScan-js-public project
 *
 * This file is part of RScan-js-public project.
 *
 * RScan-js-public is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * RScan-js-public is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RScan-js-public.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.lastrix.rscan.api.parser

import java.io.File
import java.time.{Duration, Instant}
import java.util

import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.PredictionMode
import org.lastrix.rscan.model.literal.LiteralType
import org.lastrix.rscan.model.operation._
import org.lastrix.rscan.model.operation.std._
import org.lastrix.rscan.vfs.VirtualFile
import org.slf4j.LoggerFactory

import scala.collection.{JavaConverters, mutable}

package object antlr4 {
  private val log = LoggerFactory.getLogger(getClass)
  private val enableParallel = java.lang.Boolean.parseBoolean(System.getProperty("rscan.parser.parallel", "false"))
  private val enableProfiling = java.lang.Boolean.parseBoolean(System.getProperty("rscan.parser.profiling", "false"))
  private val cacheDir = {
    val dirName = System.getProperty("rscan.parser.caching.folder")
    if (dirName == null) None
    else {
      val dir = new File(dirName)
      if (!dir.exists() && !dir.mkdirs() || !dir.isDirectory) throw new IllegalStateException("Unable to mkdirs: " + dir)
      Some(dir)
    }
  }

  /**
   * Parse file using supplied ParserService, no language recognition or any other
   * analysis is performed.
   *
   * @param file    the input file to read source code from
   * @param config  the parser configuration
   * @param service the parser service for lexer and parser creation
   * @return RLangOperation - the root of parsed file operation tree
   */
  def parseFile(file: VirtualFile, config: ParserConfig = ParserConfig())(implicit service: ParserService): RLangOp =
    parseText(file, None, config)

  /**
   * Parse file part represented as optional text parameter. If no text supplied,
   * then file reading occur.
   *
   * @param file    the input file to read source code from if text is None
   * @param text    the text to parse
   * @param config  the parser configuration, it's recommended to set offsets if text is not None
   * @param service the parser service for lexer and parser creation
   * @return RLangOperation - the root of parsed source code
   */
  def parseText(file: VirtualFile, text: Option[String], config: ParserConfig = ParserConfig())(implicit service: ParserService): RLangOp = {
    parseTextNoCaching(file, text, config)
  }

  private def targetFileFor(file: VirtualFile, dir: File): File = {
    val f = new File(dir, file.absoluteName)
    val p = f.getParentFile
    if (!p.exists() && !p.mkdirs() || !p.isDirectory) throw new IllegalStateException("Unable to mkdirs: " + p)
    f
  }

  private def parseTextNoCaching(file: VirtualFile, text: Option[String], config: ParserConfig = ParserConfig())(implicit service: ParserService): RLangOp = {
    val start = Instant.now()
    val parser =
      if (enableParallel) new ParallelParser(service, file, config, text)
      else new SequentialParser(service, file, config, text)
    val root = parser.parse()
    val elapsed = Duration.between(start, Instant.now())
    val msg = config.additionalSuccessMsg match {
      case Some(x) => x
      case None => ""
    }
    log.info(s"Source from ${file.virtualPath} parsed successfully in ${elapsed.toMillis} ms$msg")
    root
  }

  /**
   * Abstract for parsing source code.
   * The main goal of whole process is to split source code via FoldBlock tokens or template strings
   * into chunks, thus allowing to narrow lookahead of parser significantly.
   *
   * Two approaches to handle children jobs available via SequentialParser and ParallelParser,
   * the main difference between them: first one enqueues jobs into single queue and then pulls
   * till queue is empty, the other one overrides #enqueue method to call #parseItem in parallel mode
   */
  private trait Parser {
    def parserService: ParserService

    def config: ParserConfig

    def file: VirtualFile

    def text: Option[String] = None

    private val jobs: mutable.Queue[ROp] = mutable.Queue[ROp]()

    private def enqueue(list: Seq[ROp]): Unit = jobs.synchronized {
      jobs ++= list
    }

    private def hasJobs: Boolean = jobs.synchronized {
      jobs.nonEmpty
    }

    final def parse(): RLangOp = {
      val result = parseInitial()
      enqueue(result.children)
      while (hasJobs) {
        val jobProxy = jobs.synchronized {
          val _j = Seq.empty ++ jobs
          jobs.clear()
          _j
        }
        parseItems(jobProxy)
      }
      result.op.asInstanceOf[RLangOp]
    }

    private def parseInitial(): ParsedResult = {
      val lexer = parserService.newLexer(file, text, config.offsetLine, config.offsetLinePosition)
      val stream = new CommonTokenStream(lexer)
      stream.fill()
      config.onLexerComplete(lexer)
      val parserCallback: AbstractRScanParser => ROp = config.initialRule match {
        case Some(rule) => _.invokeRule[ROp](rule)
        case None => _.invokeDefaultRule[RLangOp]
      }
      parseTokens(stream, parserCallback)
    }

    protected def parseItems(items: Seq[ROp]): Unit = for (item <- items) parseItem(item)

    protected final def parseItem(item: ROp): Unit = enqueue(parseOperation(item).children)

    private def parseOperation(op: ROp): ParsedResult = op match {
      case x: RFoldOp =>
        if (x.parent == null) {
          log.trace("Unable to process parentless fold block: " + x.key)
          ParsedResultEmpty
        }
        else parseTokens(asTokenStream(x), p => {
          p.options(x.token.options)
          val o = p.invokeRuleFor(x)
          x.parent.synchronized {
            x.parent.replaceChild(x, o)
          }
          o
        })
      case x: RLiteralOp if x.literal.`type` == LiteralType.TEMPLATE =>
        parseTokens(templateTokenStream(file, x), p => {
          x.findChild(p => p.`type` == StdOpType.PROPS) match {
            case Some(c: RPropsOp) => p.options(c.props)
            case _ =>
          }
          val o = p.invokeTemplateRule[ROp]
          x.children(Array(o))
          o
        })
      case _ => throw new IllegalArgumentException("Only RFoldOperation or RLiteralOperation[TEMPLATE] permitted")
    }

    private def asTokenStream(op: RFoldOp): TokenStream =
      new CommonTokenStream(new ListTokenSource(asArrayList(op)))

    private def asArrayList(op: RFoldOp): util.ArrayList[Token] =
      new util.ArrayList[Token](JavaConverters.asJavaCollection(op.token.foldedTokens()))

    private def templateTokenStream(file: VirtualFile, x: RLiteralOp): TokenStream = {
      val lexer = parserService.newLexer(
        file,
        Some(x.literal.asString),
        x.statement.startLine,
        x.statement.startLinePosition)
      lexer.templateMode(true)
      new CommonTokenStream(lexer)
    }

    private def parseTokens(tokenStream: TokenStream, parse: AbstractRScanParser => ROp): ParsedResult = {
      val parser = parserService.newParser(file, tokenStream)
      parser.setBuildParseTree(false)
      parser.getInterpreter.setPredictionMode(PredictionMode.SLL)
      parser.removeErrorListeners()
      parser.addErrorListener(new RScanErrorListener(file, tokenStream))
      parser.setProfile(enableProfiling)
      val root = parse(parser)
      if (enableProfiling) printProfileInfo(parser, tokenStream)
      ParsedResult(root, parser.folds, parser.templates)
    }

    ////////////////////// Profiling support /////////////////////////////////////////////////////////////////////////////
    // TODO: refactoring required, make messages more versatile
    private def printProfileInfo(parser: AbstractRScanParser, tokenStream: TokenStream): Unit = {
      // do the actual parsing
      val parseInfo = parser.getParseInfo
      val atn = parser.getATN
      for (di <- parseInfo.getDecisionInfo if di.ambiguities.size() > 0) {
        val ds = atn.decisionToState.get(di.decision)
        val ruleName = parser.ruleName(ds.ruleIndex)
        log.debug("Ambiguity in rule '" + ruleName + "' -> {}", di)
        log.debug("=========================")
        log.debug(tokenStream.getText)
        log.debug("=========================")
      }
    }

  }

  private sealed case class ParsedResult(op: ROp, folds: List[RFoldOp], templates: List[RLiteralOp]) {
    def children: Seq[ROp] = Seq.empty ++ folds ++ templates
  }

  private val ParsedResultEmpty: ParsedResult = ParsedResult(null, List.empty, List.empty)

  private sealed class SequentialParser
  (override val parserService: ParserService,
   override val file: VirtualFile,
   override val config: ParserConfig,
   override val text: Option[String] = None)
    extends Parser

  private sealed class ParallelParser
  (override val parserService: ParserService,
   override val file: VirtualFile,
   override val config: ParserConfig,
   override val text: Option[String] = None)
    extends Parser {
    override protected def parseItems(items: Seq[ROp]): Unit = items.par.foreach(parseItem)
  }

}
