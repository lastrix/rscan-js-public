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

import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.ROp.LINE_SEPARATOR
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RBuiltinCallOp private
(
  key: ROpKey,
  val name: String,
  children: Seq[ROp]
) extends ROp(key, children) {

  override def prettyPrint(sb: StringBuilder, prefix: String): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
    sb.append(prefix).append('\t').append("Name: ").append(name).append(LINE_SEPARATOR)
    if (hasChildren)
      appendChildren(sb, prefix)
  }
}

object RBuiltinCallOp {
  def apply(statement: Statement, name: String, children: Seq[ROp]): RBuiltinCallOp =
    new RBuiltinCallOp(ROpKey(StdOpType.BUILTIN_CALL, statement), name, children)

}

