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

package org.lastrix.rscan.app

import java.io.{File, FileNotFoundException, InputStream}
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.apache.commons.io.{FileUtils, IOUtils}
import org.lastrix.rscan.api.parser.antlr4.ParserConfig
import org.lastrix.rscan.api.parser.{ParserService, antlr4}
import org.lastrix.rscan.lang.{Language, LanguageDetector, LanguageRef}
import org.lastrix.rscan.model.operation.std.RLangOp
import org.lastrix.rscan.vfs
import org.lastrix.rscan.vfs.{VirtualFile, VirtualFileSystem}
import org.slf4j.{Logger, LoggerFactory}

object GrammarTester {
  private val log: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (args.length != 2) throw new IllegalArgumentException("Two arguments expected: <language> <path-to-source-dir>")
    val lang = Language.resolve(args(0)).ref
    val path = new File(args(1))

    def isOnlyLang(x: Set[LanguageRef]): Boolean = x.size == 1 && x.contains(lang)

    val vfs = new VirtualFileSystem()
    try {
      vfs.register(path)
      val impl = new Impl(lang)
      val files = vfs.root.listFilesRecursive(x => isOnlyLang(LanguageDetector.apply(x))).sorted
      files
        //        .filter(!_.absoluteName.endsWith(".min.js"))
        //.filter(_.absoluteName == "/node_modules/babel-preset-react-app/node_modules/@babel/plugin-transform-computed-properties/node_modules/@babel/core/lib/parser/index.js")
        .foreach(x => impl.parse(x))
      log.info(s"Parsing completed, SuccessCount = ${impl.successCount}, FailCount = ${impl.failCount}, Total = ${files.size}")
    } finally {
      vfs.close()
    }
  }

  private sealed class Impl(val lang: LanguageRef) {
    private val cacheFolder = new File(System.getProperty("rscan.tester.cache.dir", FileUtils.getTempDirectoryPath))
    private val ifGrammarChanged = java.lang.Boolean.parseBoolean(System.getProperty("rscan.tester.on.grammar.change", "false"))
    private val invalidate = java.lang.Boolean.parseBoolean(System.getProperty("rscan.tester.invalidate", "false"))
    private val dumpResults = java.lang.Boolean.parseBoolean(System.getProperty("rscan.tester.dump", "false"))
    private implicit val ps: ParserService = ParserService.apply(lang)
    private val grammarChanged = {
      val lexerCheckSum = evalCheckSum(ps.lexerClass)
      val parserCheckSum = evalCheckSum(ps.parserClass)
      try {
        checkLangChecksum(lexerCheckSum, "lexer") ||
          checkLangChecksum(parserCheckSum, "parser")
      } finally {
        saveChecksum(lexerCheckSum, "lexer")
        saveChecksum(parserCheckSum, "parser")
      }
    }

    var failCount: Int = 0
    var successCount: Int = 0

    def parse(f: VirtualFile): Unit = {
      val lockFile = createItemFile(f)
      if (checkFile(lockFile)) {
        FileUtils.deleteQuietly(lockFile)
        // parse the file
        if (parseFileSafely(f))
          FileUtils.writeStringToFile(lockFile, "true", StandardCharsets.UTF_8)
      }
    }

    private def saveResults(op: RLangOp, f: VirtualFile): Unit = {
      val sb = new StringBuilder
      op.prettyPrint(sb)
      val base = dumpFolderFor(f)
      val sourceFile = new File(base, f.name)
      val dumpFile = new File(base, f.name + ".txt")
      FileUtils.writeStringToFile(sourceFile, vfs.toPlainText(f), StandardCharsets.UTF_8)
      FileUtils.writeStringToFile(dumpFile, sb.toString, StandardCharsets.UTF_8)
    }

    private def parseFileSafely(f: VirtualFile): Boolean = try {
      val r = antlr4.parseFile(f, config = ParserConfig(additionalSuccessMsg = Some(", size = " + f.source.size() / 1024 + " kb")))
      if (dumpResults) saveResults(r, f)
      successCount += 1
      true
    } catch {
      case e: Throwable =>
        log.error(s"Failed to parse file ${f.absoluteName}: ", e)
        failCount += 1
        false
    }

    private def checkFile(lockFile: File): Boolean = {
      if (invalidate) true
      else if (ifGrammarChanged && grammarChanged) true
      else !lockFile.exists()
    }

    private def saveChecksum(checksum: String, suffix: String): Unit =
      FileUtils.writeStringToFile(createLangFile(suffix), checksum, StandardCharsets.UTF_8)

    private def checkLangChecksum(checksum: String, suffix: String): Boolean = {
      val prevValue =
        try FileUtils.readFileToString(createLangFile(suffix), StandardCharsets.UTF_8)
        catch {
          case _: Throwable => ""
        }
      prevValue != checksum
    }

    private def createLangFile(suffix: String): File = {
      if (!cacheFolder.exists() && !cacheFolder.mkdirs() || !cacheFolder.isDirectory)
        throw new IllegalStateException

      new File(cacheFolder, lang.name + "_" + suffix)
    }

    private def createItemFile(f: VirtualFile): File = {
      val file = new File(cacheFolder, f.absoluteName)
      assertFolderExist(file.getParentFile)
      file
    }

    private def dumpFolderFor(f: VirtualFile): File = {
      val folder = new File(new File(cacheFolder, "__dump"), f.parent.absoluteName)
      assertFolderExist(folder)
      folder
    }

    private def assertFolderExist(folder: File): Unit =
      if (!folder.exists() && !folder.mkdirs() || !folder.isDirectory)
        throw new IllegalStateException
  }

  val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

  private def evalCheckSum(aClass: Class[_]): String = {
    val resourceName = aClass.getName.replace('.', '/') + ".class"
    var is: InputStream = null
    try {
      is = aClass.getClassLoader.getResourceAsStream(resourceName)
      if (is == null) throw new FileNotFoundException("Unable to resolve resource: " + aClass.getTypeName)
      val bytes = IOUtils.toByteArray(is)
      digest.digest(bytes)
        .map("%02x".format(_))
        .mkString
    } finally if (is != null) is.close()
  }
}
