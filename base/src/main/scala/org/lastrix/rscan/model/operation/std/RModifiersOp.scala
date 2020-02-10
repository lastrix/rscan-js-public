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

package org.lastrix.rscan.model.operation.std

import org.jetbrains.annotations.NotNull
import org.lastrix.rscan.model.operation.ROp.LINE_SEPARATOR
import org.lastrix.rscan.model.operation.{ROp, ROpKey}
import org.lastrix.rscan.model.{RModifier, Statement}

sealed class RModifiersOp private
(
  key: ROpKey,
  val modifiers: Array[RModifier]
)
  extends ROp(key) {

  override def prettyPrint(sb: StringBuilder, prefix: String): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
    sb.append(prefix).append('\t').append("Modifiers: ")
    for (m <- modifiers) sb.append(m.text).append(" ")
    sb.append(LINE_SEPARATOR)
    appendChildren(sb, prefix)
  }

}

object RModifiersOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull modifiers: Seq[RModifier]): RModifiersOp =
    new RModifiersOp(ROpKey(StdOpType.MODIFIERS, statement), modifiers.toArray)

  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull modifier: RModifier): RModifiersOp =
    new RModifiersOp(ROpKey(StdOpType.MODIFIERS, statement), Array(modifier))

}



