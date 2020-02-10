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
import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.ROp.LINE_SEPARATOR
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RPropsOp private
(
  key: ROpKey,
  val props: Array[(String, String)]
)
  extends ROp(key) {

  def prop(key: String): Option[String] = props.find(_._1 == key).map(_._2)

  def boolProp(key: String, defaultValue: Boolean = false): Boolean = prop(key) match {
    case Some(x) => java.lang.Boolean.parseBoolean(x)
    case None => defaultValue
  }

  override def prettyPrint(sb: StringBuilder, prefix: String): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
    for (item <- props)
      sb.append(prefix).append('\t')
        .append(item._1).append(": ").append(item._2)
        .append(LINE_SEPARATOR)
  }

}

object RPropsOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull props: Seq[(String, String)]): RPropsOp =
    new RPropsOp(ROpKey(StdOpType.PROPS, statement), props.toArray)

  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull key: String,
            @NotNull value: String): RPropsOp =
    new RPropsOp(ROpKey(StdOpType.PROPS, statement), Array((key, value)))

}


