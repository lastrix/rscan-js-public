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

import org.lastrix.rscan.vfs.source.FileSource
import org.lastrix.rscan.vfs.{VirtualFile, VirtualFileSystem, VirtualFolder}
import org.slf4j.LoggerFactory

object DirectoryFileAnalyzer extends FileAnalyzer {
  private val log = LoggerFactory.getLogger(this.getClass)

  override val priority: Int = Int.MaxValue - 1

  override def isApplicable(file: File, actualName: String): Boolean = file.isDirectory

  override def analyze(vfs: VirtualFileSystem, file: File, actualName: String): Unit = {
    if (!file.isDirectory)
      throw new IllegalStateException("Unable to handle: " + file)
    log.debug("Registering directory: {}", file.getAbsolutePath)
    collectFilesRecursive(vfs, file, vfs.root)
  }

  private def collectFilesRecursive(vfs: VirtualFileSystem, directory: File, folder: VirtualFolder): Unit = {
    val files = directory.listFiles
    if (files == null) return
    val folderPath = folder.absoluteName
    for (file <- files) {
      if (file.isDirectory) {
        val virtualFolder = folder.mkdirs(file.getName, vfs.fileFilter)
        if (virtualFolder != null) collectFilesRecursive(vfs, file, virtualFolder)
      }
      else if (vfs.fileFilter.isAllowed(folderPath, file.getName)) {
        val source = new FileSource(file, folderPath + org.lastrix.rscan.vfs.SEPARATOR_CHAR + file.getName)
        folder :+ new VirtualFile(file.getName, source, folder)
      }
    }
  }
}
