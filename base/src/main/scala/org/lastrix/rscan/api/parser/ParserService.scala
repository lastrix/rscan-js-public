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

import java.util.ServiceLoader

import org.antlr.v4.runtime.TokenStream
import org.lastrix.rscan.api.parser.antlr4.{AbstractRScanLexer, AbstractRScanParser}
import org.lastrix.rscan.lang.LanguageRef
import org.lastrix.rscan.vfs.VirtualFile

import scala.collection.JavaConverters

trait ParserService {
  def language: LanguageRef

  /**
   * Create new lexer for provided file
   *
   * @param file          the virtual file
   * @param text          the optional loaded text
   * @param offsetLine    the optional offset line number if partial parser
   * @param offsetLinePos the optional offset line position if partial parser
   * @return the new lexer for target file
   */
  def newLexer(file: VirtualFile, text: Option[String] = None, offsetLine: Int = 0, offsetLinePos: Int = 0): AbstractRScanLexer

  /**
   * Create new parser for provided file and Token stream
   *
   * @param file   the virtual file (source)
   * @param stream the token stream to analyze
   * @return the Parser
   */
  def newParser(file: VirtualFile, stream: TokenStream): AbstractRScanParser

  def lexerClass: Class[_ <: AbstractRScanLexer]

  def parserClass: Class[_ <: AbstractRScanParser]
}

object ParserService {
  private[this] val _map: Map[LanguageRef, ParserService] =
    JavaConverters.iterableAsScalaIterable(ServiceLoader.load[ParserService](classOf[ParserService], getClass.getClassLoader))
      .map(e => (e.language, e))
      .toMap

  def apply(language: LanguageRef): ParserService =
    _map.get(language) match {
      case Some(srv) => srv
      case None => throw new IllegalArgumentException("No Antlr4 service found for language: " + language.name)
    }
}
