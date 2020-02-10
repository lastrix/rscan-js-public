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

import org.jetbrains.annotations.{NotNull, Nullable}
import org.lastrix.rscan.model.Statement
import org.lastrix.rscan.model.operation.{ROp, ROpKey}

sealed class RConditionItemOp private
(
  key: ROpKey,
  private var _condition: ROp,
  private var _body: ROp,
  children: Seq[ROp]
)
  extends ROp(key, children) {

  override def replaceChild(original: ROp, replacement: ROp): Unit = {
    super.replaceChild(original, replacement)
    if (_condition == original) _condition = replacement
    else if (_body == original) _body = replacement
  }

  def condition: ROp = _condition

  def body: ROp = _body

  def isDefault: Boolean = condition == null

  def hasBody: Boolean = body != null
}

object RConditionItemOp {
  @NotNull
  def apply(@NotNull statement: Statement,
            @NotNull condition: ROp,
            @Nullable body: ROp): RConditionItemOp =
    new RConditionItemOp(
      ROpKey(StdOpType.CASE_ITEM, statement),
      condition,
      body,
      if (body == null) Seq(condition) else Seq(condition, body))

  @NotNull
  def apply(@NotNull statement: Statement,
            @Nullable body: ROp): RConditionItemOp =
    new RConditionItemOp(
      ROpKey(StdOpType.CASE_ITEM, statement),
      null,
      body,
      if (body == null) Seq.empty else Seq(body))

}
