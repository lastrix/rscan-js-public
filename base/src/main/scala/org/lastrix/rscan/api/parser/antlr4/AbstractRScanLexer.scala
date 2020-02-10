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

package org.lastrix.rscan.api.parser.antlr4

import org.antlr.v4.runtime.{CharStream, Lexer, Token, Vocabulary}
import org.lastrix.rscan.model.tokens.RScanToken
import org.lastrix.rscan.vfs.VirtualFile

import scala.collection.mutable

abstract class AbstractRScanLexer(stream: CharStream, val file: VirtualFile)
  extends Lexer(stream) {
  private val path = file.virtualPath
  private lazy val tokenTypeMap = AbstractRScanLexer.tokenTypeMapForVocabulary(getVocabulary)
  private var _templateMode: Boolean = false
  private val tokenQueue = mutable.Queue[Token]()

  final def templateMode: Boolean = _templateMode

  final def templateMode(value: Boolean): Unit = _templateMode = value

  def foldOpen: Int

  def foldClose: Int

  final val foldBlock: Int = tokenType("FoldBlock")

  def tokenType(name: String): Int =
    tokenTypeMap.getOrElse(name, Token.INVALID_TYPE)

  def buildFoldToken(start: RScanToken): RScanToken = {
    val (token, list) = createFoldBlockBody(
      mutable.MutableList[RScanToken]()
        ++ List(start.asInstanceOf[RScanToken])
    )
    val result = new RScanToken(path, _tokenFactorySourcePair, foldBlock, Lexer.DEFAULT_TOKEN_CHANNEL, start.getStartIndex, token.getStopIndex)
    result.setLine(start.getLine)
    result.setCharPositionInLine(start.getCharPositionInLine)
    result.setFoldedTokens(list)
    result
  }

  private def createFoldBlockBody(list: mutable.MutableList[RScanToken]): (RScanToken, mutable.MutableList[RScanToken]) = {
    var current: RScanToken = null
    while (current == null || current.getType != Token.EOF && current.getType != foldClose) {
      current = nextToken().asInstanceOf[RScanToken]
      list += current
    }
    if (current.getType == Token.EOF)
      throw new IllegalStateException()
    (current, list)
  }

  override def nextToken(): Token = {
    if (hasEnqueuedTokens) dequeueToken
    else super.nextToken() match {
      case x: RScanToken if x.getType == foldOpen => buildFoldToken(x)
      case x: Token => x
    }
  }

  final def isStartOfFile: Boolean = getCharPositionInLine == 0 && getLine == 1

  protected final def queueToken(token: Token): Unit = tokenQueue.enqueue(token)

  protected final def hasEnqueuedTokens: Boolean = tokenQueue.nonEmpty

  protected final def dequeueToken: Token = tokenQueue.dequeue()
}

private object AbstractRScanLexer {
  private val tokenTypeMap = mutable.HashMap[Vocabulary, Map[String, Int]]()

  def clear(): Unit = tokenTypeMap.clear()

  def tokenTypeMapForVocabulary(vocabulary: Vocabulary): Map[String, Int] = {
    tokenTypeMap.synchronized {
      val map = tokenTypeMap.get(vocabulary)
      map match {
        case Some(map) => map
        case None =>
          val im = buildTokenTypeMap(vocabulary)
          tokenTypeMap.put(vocabulary, im)
          im
      }
    }
  }

  private def buildTokenTypeMap(vocabulary: Vocabulary): Map[String, Int] = {
    val m = mutable.HashMap[String, Int]()
    for (i <- 0 until vocabulary.getMaxTokenType) {
      val name = vocabulary.getLiteralName(i)
      if (name != null) m.put(name, i)

      val sName = vocabulary.getSymbolicName(i)
      if (sName != null) m.put(sName, i)
    }

    m.put("EOF", Token.EOF)
    m.toMap
  }
}
