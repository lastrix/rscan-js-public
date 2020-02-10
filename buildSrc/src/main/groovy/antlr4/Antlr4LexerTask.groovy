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

import org.antlr.v4.Tool
import org.antlr.v4.tool.Grammar
import org.antlr.v4.tool.ast.GrammarRootAST
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Antlr4LexerTask extends DefaultTask {
    @Input
    GrammarItem grammar
    @InputFile
    File grammarFile
    @InputFile
    File patchesFile

    @OutputFile
    File outputFile;

    @TaskAction
    def generate() {
        println("Output to " + outputFile)
        if (!grammar.targetFolder.exists() && !grammar.targetFolder.mkdirs() || !grammar.targetFolder.isDirectory())
            throw new IllegalStateException("Unable to mkdirs: " + grammar.targetFolder)
        generateLexer(GrammarPatches.load(patchesFile))
        File lexerTokensFile = new File(grammar.targetFolder, grammar.grammarName + "Lexer.tokens")
        if (lexerTokensFile.exists() && !lexerTokensFile.delete())
            throw new IllegalStateException("Unable to remove file: " + lexerTokensFile)
    }

    private void generateLexer(GrammarPatches patches) throws IOException {
        // generate lexer
        Tool tool = new Tool(
                grammarFile.absolutePath,
                "-o", grammar.targetFolder.absolutePath,
                "-no-listener",
                "-no-visitor")

        GrammarRootAST lexerAst = tool.parseGrammar(grammarFile.absolutePath)
        Grammar lexer = new Grammar(tool, lexerAst)
        lexer.fileName = grammarFile.absolutePath

        tool.processGrammarsOnCommandLine()

        outputFile.text = patches.applyLexer(outputFile.text, grammar.grammarName)

//        String lexerBody = FileUtils.readFileToString(outputFile, Charsets.UTF_8)
//        lexerBody = patches.applyLexer(lexerBody, grammar.grammarName)
//        FileUtils.writeStringToFile(outputFile, lexerBody, Charsets.UTF_8)
    }
}
