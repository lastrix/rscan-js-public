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

import org.antlr.v4.runtime.{CharStream, TokenFactory, TokenSource}
import org.lastrix.rscan.vfs.VirtualPath

class RScanTokenFactory
(
  val path: VirtualPath,
  val offsetLine: Int,
  val offsetLinePos: Int
)
  extends TokenFactory[RScanToken] {
  override def create(source: org.antlr.v4.runtime.misc.Pair[TokenSource, CharStream], `type`: Int, text: String, channel: Int, start: Int, stop: Int, line: Int, charPositionInLine: Int): RScanToken = {
    val token = new RScanToken(path, source, `type`, channel, start, stop)
    token.setLine(offsetLine + line)
    token.setCharPositionInLine(if (line == 1) offsetLinePos + charPositionInLine else charPositionInLine)
    token.setText(text)
    token
  }

  override def create(`type`: Int, text: String): RScanToken =
    new RScanToken(path, null, `type`)
}
