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

import java.io.{Closeable, File}

import org.jetbrains.annotations.{NotNull, Nullable}
import org.lastrix.rscan.vfs.analyzers.{AnyFileAnalyzer, DirectoryFileAnalyzer}

class VirtualFileSystem(val fileFilter: FileFilter = EmptyFileFilter) extends Closeable {
  val root = new VirtualFolder("")

  def register(@NotNull file: File, @Nullable actualName: String = null): Unit = {
    val tmp = if (actualName == null) file.getName else actualName
    val analyzer = VirtualFileSystem.analyzers.find(x => x.isApplicable(file, tmp))
    analyzer match {
      case Some(a) => a.analyze(this, file, tmp)
      case None => throw new IllegalArgumentException("Unable to handle file: " + tmp)
    }
  }

  override def close(): Unit = root.dispose()
}

object VirtualFileSystem {
  private val analyzers = List(
    AnyFileAnalyzer,
    DirectoryFileAnalyzer
  ).sorted

}
