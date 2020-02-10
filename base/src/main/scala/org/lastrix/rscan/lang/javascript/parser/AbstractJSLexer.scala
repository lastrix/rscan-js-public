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

package org.lastrix.rscan.lang.javascript.parser

import org.antlr.v4.runtime._
import org.lastrix.rscan.api.parser.antlr4.AbstractRScanLexer
import org.lastrix.rscan.vfs.VirtualFile

abstract class AbstractJSLexer
(stream: CharStream, file: VirtualFile)
  extends AbstractRScanLexer(stream, file) {

  private var lastToken: Token = _
  private var insideTemplate = false
  private var closePosition = -1

  override def nextToken(): Token =
    if (hasEnqueuedTokens) dequeueToken
    else if (templateMode) nextTokenTemplate
    else nextTokenNonTemplate

  private def nextTokenNonTemplate: Token = {
    val token = _input.LA(1) match {
      case '`' => parseTemplateLiteral
      case _ => super.nextToken()
    }
    if (token.getChannel == Token.DEFAULT_CHANNEL)
      lastToken = token
    token
  }

  private def nextTokenTemplate: Token =
    if (!insideTemplate) {
      if (_input.LA(1) != '`')
        throw new IllegalStateException("Template string must start with '`' character")
      insideTemplate = true
      val token = createToken(_input.index(), 1, templateLiteralDelimiter, Token.HIDDEN_CHANNEL)
      templateStartToken()
      token
    } else if (_input.index() == closePosition) {
      if (_input.LA(1) != '}')
        throw new IllegalStateException("Close position is not at '}' character")
      val token = createToken(_input.index(), 1, templateLiteralDelimiter, Token.HIDDEN_CHANNEL)
      templateMiddleOrStopToken()
      token
    } else nextTokenNonTemplate

  def isRegexStart: Boolean = _input.LA(1) == '/' && _input.LA(2) != '*' &&
    (lastToken == null || !REGEXP_DISABLED_TOKENS.contains(getVocabulary.getSymbolicName(lastToken.getType)))

  def tryParseRegExp: Token = {
    val start = _input.index()
    val regexEndIndex = findRegExpSize(1)
    if (regexEndIndex == -1) null
    else createToken(start, regexEndIndex, regExpLiteral)
  }

  private def findRegExpSize(start: Int): Int = {
    var escaped = false
    var regexEndIndex = start
    val regex = new StringBuilder
    var done = false
    regex.append('/')

    while (!done) {
      regexEndIndex += 1

      if (regexEndIndex >= REGEXP_SIZE_LIMIT)
        return -1

      val cCh = _input.LA(regexEndIndex)
      if (cCh == IntStream.EOF)
        return -1

      regex.append(cCh.toChar)
      if (escaped)
        escaped = false
      else if (cCh == '\r' || cCh == '\n')
        return -1
      else if (cCh == '\\')
        escaped = true
      else if (cCh == '/')
        done = true
      else if (cCh == '[')
        regexEndIndex = consumeRegExGroup(regexEndIndex, regex)
    }

    done = false
    var allowFlags = true
    while (!done) {
      regexEndIndex += 1

      val cCh = _input.LA(regexEndIndex)
      if (cCh == IntStream.EOF) {
        allowFlags = false
        done = true
      } else if (!allowFlags || "gimy".indexOf(cCh.toChar) == -1) {
        allowFlags = false
        done = true
      }
    }

    val regexpStr = regex.toString()
    if (regexpStr.contains("var ") || regexpStr.contains("function("))
      return -1

    val endIdx = regexpStr.lastIndexOf('/')
    if (endIdx < 2) -1
    else regexEndIndex - start + 1
  }

  private def consumeRegExGroup(idx: Int, regex: StringBuilder): Int = {
    var pIdx = idx
    var escaped = false
    while (true) {
      pIdx += 1

      if (pIdx >= REGEXP_SIZE_LIMIT)
        return pIdx

      val cCh = _input.LA(pIdx)
      if (cCh == IntStream.EOF)
        return pIdx

      if (cCh == '[' && !escaped)
        regex.append("\\")
      regex.append(cCh.toChar)
      if (escaped)
        escaped = false
      else if (cCh == '\\')
        escaped = true
      else if (cCh == '\r' || cCh == '\n')
        return pIdx - 1
      else if (cCh == ']')
        return pIdx
    }
    pIdx
  }

  private val regExpLiteral = tokenType("RegexpLiteral")
  private val templateLiteral = tokenType("TemplateLiteral")
  private val templateLiteralStart = tokenType("TemplateLiteralStart")
  private val templateLiteralMiddle = tokenType("TemplateLiteralMiddle")
  private val templateLiteralStop = tokenType("TemplateLiteralStop")
  private val templateLiteralDelimiter = tokenType("TemplateLiteralDelimiter")

  val foldOpen: Int = tokenType("LBRACE")

  val foldClose: Int = tokenType("RBRACE")

  private val REGEXP_DISABLED_TOKENS = Set(
    "Identifier",
    "TARGET",
    "UNDEFINED",
    "FROM",
    "OF",
    "AS",
    "IN",
    "NullLiteral",
    "BooleanLiteral",
    "KW_THIS",
    "DecimalLiteral",
    "HexIntLiteral",
    "BinIntLiteral",
    "OctIntLiteral",
    "BigHexIntLiteral",
    "BigOctIntLiteral",
    "BigBinIntLiteral",
    "BigDecIntLiteral",
    "StringLiteral",
    "RBRACK",
    "RPAREN"
  )

  private val REGEXP_SIZE_LIMIT: Int = 256

  /**
   * Parse template literal expression content,
   * it's possible that inside expression someone placed
   * a ton of shitcode with nested template literals, good luck!
   *
   * @return the template literal token object
   */
  private def parseTemplateLiteral: Token = {
    val start = _input.index()
    createToken(start, parseTemplateString(), templateLiteral)
  }

  private def parseTemplateString(startSize: Int = 1): Int = {
    var inside = true
    var size = startSize
    var escape = false
    while (inside) _input.LA(size + 1) match {
      case '`' if !escape => inside = false; size += 1
      case '$' if !escape && _input.LA(size + 2) == '{' => size = parseTemplateExpression(size + 2); escape = false
      case '\\' if !escape => size += 1; escape = true
      case -1 =>
        throw new IllegalStateException()
      case _ => size += 1; escape = false
    }
    size
  }

  def parseTemplateExpression(startSize: Int): Int = {
    var braceLevel = 0
    var size = startSize
    var inside = true
    // TODO: regular expression may cause havoc
    while (inside) _input.LA(size + 1) match {
      case '{' => braceLevel += 1; size += 1
      case '}' => if (braceLevel == 0) inside = false else braceLevel -= 1; size += 1
      case '`' => size = parseTemplateString(size + 1)
      case '/' if _input.LA(size + 2) == '/' && _input.LA(size) != '\\' => size = parseLineComment(size + 2)
      case '/' if _input.LA(size + 2) == '*' => size = parseMLineComment(size + 2)
      case '/' =>
        val regExpSize = findRegExpSize(size + 1)
        if (regExpSize == -1) size += 1
        else size += regExpSize

      case x: Int if x == '\'' || x == '"' => size = parseStringLiteral(size + 1, x)
      case -1 => throw new IllegalStateException()
      case _ => size += 1
    }
    size
  }

  private def parseLineComment(startSize: Int): Int = {
    var size = startSize
    var inside = true
    while (inside) _input.LA(size + 1) match {
      case '\r' => inside = false; size += (if (_input.LA(size + 2) == '\n') 3 else 2)
      case '\n' => inside = false; size += 2
      case _ => size += 1
    }
    size
  }

  private def parseMLineComment(startSize: Int): Int = {
    var size = startSize
    var inside = true
    while (inside) _input.LA(size + 1) match {
      case '*' if _input.LA(size + 2) == '/' => size += 3; inside = false
      case _ => size += 1
    }
    size
  }

  private def parseStringLiteral(startSize: Int, stopCh: Int): Int = {
    if (startSize < 0)
      throw new IllegalArgumentException
    var escape = false
    var size = startSize
    var inside = true
    while (inside) {
      _input.LA(size + 1) match {
        case x: Int if x == stopCh && !escape => inside = false
        case '\\' if !escape => escape = true
        case -1 =>
          throw new IllegalStateException
        case _ => escape = false
      }
      size += 1
    }
    size
  }

  private def templateStartToken(): Unit = {
    val size = parseTemplateTillExprOrEnd
    queueToken(createToken(_input.index(), size, templateLiteralStart))
    queueToken(createToken(_input.index(), if (_input.LA(1) == '$') 2 else 1, templateLiteralDelimiter, Token.HIDDEN_CHANNEL))
    if (_input.LA(-1) == '`') closePosition = -1
    else closePosition = _input.index() + parseTemplateExpression(0) - 1
  }

  private def parseTemplateTillExprOrEnd: Int = {
    var inside = true
    var size = 0
    var escape = false
    while (inside) _input.LA(size + 1) match {
      case '`' if !escape => inside = false;
      case '$' if !escape && _input.LA(size + 2) == '{' => inside = false;
      case '\\' if !escape => size += 1; escape = true
      case -1 => throw new IllegalStateException
      case _ => size += 1; escape = false
    }
    size
  }

  private def templateMiddleOrStopToken(): Unit = {
    val size = parseTemplateTillExprOrEnd
    val tokenType = if (_input.LA(size + 1) == '$') templateLiteralMiddle else templateLiteralStop
    queueToken(createToken(_input.index(), size, tokenType))
    queueToken(createToken(_input.index(), if (tokenType == templateLiteralMiddle) 2 else 1, templateLiteralDelimiter, Token.HIDDEN_CHANNEL))
    if (tokenType == templateLiteralMiddle)
      closePosition = _input.index() + parseTemplateExpression(0) - 1
  }

  private def createToken(start: Int, size: Int, tokenType: Int, channel: Int = Token.DEFAULT_CHANNEL): Token = {
    val sb = new StringBuilder
    for (_ <- 0 until size) {
      sb.append(_input.LA(1).toChar)
      _input.consume()
    }
    val text = sb.toString()
    getTokenFactory.create(
      _tokenFactorySourcePair,
      tokenType,
      text,
      channel,
      start,
      _input.index(),
      _tokenFactorySourcePair.a.getLine,
      _tokenFactorySourcePair.a.getCharPositionInLine
    )
  }

}
