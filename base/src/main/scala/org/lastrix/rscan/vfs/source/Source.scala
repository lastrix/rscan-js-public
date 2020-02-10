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

package org.lastrix.rscan.vfs.source

import java.io.InputStream

/**
 * Interface for every source, provides raw data for anyone
 */
trait Source {
  /**
   * Return origin of this source,
   * The name must be unique for each source.
   *
   * @return String
   */
  def origin(): String

  /**
   * Return size stored data or -1 if not available
   *
   * @return Long
   */
  def size(): Long

  /**
   * Return line count for this file
   *
   * @return Long
   */
  def getLineCount: Long

  /**
   * Open non-buffered input stream, do not apply any of transformation or stream truncation,
   * This stream should return data as is.
   *
   * @return InputStream
   */
  def inputStream(): InputStream
}
