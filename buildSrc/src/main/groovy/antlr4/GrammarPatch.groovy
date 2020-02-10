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

import java.util.regex.Pattern

class GrammarPatch {
    @JsonProperty(required = true)
    private String regexp
    @JsonProperty(required = true)
    private String value

    GrammarPatch() {
    }

    GrammarPatch(String regexp, String value) {
        Objects.requireNonNull(regexp)
        assertRegExp(regexp)
        Objects.requireNonNull(value)
        this.regexp = regexp
        this.value = value
    }

    private static boolean assertRegExp(String value) {
        try {
            Pattern.compile(value)
            return true
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid regexp value: " + value, e)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    String apply(String source, String grammarName) {
        String replacement = value.replace("##{grammarName}##", grammarName)
        return Pattern.compile(regexp)
                .matcher(source)
                .replaceAll(replacement)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    String getRegexp() {
        return regexp
    }

    void setRegexp(String regexp) {
        this.regexp = regexp
    }

    String getValue() {
        return value
    }

    void setValue(String value) {
        this.value = value
    }
}
