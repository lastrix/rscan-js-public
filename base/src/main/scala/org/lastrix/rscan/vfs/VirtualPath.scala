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

package org.lastrix.rscan.vfs

import org.lastrix.rscan.vfs

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

/**
 * Defines virtual path used to identify file inside virtual file system
 *
 * @param parts absolute file name parts, no path separators allowed
 */
sealed class VirtualPath(val parts: Array[String])
  extends Comparable[VirtualPath] {
  override def toString: String =
    if (parts == null || parts.isEmpty)
      "/"
    else
      parts.reduce(_ + vfs.SEPARATOR_CHAR + _)

  override def equals(obj: Any): Boolean =
    obj match {
      case vp: VirtualPath => parts.sameElements(vp.parts)
      case _ => false
    }

  override def hashCode(): Int = MurmurHash3.arrayHash(parts)

  override def compareTo(t: VirtualPath): Int = toString.compareTo(t.toString)
}

object VirtualPath {
  def apply(parts: Array[String]): VirtualPath = new VirtualPath(parts)

  def forFolderAndName(folder: VirtualFolder = null, name: String = null): VirtualPath = {
    if (folder == null) {
      if (name == null)
        throw new IllegalArgumentException("Name must be non null if folder is null")
      return VirtualPath(Array(name))
    }
    val list = mutable.MutableList[String]()
    if (name != null)
      list += name
    var tmp: VirtualFolder = folder
    while (tmp != null) {
      list += tmp.name
      tmp = tmp.parent
    }
    VirtualPath(list.reverse.toArray)
  }

  val Empty: VirtualPath = VirtualPath(Array(""))
}
