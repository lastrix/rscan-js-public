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

import org.lastrix.rscan.model.{RModifier, RTextModifier}

object JSModifiers {
  val Async: RModifier = new RTextModifier("async")
  val Generator: RModifier = new RTextModifier("generator")
  val VarArgs: RModifier = new RTextModifier("varargs")
  val Const: RModifier = new RTextModifier("const")
  val Let: RModifier = new RTextModifier("let")
  val Var: RModifier = new RTextModifier("var")
  val Lambda: RModifier = new RTextModifier("lambda")

  val Readonly: RModifier = new RTextModifier("readonly")
  val NonReadonly: RModifier = new RTextModifier("nonreadonly")

  val Optional: RModifier = new RTextModifier("optional")
  val NonOptional: RModifier = new RTextModifier("nonoptional")

  val Getter: RModifier = new RTextModifier("getter")
  val Setter: RModifier = new RTextModifier("setter")

  val Public: RModifier = new RTextModifier("public")
  val Private: RModifier = new RTextModifier("private")
  val Protected: RModifier = new RTextModifier("protected")
  val Abstract: RModifier = new RTextModifier("abstract")
  val Static: RModifier = new RTextModifier("static")
}
