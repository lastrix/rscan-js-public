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

package org.lastrix.rscan.model.tokens

import org.antlr.v4.runtime.{CharStream, CommonToken, Lexer, TokenSource}
import org.lastrix.rscan.vfs.VirtualPath

import scala.collection.mutable

/**
 * Default RScan token with folding support
 *
 * @param path    the virtual path of target file
 * @param source  the source for this token
 * @param `type`  token type, see corresponding Lexer class
 * @param channel the token channel, DEFAULT is 0, COMMENTS should be 1
 * @param start   the start position of token in stream
 * @param stop    the stop position of token in stream
 */
class RScanToken(val path: VirtualPath, source: org.antlr.v4.runtime.misc.Pair[TokenSource, CharStream], `type`: Int, channel: Int = Lexer.DEFAULT_TOKEN_CHANNEL, start: Int = 0, stop: Int = 0)
  extends CommonToken(source, `type`, channel, start, stop) {

  private[this] val foldedTokens = mutable.MutableList[RScanToken]()
  private[this] val _options = mutable.MutableList[(String, String)]()

  def this(token: RScanToken) {
    this(token.path, new org.antlr.v4.runtime.misc.Pair(token.getTokenSource, token.getInputStream), token.getType, token.getChannel, token.getStartIndex, token.getStopIndex)
    setText(token.text)
    setCharPositionInLine(token.charPositionInLine)
    setTokenIndex(token.getTokenIndex)
    setLine(token.line)
  }

  def setFoldedTokens(list: Traversable[RScanToken]): Unit = {
    foldedTokens.clear()
    foldedTokens ++= list
  }

  def foldedTokens(): List[RScanToken] = foldedTokens.toList

  def options: Seq[(String, String)] = _options.toList

  def option(key: String): Option[String] = _options.find(_._1 == key).map(_._2)

  def option(key: String, value: String): Unit =
    indexOf(key) match {
      case Some(idx) => _options.update(idx, (key, value))
      case None => _options += ((key, value))
    }

  private def indexOf(key: String): Option[Int] =
    _options.find(_._1 == key).map(p => _options.indexOf(p))
}
