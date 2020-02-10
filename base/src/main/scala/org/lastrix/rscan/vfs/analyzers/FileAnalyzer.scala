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

package org.lastrix.rscan.vfs.analyzers

import java.io.File

import org.jetbrains.annotations.Nullable
import org.lastrix.rscan.vfs.VirtualFileSystem

trait FileAnalyzer extends Comparable[FileAnalyzer] {
  val priority: Int

  def isApplicable(file: File): Boolean = isApplicable(file, file.getName)

  def isApplicable(file: File, @Nullable actualName: String = null): Boolean

  def analyze(vfs: VirtualFileSystem, file: File): Unit = analyze(vfs, file, file.getName)

  def analyze(vfs: VirtualFileSystem, file: File, @Nullable actualName: String = null): Unit

  override def compareTo(t: FileAnalyzer): Int = Integer.compare(priority, t.priority)
}
