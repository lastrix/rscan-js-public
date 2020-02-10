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

package antlr4

class GrammarItem implements Serializable {
    static final String ANTLR4 = "antlr4"
    static final String EXT = ".java"

    final GrammarType type
    final String grammarName
    final File grammarFile
    final File targetFolder
    final File outputFile

    GrammarItem(File targetFolder, File grammarFile) {
        this.grammarFile = grammarFile
        this.grammarName = basename(grammarFile)
        this.targetFolder = toTargetFolder(grammarFile, targetFolder)
        type = GrammarType.forFileName(grammarFile.getName())
        outputFile = new File(
                this.targetFolder,
                grammarName + EXT)
    }

    private static File toTargetFolder(File grammarFile, File targetFolder) {
        String path = grammarFile.getParentFile().getAbsolutePath()
        def needle = ("src/main/" + ANTLR4).replace((char) '/', File.separatorChar)
        int idx = path.lastIndexOf(needle)
        if (idx == -1)
            throw new IllegalArgumentException("Not in antlr4 folder: " + grammarFile)

        String relPath = path.substring(idx + needle.length() + 1)
        return new File(targetFolder, relPath)
    }

    static String basename(File file) {
        def name = file.getName()
        def idx = name.lastIndexOf('.')
        if (idx < 0)
            return name
        return name.substring(0, idx)
    }
}
