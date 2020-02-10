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

package org.lastrix.rscan.api.parser

import org.antlr.v4.runtime.{CharStream, CharStreams}
import org.jetbrains.annotations.NotNull
import org.lastrix.rscan.api.parser.antlr4.AbstractRScanLexer
import org.lastrix.rscan.model.tokens.RScanTokenFactory
import org.lastrix.rscan.vfs
import org.lastrix.rscan.vfs.VirtualFile

abstract class AbstractParserService extends ParserService {

  /**
   * Create new lexer for provided file
   *
   * @param file          the virtual file
   * @param text          the optional loaded text
   * @param offsetLine    the optional offset line number if partial parser
   * @param offsetLinePos the optional offset line position if partial parser
   * @return the new lexer for target file
   */
  @NotNull
  override final def newLexer(@NotNull file: VirtualFile, text: Option[String], offsetLine: Int, offsetLinePos: Int): AbstractRScanLexer = {
    val charStream = text match {
      case Some(v) => CharStreams.fromString(v, file.absoluteName)
      case None => vfs.toCharStream(file)
    }
    val lexer = newLexer(charStream, file)
    lexer.setTokenFactory(new RScanTokenFactory(file.virtualPath, offsetLine, offsetLinePos))
    lexer
  }

  def newLexer(stream: CharStream, file: VirtualFile): AbstractRScanLexer
}
