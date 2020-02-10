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
import java.nio.file.Files

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.Nullable
import org.lastrix.rscan.vfs.source.FileSource
import org.lastrix.rscan.vfs.{VirtualFile, VirtualFileSystem}
import org.slf4j.LoggerFactory

object AnyFileAnalyzer extends FileAnalyzer {
  private val log = LoggerFactory.getLogger(this.getClass)
  override val priority: Int = Int.MaxValue

  override def isApplicable(file: File, @Nullable actualName: String): Boolean = true

  override def analyze(vfs: VirtualFileSystem, file: File, actualName: String): Unit = {
    log.trace("Registering file: {}", file)

    val actualFile = getActualFile(file)
    if (actualFile == null) return

    var name = if (StringUtils.isBlank(actualName)) actualFile.getName
    else actualName
    if (name.indexOf('/') != -1) name = name.substring(name.lastIndexOf('/'))
    if (name.indexOf('\\') != -1) name = name.substring(name.lastIndexOf('\\'))

    val root = vfs.root
    if (vfs.fileFilter.isAllowed(root.absoluteName, name))
      root :+ new VirtualFile(name, new FileSource(actualFile, name), root)
  }

  private def getActualFile(file: File): File = {
    if (!Files.isSymbolicLink(file.toPath)) return file
    val actual = Files.readSymbolicLink(file.toPath).toFile
    if (!actual.exists) {
      log.warn("Symbolic link {} points to non-existing file", file)
      return null
    }
    actual
  }
}
