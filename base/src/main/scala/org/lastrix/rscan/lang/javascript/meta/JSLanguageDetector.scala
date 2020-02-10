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

package org.lastrix.rscan.lang.javascript.meta

import org.lastrix.rscan.lang.{LanguageDetector, LanguageDetectorFactory, LanguageRef}
import org.lastrix.rscan.vfs.VirtualFile

object JSLanguageDetector extends LanguageDetector {
  override def priority: Int = Int.MinValue

  override def detect(file: VirtualFile): Option[LanguageRef] =
    if (file.hasExtension("js")) Some(JSLanguage.ref)
    else None
}

sealed class JSLanguageDetectorFactory extends LanguageDetectorFactory {
  override def create: LanguageDetector = JSLanguageDetector
}
