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

import java.io.InputStream
import java.nio.charset.Charset
import java.util.regex.Pattern

import org.antlr.v4.runtime.{CharStream, CharStreams}
import org.apache.commons.io.input.BOMInputStream
import org.apache.commons.io.{ByteOrderMark, FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.{NotNull, Nullable}
import org.mozilla.universalchardet.UniversalDetector

package object vfs {
  private val DEFAULT_BYTE_COUNT = 4096
  private val MAX_CHARSET_SIZE = 50 * FileUtils.ONE_KB.toInt
  val SEPARATOR_CHAR = '/'
  val SEPARATOR = "/"
  val MULPTIPLE_SLASH_REPLACE: Pattern = Pattern.compile("^\\\\+$");

  def asBOMlessStream(stream: InputStream) =
    new BOMInputStream(stream, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE)


  def toCanonicalVirtualFileName(name: String): String = {
    var tmp: String = toVirtualPath(name)
    if (StringUtils.isBlank(tmp) || SEPARATOR == tmp) return SEPARATOR
    while (!tmp.isEmpty && tmp.charAt(0) == SEPARATOR_CHAR)
      tmp = tmp.substring(1)
    while (!tmp.isEmpty && tmp.charAt(tmp.length - 1) == SEPARATOR_CHAR)
      tmp = tmp.substring(0, tmp.length - 1)
    tmp
  }

  @NotNull
  def toVirtualPath(@Nullable path: String): String = {
    if (StringUtils.isBlank(path))
      return SEPARATOR;

    var result = MULPTIPLE_SLASH_REPLACE.matcher(path.replace('\\', SEPARATOR_CHAR)).replaceAll("\\")
    if (SEPARATOR == result)
      return SEPARATOR

    while (!result.isEmpty && result.charAt(0) == SEPARATOR_CHAR)
      result = result.substring(1);

    while (!result.isEmpty && result.charAt(result.length() - 1) == SEPARATOR_CHAR)
      result = result.substring(0, result.length() - 1);

    result = result.replaceAll("/+", "/");

    if (StringUtils.isBlank(path)) SEPARATOR else result
  }

  def toCharStream(file: VirtualFile): CharStream = {
    val text = toPlainText(file)
    CharStreams.fromString(text, file.absoluteName)
  }

  @NotNull
  def toPlainText(file: VirtualFile): String = {
    val charsetName: String = detectCharset(file).displayName
    toPlainTextWithCharset(file, charsetName)
  }

  private def toPlainTextWithCharset(file: VirtualFile, charsetName: String) = {
    var is: InputStream = null
    try {
      is = asBOMlessStream(file.source.inputStream())
      IOUtils.toString(is, charsetName)
    }
    finally if (is != null) is.close()
  }


  def detectCharset(file: VirtualFile): Charset = {
    file.getCustomData(classOf[Charset].getSimpleName) match {
      case Some(charsetName) => return Charset.forName(charsetName.asInstanceOf[String])
      case None =>
    }
    var is: InputStream = null
    try {
      is = file.source.inputStream()
      val bytes = IOUtils.toByteArray(is, Math.min(file.source.size(), DEFAULT_BYTE_COUNT))
      val charset = detectCharset(bytes)
      file.customData(classOf[Charset].getSimpleName, charset.displayName)
      charset
    } finally if (is != null) is.close()
  }

  private def detectCharset(buffer: Array[Byte]) = {
    val detector = new UniversalDetector(null)
    var charsetDetectionLength = buffer.length
    if (charsetDetectionLength > MAX_CHARSET_SIZE) charsetDetectionLength = MAX_CHARSET_SIZE
    detector.handleData(buffer, 0, charsetDetectionLength)
    detector.dataEnd()
    var encodingName = detector.getDetectedCharset
    if (encodingName == null) encodingName = "UTF-8"
    // ensure exist
    Charset.forName(encodingName)
  }

}
