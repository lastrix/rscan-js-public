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

package org.lastrix.rscan.lang.javascript.parser.model.operation

import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.ROp.LINE_SEPARATOR
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RJsxOp private
(
  key: ROpKey,
  val tagName: String,
  children: Seq[ROp]
)
  extends ROp(key, children) {

  override def prettyPrint(sb: StringBuilder, prefix: String): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
      .append(prefix).append("\t").append("Tag: ").append(tagName).append(LINE_SEPARATOR)
    appendChildren(sb, prefix)
  }
}

object RJsxOp {
  def apply(statement: Statement, tagName: String, children: Seq[ROp]): RJsxOp =
    new RJsxOp(ROpKey(JsOpType.RAW_JSX, statement), tagName, children)
}
