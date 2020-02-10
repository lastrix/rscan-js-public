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

package org.lastrix.rscan.model.operation

import java.util.concurrent.ConcurrentHashMap

import org.lastrix.rscan.model.Statement

sealed class ROpKey private
(
  val `type`: ROpType,
  val statement: Statement
) extends Comparable[ROpKey] {
  override def toString: String =
    if (statement.undefined)
      s"${`type`.name} at <undefined>"
    else
      s"${`type`.name} at $statement"


  override def hashCode(): Int = `type`.hashCode() * 31 + statement.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case x: ROpKey => `type` == x.`type` && statement == x.statement
    case _ => false
  }

  override def compareTo(t: ROpKey): Int = {
    val res = statement.compareTo(t.statement)
    if (res != 0)
      return res

    `type`.name.compareTo(t.`type`.name)
  }
}

object ROpKey {
  private val cache = new ConcurrentHashMap[ROpKey, ROpKey]()

  def apply(`type`: ROpType, statement: Statement): ROpKey =
    cache.computeIfAbsent(new ROpKey(`type`, statement), _key => _key)
}
