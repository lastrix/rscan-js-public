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

import java.util

import org.antlr.v4.runtime.{Parser, Token, TokenStream}
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.Nullable
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.model.literal.{RBooleanLiteral, RLiteral, RNullLiteral, RUndefinedLiteral}
import org.lastrix.rscan.model.operation._
import org.lastrix.rscan.model.operation.raw.RawOpType
import org.lastrix.rscan.model.operation.std._
import org.lastrix.rscan.model.tokens.RScanToken
import org.lastrix.rscan.model.{RModifier, Statement}
import org.lastrix.rscan.vfs.VirtualFile

import scala.collection.{JavaConverters, mutable}

abstract class AbstractRScanParser
(stream: TokenStream, val file: VirtualFile)
  extends Parser(stream) {

  import AbstractRScanParser._

  /**
   * Returns language used by this parser,
   * Each parser expected to serve single language.
   *
   * @return LanguageRef
   */
  def language: LanguageRef

  /**
   * Returns default rule executed if no value provided by caller
   *
   * @return String
   * @see #invokeDefaultRule
   */
  def defaultRule: String

  /**
   * Returns template rule for processing TemplateLiterals.
   *
   * @return String
   * @see #invokeTemplateRule
   * @throws UnsupportedOperationException if template strings not supported for this language
   */
  def templateRule: String

  /**
   * Returns rule name that must be used for specified operation type
   *
   * @param foldBlock the folded block operation
   * @return String
   * @see #invokeRuleFor
   */
  def ruleNameForFoldedBlock(foldBlock: RFoldOp): String =
    option(FoldRuleOption) match {
      case Some(rule) => rule
      case None => DefaultFoldRuleName
    }


  /**
   * Returns rule name by its index
   *
   * @param ruleIndex the rule index
   * @return String
   */
  def ruleName(ruleIndex: Int): String = getRuleNames()(ruleIndex)

  override final def getTokenNames: Array[String] =
    throw new UnsupportedOperationException

  //////////////////////////////////// Execution ///////////////////////////////////////////////////////////////////////
  /**
   * Default entry point for all parsers, this method should be called
   * when file parsing started.
   *
   * @tparam T the result type, should be RLangOperation in most cases
   * @return T
   * @see #defaultRule
   */
  final def invokeDefaultRule[T]: T = invokeRule[T](defaultRule)

  /**
   * Parses templates (if supported by language)
   *
   * @tparam T the result type
   * @return operation in most cases
   */
  final def invokeTemplateRule[T]: T = invokeRule[T](templateRule)

  /**
   * Invoke rule by name, will throw exception if there is no rule (MethodNotFoundException)
   *
   * @param ruleName the called rule name
   * @tparam T the result type
   * @return T
   */
  final def invokeRule[T](ruleName: String): T = resultOfRule[T](ruleName)

  /**
   * Special method for folding support.
   * For example, when operation type is FOLD parser will use rule
   * for blocks in language (depends on actual implementation).
   * The rule name for operation type is resolved by #ruleNameForFoldedBlock .
   *
   * @param foldBlock the folded block operation with token containing list of folded tokens
   * @return the operation
   * @see #ruleNameForFoldedBlock
   */
  final def invokeRuleFor(foldBlock: RFoldOp): ROp =
    resultOfRule[ROp](ruleNameForFoldedBlock(foldBlock))

  private def resultOfRule[T](ruleName: String): T =
    resultField[T](getClass.getDeclaredMethod(ruleName).invoke(this))

  private def resultField[T](value: AnyRef): T =
    value.getClass.getDeclaredField("result").get(value).asInstanceOf[T]

  ////////////////////////////////////// Attributes ////////////////////////////////////////////////////////////////////

  private lazy val tokenTypeMap = AbstractRScanLexer.tokenTypeMapForVocabulary(getVocabulary)
  private val _folds = mutable.MutableList[RFoldOp]()
  private val _templates = mutable.MutableList[RLiteralOp]()
  protected var _options = new mutable.HashMap[String, String]()

  def tokenType(name: String): Int =
    tokenTypeMap.getOrElse(name, Token.INVALID_TYPE)

  protected def addFold(op: RFoldOp): Unit = _folds += op

  def folds: List[RFoldOp] = _folds.toList

  protected def addTemplate(op: RLiteralOp): Unit = _templates += op

  def templates: List[RLiteralOp] = _templates.toList

  def options(vals: Seq[(String, String)]): Unit =
    for (item <- vals) _options.put(item._1, item._2)

  def option(key: String): Option[String] = _options.get(key)

  def boolOption(key: String): Boolean =
    option(key) match {
      case Some(value) => java.lang.Boolean.parseBoolean(value)
      case None => false
    }

  ////////////////////////////////////// Statement operations //////////////////////////////////////////////////////////
  def evalStatement(list: java.util.List[ROp]): Statement =
    if (list.isEmpty) throw new IllegalArgumentException("At least single element expected")
    else evalStatement(list.get(0), list.get(list.size() - 1))

  def evalStatement(start: ROp, stop: ROp): Statement =
    Statement(
      file.virtualPath,
      start.statement.startLine,
      start.statement.startLinePosition,
      stop.statement.endLine,
      stop.statement.endLinePosition
    )

  def evalStatement(token: Token): Statement = evalStatement(token, token)

  def evalStatement(startToken: Token, endToken: Token): Statement =
    Statement(
      file.virtualPath,
      startToken.getLine,
      startToken.getCharPositionInLine,
      endLine(endToken),
      endLinePosition(endToken)
    )

  def endLine(t: Token): Int = t.getLine + StringUtils.countMatches(t.getText, "\n")

  def endLinePosition(t: Token): Int = t.getText.lastIndexOf('\n') match {
    case -1 => t.getCharPositionInLine + t.getText.length
    case idx: Int => t.getText.length - idx - 1
  }

  ////////////////////////////////////// Operation creation ////////////////////////////////////////////////////////////
  /**
   * Create folded block operation from token, the FOLD type is used by default
   *
   * @param token        the folded tokens token
   * @param foldRuleName the rule name to execute for parser
   * @return
   */
  def opFoldBlock(token: RScanToken, foldRuleName: String = DefaultFoldRuleName): ROp = {
    val head: RScanToken = token.foldedTokens().head
    val last = token.foldedTokens().last
    token.option(FoldRuleOption, foldRuleName)
    val op = RFoldOp(evalStatement(head, last), token)
    _folds += op
    op
  }

  /**
   * Create language root operation, this one is used to separate
   * languages inside multi-lang documents
   *
   * @param start the start token  for statement evaluation
   * @param stop  the stop token for statement evaluation
   * @param list  children list
   * @return operation
   */
  def opLang(start: Token, stop: Token, list: java.util.List[ROp]): RLangOp = RLangOp(
    Statement(
      file.virtualPath,
      1,
      0,
      endLine(stop),
      endLinePosition(stop)
    ),
    language,
    asArray(list))

  def opEmptyLang(): RLangOp = RLangOp(Statement(file.virtualPath), language, Seq.empty)

  /**
   * Creates BLOCK operation
   *
   * @param start start token
   * @param stop  stop token
   * @param list  children operations
   * @return the block operation
   */
  def opBlock(start: Token, stop: Token, list: java.util.List[ROp]): ROp =
    opNode(evalStatement(start, stop), StdOpType.BLOCK, asArray(list))

  def opName(child: ROp): ROp = opNode(StdOpType.NAME, child)

  def opNone(token: Token): ROp = opNode(StdOpType.NONE, token)

  def opProps(statement: Statement, props: java.util.Map[String, String]): ROp =
    RPropsOp(statement, JavaConverters.mapAsScalaMap(props).toSeq)

  def opModifiers(statement: Statement, modifiers: java.util.List[RModifier]): ROp =
    RModifiersOp(statement, JavaConverters.asScalaBuffer(modifiers))

  def opCondition(op: ROp): ROp = opNode(StdOpType.CONDITION, op)

  def opConditionalBlock(start: Token,
                         stop: Token,
                         conditionalType: ConditionalType,
                         condition: ROp,
                         body: ROp): ROp =
    RConditionBlockOp(evalStatement(start, stop), conditionalType, condition, body)

  def opSwitch(start: Token, stop: Token, condition: ROp, body: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.CASE, Array(condition, body))

  def opCaseItem(start: Token, stop: Token, condition: ROp, body: java.util.List[ROp]): ROp =
    RConditionItemOp(evalStatement(start, stop), condition, blockWrapOrNull(body))

  def opDefaultCaseItem(start: Token, stop: Token, body: java.util.List[ROp]): ROp =
    RConditionItemOp(evalStatement(start, stop), blockWrapOrNull(body))

  def opTry(start: Token, stop: Token, body: ROp, catchBlocks: java.util.List[ROp], finallyBlock: ROp): ROp =
    opNode(
      evalStatement(start, stop),
      StdOpType.TRY,
      Array(body) ++ asArray(catchBlocks) ++ (if (finallyBlock == null) Array.empty[ROp] else Array(finallyBlock))
    )

  def opIfStmt(start: Token, stop: Token, conditional: java.util.List[ROp], @Nullable unconditional: ROp): ROp =
    opNode(
      evalStatement(start, stop),
      StdOpType.IF,
      asArray(conditional) ++ (if (unconditional == null) Array.empty[ROp] else Array(unconditional))
    )

  def opStmtList(list: java.util.List[ROp]): ROp = opNode(StdOpType.BLOCK, list)

  def opExprList(list: java.util.List[ROp]): ROp =
    if (list.size() == 1) list.get(0)
    else opNode(StdOpType.EXPR_LIST, list)

  def opTernary(condition: ROp, opTrue: ROp, opFalse: ROp): ROp =
    RTernaryOp(evalStatement(condition, opFalse), opCondition(condition), opExpr(opTrue), opExpr(opFalse))

  def opBinary(`type`: BinaryType, list: java.util.List[ROp]): ROp =
    RBinaryOp(evalStatement(list), `type`, asArray(list))

  def opBinary(`type`: BinaryType, left: ROp, right: ROp): ROp =
    RBinaryOp(evalStatement(left, right), `type`, Array(left, right))

  def opUnary(start: Token, stop: Token, `type`: UnaryType, operation: ROp): ROp =
    RUnaryOp(evalStatement(start, stop), `type`, operation)

  def opAssign(left: ROp, assignType: AssignType, right: ROp): ROp =
    RAssignOp(evalStatement(left, right), assignType, left, right)

  def opSuper(token: Token): ROp = opNode(StdOpType.SUPER, token)

  def opReturn(token: Token): ROp = opNode(StdOpType.RETURN, token)

  def opReturn(start: Token, stop: Token, child: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.RETURN, Array(child))

  def opContinue(token: Token): ROp = opNode(StdOpType.CONTINUE, token)

  def opBreak(token: Token): ROp = opNode(StdOpType.BREAK, token)

  def opThrow(start: Token, stop: Token, child: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.THROW, Array(child))

  def opNode(statement: Statement, `type`: ROpType, children: Array[ROp] = Array.empty): ROp =
    RNodeOp(`type`, statement, children)

  def opNode(statement: Statement, `type`: ROpType, list: java.util.List[ROp]): ROp =
    opNode(statement, `type`, asArray(list))

  def opNode(statement: Statement, `type`: ROpType, op: ROp): ROp =
    opNode(statement, `type`, Array(op))

  def opNode(`type`: ROpType, list: java.util.List[ROp]): ROp =
    opNode(evalStatement(list), `type`, asArray(list))

  def opNode(`type`: ROpType, child: ROp): ROp = opNode(child.statement, `type`, Array(child))

  def opNode(`type`: ROpType, token: Token): ROp = opNode(evalStatement(token), `type`)

  def noOp(token: Token): ROp = opNode(evalStatement(token), StdOpType.NONE)

  def opParen(start: Token, stop: Token, child: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.PARENTHESIZED, Array(child))

  def opArrayAccessor(start: Token, stop: Token, expr: ROp): ROp =
    opNode(evalStatement(start, stop), StdOpType.ARRAY_ACCESSOR, Array(expr))

  def opChain(list: java.util.List[ROp]): ROp = opNode(StdOpType.CHAIN, list)

  def opBuiltin(start: Token, stop: Token, function: String, argument: ROp): ROp =
    RBuiltinCallOp(evalStatement(start, stop), function, if (argument == null) Seq.empty else Seq(argument))

  def opCall(start: Token, stop: Token, list: java.util.List[ROp]): ROp =
    opNode(evalStatement(start, stop), RawOpType.RAW_CALL, asArray(list))

  def opEmptyCall(start: Token, stop: Token): ROp = opNode(evalStatement(start, stop), RawOpType.RAW_CALL)

  def trueLiteral(token: Token): ROp = opLiteral(token, RBooleanLiteral.TRUE)

  def falseLiteral(token: Token): ROp = opLiteral(token, RBooleanLiteral.FALSE)

  def nullLiteral(token: Token): ROp = opLiteral(token, RNullLiteral.NULL)

  def undefinedLiteral(token: Token): ROp = opLiteral(token, RUndefinedLiteral.UNDEFINED)

  def opLiteral(token: Token, literal: RLiteral): RLiteralOp = RLiteralOp(evalStatement(token), literal)

  def opUnresolvedId(token: Token): ROp = opUnresolvedId(token, token.getText)

  def opUnresolvedId(token: Token, text: String): ROp = RUnresolvedIdOp(evalStatement(token), text)

  def opLabel(start: Token, stop: Token, label: String, op: ROp): ROp =
    RLabelOp(evalStatement(start, stop), label, Array(op))

  def opExpr(child: ROp): ROp = opNode(child.statement, StdOpType.EXPR, child)

  ////////////////////////////////////// Helpers ///////////////////////////////////////////////////////////////////////

  def asArray(list: util.List[ROp]): Array[ROp] = list.toArray(Array.ofDim[ROp](list.size()))

  def unimplemented(): Unit = throw new UnsupportedOperationException("Not implemented")

  def lastFoldToken(token: RScanToken): Token = token.foldedTokens().last

  def blockWrapOrNull(list: util.List[ROp]): ROp =
    if (list.isEmpty) null
    else if (list.size() == 1) {
      val first = list.get(0)
      first.`type` match {
        case StdOpType.BLOCK | StdOpType.BLOCK_WRAP => first
        case _ =>
          if (first.isInstanceOf[RFoldOp]) first
          else opNode(StdOpType.BLOCK_WRAP, first)
      }
    } else opNode(StdOpType.BLOCK_WRAP, list)
}

object AbstractRScanParser {
  val FoldRuleOption = "foldRule"
  val DefaultFoldRuleName = "startFoldBlock"
}
