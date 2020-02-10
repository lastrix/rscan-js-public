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

import org.antlr.v4.runtime.{Token, TokenStream}
import org.lastrix.rscan.api.parser.antlr4.AbstractRScanParser
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.lang.javascript.meta.JSLanguage
import org.lastrix.rscan.lang.javascript.parser.model.operation.{RJsxAttrOp, RJsxOp}
import org.lastrix.rscan.model.literal._
import org.lastrix.rscan.model.operation.ROp
import org.lastrix.rscan.model.operation.std.RPropsOp
import org.lastrix.rscan.vfs._

abstract class AbstractJSParser
(stream: TokenStream, file: VirtualFile)
  extends AbstractRScanParser(stream, file) {

  override def language: LanguageRef = JSLanguage.ref

  def isStrictMode(): Boolean = false // TODO: strict mode

  ///////////////////////////////////////// Rule operations ////////////////////////////////////////////////////////////
  override val defaultRule: String = {
    val name = file.name.toLowerCase
    if (name.endsWith(".js") || name.endsWith(".jsx")) "startJavaScript"
    else throw new UnsupportedOperationException
  }

  override val templateRule: String = "startTemplate"

  //////////////////////////////////////// Token operations ////////////////////////////////////////////////////////////
  private val kwFunctionType = tokenType("KW_FUNCTION")
  private val kwClassType = tokenType("KW_CLASS")
  private val asyncType = tokenType("ASYNC")
  private val lBraceType = tokenType("LBRACE")
  private val rBraceType = tokenType("RBRACE")
  private val lBrackType = tokenType("LBRACK")
  private val letType = tokenType("LET")
  private val lessThan = tokenType("LT")

  def checkLineTerm: Boolean = {
    var ct = _input.get(getCurrentToken.getTokenIndex - 1)
    while (ct.getChannel != Token.DEFAULT_CHANNEL) ct = _input.get(ct.getTokenIndex - 1)
    ct.getLine < getCurrentToken.getLine
  }

  def checkNoLineTerm: Boolean = {
    var ct = _input.get(getCurrentToken.getTokenIndex - 1)
    while (ct.getChannel != Token.DEFAULT_CHANNEL) ct = _input.get(ct.getTokenIndex - 1)
    ct.getLine == getCurrentToken.getLine
  }

  def checkLBrace: Boolean = _input.LT(1).getType == lBraceType

  def checkRBrace: Boolean = _input.LT(1).getType == rBraceType

  def checkFunction: Boolean = _input.LT(1).getType == kwFunctionType

  def checkLBrack: Boolean = _input.LT(1).getType == lBrackType

  def checkLet: Boolean = _input.LT(1).getType == letType

  def checkLetLBrack: Boolean = _input.LT(1).getType == letType && _input.LT(2).getType == lBrackType

  def checkNoWs(count: Int): Boolean = {
    var i = 1
    while (i != -1 && i < count) {
      val a = _input.LT(i)
      val b = _input.LT(i + 1)
      if (a.getCharPositionInLine + a.getText.length != b.getCharPositionInLine)
        i = -1
      else
        i += 1
    }
    i != -1
  }

  def checkNoNewline(token: Token): Boolean = token.getLine == _input.LT(1).getLine

  def checkNoLt: Boolean = _input.LT(1).getType != lessThan

  val jsxAllowed: Boolean = true // TODO: actually checks must be present

  //////////////////////////////////////// JSX /////////////////////////////////////////////////////////////////////////
  def opJsx(start: Token, stop: Token, tagName: String, children: java.util.List[ROp]): ROp =
    RJsxOp(evalStatement(start, stop), tagName, asArray(children))

  def opJsxAttr(start: Token, stop: Token, attrName: String, children: java.util.List[ROp]): ROp =
    RJsxAttrOp(evalStatement(start, stop), attrName, asArray(children))

  //////////////////////////////////////// Literal operation processing ////////////////////////////////////////////////
  def boolLiteral(token: Token): ROp =
    if ("true".equalsIgnoreCase(token.getText)) trueLiteral(token)
    else falseLiteral(token)

  def bigNumLiteral(negate: Boolean, token: Token, radix: Int = 10): ROp = {
    // TODO: parse numbers
    opLiteral(token, new RLongLiteral(1))
  }

  def numLiteral(negate: Boolean, token: Token, radix: Int = 10): ROp = {
    val text = if (radix == 10) token.getText else token.getText.substring(2)
    try {
      val value = java.lang.Long.parseLong(text, radix)
      opLiteral(token, new RLongLiteral(if (negate) -value else value))
    } catch {
      case _: NumberFormatException =>
    }

    opLiteral(token, new RBigIntLiteral(BigInt(text, radix)))
  }

  def floatLiteral(negate: Boolean, token: Token): ROp = {
    val value = java.lang.Double.parseDouble(token.getText.replaceAll("_", ""))
    opLiteral(token, new RDoubleLiteral(if (negate) -value else value))
  }

  def regExpLiteral(token: Token): ROp = opLiteral(token, new RRegExpLiteral(token.getText))

  def stringLiteral(token: Token): ROp = opLiteral(token, new RStringLiteral(token.getText))

  def regexpLiteral(token: Token): ROp = opLiteral(token, new RRegExpLiteral(token.getText))

  def _templateLiteral(token: Token, _yield: Boolean, await: Boolean, tagged: Boolean): ROp = {
    val op = opLiteral(token, new RTemplateStringLiteral(token.getText))
    // parse substitution templates in separate parser
    if (op.literal.asString.contains("${")) {
      addTemplate(op)
      op.add(
        RPropsOp(
          op.statement,
          Seq(
            ("yield", String.valueOf(_yield)),
            ("await", String.valueOf(await)),
            ("tagged", String.valueOf(tagged))
          )
        )
      )
    }
    op
  }
}
