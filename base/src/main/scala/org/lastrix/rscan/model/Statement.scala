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

package org.lastrix.rscan.model

import org.lastrix.rscan.vfs.VirtualPath

sealed case class Statement
(
  path: VirtualPath,
  startLine: Int = 0,
  startLinePosition: Int = 0,
  endLine: Int = 0,
  endLinePosition: Int = 0,
  undefined: Boolean = false,
  virtual: Int = 0
) extends Comparable[Statement] {

  def nextVirtual: Statement =
    Statement(path, startLine, startLinePosition, endLine, endLinePosition, undefined, virtual + 1)

  def inside(statement: Statement): Boolean = path != statement.path ||
    startLine >= statement.startLine && (startLine != statement.startLine || startLinePosition >= statement.startLinePosition) &&
      endLine <= statement.endLine && (endLine != statement.endLine || endLinePosition <= statement.endLinePosition)

  override def toString: String = s"$path[$startLine:$startLinePosition-$endLine:$endLinePosition${if (virtual == 0) "" else "@" + virtual}]${if (undefined) "*" else ""}"

  override def compareTo(t: Statement): Int = {
    var res = path.compareTo(t.path)
    if (res != 0)
      return res

    Integer.compare(startLine, t.startLine) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(startLinePosition, t.startLinePosition) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(endLine, t.endLine) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(endLinePosition, t.endLinePosition) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    Integer.compare(virtual, t.virtual) match {
      case x: Int if x != 0 => return x
      case _ =>
    }

    java.lang.Boolean.compare(undefined, t.undefined)
  }
}

object Statement {
  val Undefined: Statement = Statement(VirtualPath.Empty, undefined = true)
}
