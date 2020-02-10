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

package org.lastrix.rscan.lang.javascript.parser

import org.antlr.v4.runtime.{CharStream, TokenStream}
import org.lastrix.rscan.api.parser.AbstractParserService
import org.lastrix.rscan.api.parser.antlr4.{AbstractRScanLexer, AbstractRScanParser}
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.lang.javascript.meta.JSLanguage
import org.lastrix.rscan.vfs.VirtualFile

sealed class JSParserService extends AbstractParserService {
  override val language: LanguageRef = JSLanguage.ref

  override def newLexer(stream: CharStream, file: VirtualFile): AbstractRScanLexer = new JSLexer(stream, file)

  override def newParser(file: VirtualFile, stream: TokenStream): AbstractRScanParser = new JSParser(stream, file)

  override def lexerClass: Class[_ <: AbstractRScanLexer] = classOf[JSLexer]

  override def parserClass: Class[_ <: AbstractRScanParser] = classOf[JSParser]
}
