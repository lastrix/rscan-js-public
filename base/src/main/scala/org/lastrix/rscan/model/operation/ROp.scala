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

import org.lastrix.rscan.model.Statement

abstract class ROp
(
  val key: ROpKey,
  childrenSeq: Seq[ROp] = null
) extends Comparable[ROp] {

  import ROp._

  private var _children: Array[ROp] = _
  children(childrenSeq)

  private var _parent: ROp = _

  final def parent: ROp = _parent

  final def parent(newParent: ROp): Unit = _parent = newParent

  final def `type`: ROpType = key.`type`

  final def statement: Statement = key.statement

  final def children: Array[ROp] = if (_children == null) Array.empty else _children

  final def children(value: Seq[ROp]): Unit = {
    _children =
      if (value == null || value.isEmpty) null
      else value.toArray.clone().sorted

    if (_children != null)
      for (child <- _children)
        if (child == null)
          throw new IllegalStateException()
        else
          child._parent = this
  }


  final def add(op: ROp): Unit =
    if (_children == null) children(Array(op))
    else children(_children ++ Array(op))

  final def hasChildren: Boolean = _children != null && _children.nonEmpty

  final def findChild(p: ROp => Boolean): Option[ROp] =
    if (hasChildren) _children.find(p)
    else None

  final def filterChildren(p: ROp => Boolean): Seq[ROp] =
    if (hasChildren) _children.filter(p)
    else Seq.empty

  final def indexOf(op: ROp): Int =
    if (_children == null) -1
    else _children.indexOf(op)

  def replaceChild(original: ROp, replacement: ROp): Unit = {
    if (replacement == null) throw new IllegalArgumentException("Replacement is empty")
    val idx = _children.indexOf(original)
    if (idx == -1)
      throw new IllegalArgumentException("No element found: " + original)
    _children.update(idx, replacement)
    replacement._parent = this
  }

  final def mapEachChild(mapper: ROp => ROp): Unit =
    if (_children != null)
      for (child <- _children)
        replaceChild(child, mapper.apply(child))

  def prettyPrint(sb: StringBuilder, prefix: String = ""): Unit = {
    sb.append(prefix).append(key).append(LINE_SEPARATOR)
    appendChildren(sb, prefix)
  }

  protected final def appendChildren(sb: StringBuilder, prefix: String): Unit = {
    val cPrefix = prefix + "\t\t"
    if (_children != null && _children.nonEmpty) {
      sb.append(prefix).append('\t').append("Children: ").append(LINE_SEPARATOR)
      for (child <- _children)
        child.prettyPrint(sb, cPrefix)
    }
  }

  final override def compareTo(t: ROp): Int = key.compareTo(t.key)
}

object ROp {
  val LINE_SEPARATOR: String = System.lineSeparator()
}
