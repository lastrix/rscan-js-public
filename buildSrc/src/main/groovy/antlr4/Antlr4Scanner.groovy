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

import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel

class Antlr4Scanner {
    static def scanAntlr4Grammars(Project project, File patchesFile) {
        File generatedSrc = new File(project.buildDir, "generated-src")
        File srcDir = new File(project.projectDir, "src");
        File mainDir = new File(srcDir, "main")
        File antlr4Dir = new File(mainDir, GrammarItem.ANTLR4)
        if (!antlr4Dir.exists() || !antlr4Dir.isDirectory())
            return

        project.getExtensions()
                .findByType(IdeaModel)
                .getModule()
                .getSourceDirs()
                .add(antlr4Dir)

        def grammars = GrammarDetector.locate(generatedSrc, antlr4Dir)
        grammars.each { grammar ->
            def task
            switch (grammar.type) {
                case GrammarType.Lexer:
                    task = project.tasks.create("compileAntlr4" + grammar.grammarName, Antlr4LexerTask)
                    task.outputFile = grammar.outputFile
                    task.grammarFile = grammar.grammarFile
                    task.grammar = grammar
                    task.patchesFile = patchesFile
                    break

                case GrammarType.Parser:
                    task = project.tasks.create("compileAntlr4" + grammar.grammarName, Antlr4ParserTask)
                    task.outputFile = grammar.outputFile
                    task.grammarFile = grammar.grammarFile
                    task.grammar = grammar
                    task.patchesFile = patchesFile
                    break

                default:
                    throw new IllegalArgumentException("Unsupported grammar type: " + grammar.type)
            }

//            task.inputs.file(patchesFile)
//            task.outputs.files(grammar.outputFiles)
            project.compileJava.dependsOn task
        }
    }


}
