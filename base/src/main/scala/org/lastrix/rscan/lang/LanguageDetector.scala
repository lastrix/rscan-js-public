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

package org.lastrix.rscan.lang

import java.util.ServiceLoader

import org.lastrix.rscan.vfs.VirtualFile

import scala.collection.JavaConverters

trait LanguageDetector extends Comparable[LanguageDetector] {
  def priority: Int

  def detect(file: VirtualFile): Option[LanguageRef]

  override def compareTo(t: LanguageDetector): Int =
  // false positive
  //noinspection DuplicatedCode
    Integer.compare(priority, t.priority) match {
      case x: Int if x != 0 => x
      case _ => getClass.getTypeName.compareTo(t.getClass.getTypeName)
    }
}

object LanguageDetector {
  private[this] val CS_LANG = "$languages"

  private val _list =
    JavaConverters.iterableAsScalaIterable(ServiceLoader.load[LanguageDetectorFactory](classOf[LanguageDetectorFactory]))
      .map(p => p.create)
      .toList
      .sorted

  def apply(file: VirtualFile): Set[LanguageRef] =
    file.getCustomData(CS_LANG) match {
      case Some(data) => LanguageRef.fromString(data.toString)
      case None =>
        val set = detectLanguageSet(file)
        file.customData(CS_LANG, LanguageRef.toString(set))
        set
    }

  private def detectLanguageSet(file: VirtualFile): Set[LanguageRef] =
    _list.map(_.detect(file))
      .flatMap {
        case Some(lang) => Seq(lang)
        case None => Seq.empty
      }
      .sorted
      .toSet
}

trait LanguageDetectorFactory {
  def create: LanguageDetector
}
