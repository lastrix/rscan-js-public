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
import org.lastrix.rscan.model.operation.{ROp, ROpKey, ROpType}

sealed class RNodeOp private
(
  key: ROpKey,
  children: Seq[ROp]
) extends ROp(key, children) {
}

object RNodeOp {
  @NotNull
  def apply(@NotNull `type`: ROpType,
            @NotNull statement: Statement,
            @NotNull children: Seq[ROp]): RNodeOp =
    new RNodeOp(ROpKey(`type`, statement), children)

  @NotNull
  def apply(@NotNull `type`: ROpType,
            @NotNull statement: Statement,
            @NotNull child: ROp): RNodeOp =
    apply(`type`, statement, Seq(child))

  @NotNull
  def discarded(@NotNull statement: Statement,
                @Nullable child: ROp = null): RNodeOp =
    apply(StdOpType.DISCARDED, statement, if (child == null) Seq.empty else Seq(child))

}
