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

import scala.collection.JavaConverters

trait Language extends Comparable[Language] {
  def name: String

  def ref: LanguageRef

  override def compareTo(t: Language): Int = name.compareTo(t.name)

  override def hashCode(): Int = name.hashCode()

  override def equals(obj: Any): Boolean =
    obj match {
      case x: Language => name.equals(x.name)
      case _ => false
    }

  override def toString: String = name
}

object Language {
  private[this] val _map: Map[String, Language] =
    JavaConverters.iterableAsScalaIterable(
      ServiceLoader.load[LanguageFactory](classOf[LanguageFactory], getClass.getClassLoader)
    )
      .map(x => x.create)
      .map(x => (x.name, x))
      .toMap

  def resolve(languageRef: LanguageRef): Language =
    resolve(languageRef.name)

  def resolve(name: String): Language =
    _map.get(name) match {
      case Some(lang) => lang
      case None => throw new IllegalArgumentException("No language for name: " + name)
    }
}
