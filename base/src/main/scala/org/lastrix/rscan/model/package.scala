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

package org.lastrix.rscan

import java.io._
import java.nio.charset.StandardCharsets

import org.apache.commons.io.FileUtils
import org.lastrix.rscan.model.operation._

package object model {

  def prettyPrint(dir: File, op: ROp, kind: String = "base"): Unit = {
    val sb = new StringBuilder
    op.prettyPrint(sb)
    FileUtils.writeStringToFile(new File(s"${dir.getAbsolutePath}/${op.statement.path.toString.replace(vfs.SEPARATOR_CHAR, '_')}.$kind.txt"), sb.toString(), StandardCharsets.UTF_8)
  }
}
