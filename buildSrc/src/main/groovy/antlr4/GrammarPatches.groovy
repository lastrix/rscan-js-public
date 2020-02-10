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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

class GrammarPatches {
    @JsonProperty(required = true)
    private List<GrammarPatch> lexerPatches
    @JsonProperty(required = true)
    private List<GrammarPatch> parserPatches

    GrammarPatches lexer(String regexp, String value) {
        if (lexerPatches == null)
            lexerPatches = new ArrayList<>()
        lexerPatches.add(new GrammarPatch(regexp, value))
        return this
    }

    GrammarPatches parser(String regexp, String value) {
        if (parserPatches == null)
            parserPatches = new ArrayList<>()
        parserPatches.add(new GrammarPatch(regexp, value))
        return this
    }

    ////////////////////////////// Operations //////////////////////////////////////////////////////////////////////////
    String applyParser(String source, String language) {
        String result = source
        for (GrammarPatch patch : parserPatches)
            result = patch.apply(result, language)
        return result
    }

    String applyLexer(String source, String language) {
        String result = source
        for (GrammarPatch patch : lexerPatches)
            result = patch.apply(result, language)
        return result
    }

    ////////////////////////////// IO //////////////////////////////////////////////////////////////////////////////////
    static GrammarPatches load(File file) {
        return MAPPER.readValue(file, GrammarPatches)
    }

    void save(File file) throws IOException {
        MAPPER.writeValue(file, this)
    }

    private static final ObjectMapper MAPPER = new ObjectMapper()
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    List<GrammarPatch> getLexerPatches() {
        return lexerPatches
    }

    void setLexerPatches(List<GrammarPatch> lexerPatches) {
        this.lexerPatches = lexerPatches
    }

    List<GrammarPatch> getParserPatches() {
        return parserPatches
    }

    void setParserPatches(List<GrammarPatch> parserPatches) {
        this.parserPatches = parserPatches
    }
}
