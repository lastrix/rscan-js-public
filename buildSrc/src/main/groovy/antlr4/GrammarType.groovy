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

enum GrammarType {
    Lexer,
    Parser

    static final String LEXER_SUFFIX = "Lexer.g4"
    static final String PARSER_SUFFIX = "Parser.g4"

    static GrammarType forFileName(String fileName) {
        if (fileName.endsWith(LEXER_SUFFIX))
            return Lexer
        if (fileName.endsWith(PARSER_SUFFIX))
            return Parser

        throw new IllegalArgumentException("Unable to determine grammar type for name: " + fileName)
    }
}
