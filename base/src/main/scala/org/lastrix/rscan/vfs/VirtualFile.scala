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

import org.jetbrains.annotations.NotNull
import org.lastrix.rscan.api.Disposable
import org.lastrix.rscan.vfs.source.Source

import scala.collection.mutable


sealed class VirtualFile(
                          val name: String,
                          val source: Source,
                          var parent: VirtualFolder = null,
                          var flags: Int = 0,
                          private var custom: mutable.Map[String, Object] = null
                        ) extends Disposable with Comparable[VirtualFile] {


  /**
   * File absolute name, returns name if parent is null
   *
   * @return String
   */
  @NotNull
  def absoluteName: String =
    if (parent == null) name
    else parent.absoluteName + name

  @NotNull
  def virtualPath: VirtualPath =
    VirtualPath.forFolderAndName(parent, name)

  def extension: String = {
    val idx = name.lastIndexOf('.')
    if (idx == -1) "" else name.substring(idx + 1)
  }

  def hasExtension(@NotNull ext: String): Boolean =
    extension == ext

  /**
   * Dispose all data assigned for this object
   */
  override def dispose(): Unit = {
    source match {
      case disposable: Disposable => disposable.dispose()
      case _ =>
    }
    this.synchronized {
      if (custom != null)
        custom.clear()
    }
  }

  override def compareTo(t: VirtualFile): Int =
    absoluteName.compareTo(t.absoluteName)


  override def toString: String = absoluteName

  ///////////////////////////////// Custom Data operations ///////////////////////////////////////////////////
  def customData(): Map[String, Object] = this.synchronized {
    if (custom == null) Map.empty else custom.toMap
  }

  /**
   * Set custom data value
   *
   * @param key   the key, system properties starts with $, you should not use this symbol as first one
   * @param value the kryo-serializable value
   * @param force value overwrite is not permitted by default, if you set force to true, than
   *              all invocations of this method will overwrite stored value
   */
  def customData(key: String, value: Object, force: Boolean = false): Unit = this.synchronized {
    if (custom == null)
      custom = new mutable.HashMap[String, Object]()

    if (!force && custom.contains(key)) {
      if (custom.get(key) == value)
        return

      throw new IllegalArgumentException("Unable to overwrite value")
    }
    custom.put(key, value)
  }

  /**
   * Get custom data
   *
   * @param key the key
   * @return
   */
  def getCustomData(key: String): Option[Object] = this.synchronized {
    if (custom == null) Option.empty else custom.get(key)
  }

  def consume(): Unit =
    flags = flags | 1

  def isConsumed: Boolean =
    (flags & 1) != 0
}
