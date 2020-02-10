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

package org.lastrix.rscan.api.parser.antlr4

/**
 * Allows to configure and tune parser process for specific needs
 *
 * @param offsetLine         the initial offset line for source
 * @param offsetLinePosition the initial offset line position for source
 * @param onLexerComplete    this method will be called once tokenizing complete
 */
sealed case class ParserConfig
(
  offsetLine: Int = 0,
  offsetLinePosition: Int = 0,
  onLexerComplete: AbstractRScanLexer => Unit = { _ => },
  initialRule: Option[String] = None,
  additionalSuccessMsg: Option[String] = None,
)
