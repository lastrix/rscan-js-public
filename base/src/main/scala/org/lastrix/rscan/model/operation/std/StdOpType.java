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

package org.lastrix.rscan.model.operation.std;

import org.lastrix.rscan.model.operation.ROpType;

public enum StdOpType implements ROpType {
    NONE,
    DISCARDED,
    SPECIAL,
    LIST,
    FILE_ROOT(true, false),
    FOLD,
    LANG(true, false),
    NAME,
    PROPS,
    MODIFIERS,
    EXPR,
    EXPR_LIST,
    INIT(true, false),
    BLOCK(true, false),
    /**
     * This one is not a block, but special casing for statements
     */
    BLOCK_WRAP(false, false),
    BLOCK_CONDITIONAL(true, false),
    CONDITION(true, false),
    ASSIGN,
    TERNARY,
    BINARY,
    UNARY,
    LITERAL,
    PARENTHESIZED,
    CHAIN,
    UNRESOLVED_ID,
    RETURN,
    CONTINUE,
    BREAK,
    THROW,
    IF,
    TRY,
    CATCH,
    FINALLY,
    CASE,
    CASE_ITEM,
    LABEL,
    EXTENDS,
    IMPLEMENTS,
    TEMPLATE_PARSED,
    TEMPLATE_TEXT,
    SUPER,
    THIS,
    IMPORT,
    EXPORT,
    TYPE,
    TYPE_PARAMETERS,
    TYPE_ARGUMENTS,
    TYPE_EXPR,
    TYPE_CONSTRAINT,
    TYPE_UNION,
    TYPE_INTERSECTION,
    TYPE_CAST,
    MEMBERS,
    DECL(false, true),
    KEY,
    ARRAY_ACCESSOR,
    BUILTIN_CALL,
    DEFAULT,
    FROM,
    DECLARE,
    NAMESPACE,
    DEBUG,
    FOR_OF,
    FOR_IN,
    FOR_INIT,
    FOR_CONDITION,
    FOR_UPDATE;

    private final boolean block;
    private final boolean decl;

    StdOpType() {
        this(false, false);
    }

    StdOpType(boolean block, boolean decl) {
        this.block = block;
        this.decl = decl;
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    @Override
    public boolean hasOwnScope() {
        return block;
    }

    @Override
    public boolean isDecl() {
        return decl;
    }

    @Override
    public boolean isDiscarded() {
        return this == DISCARDED;
    }
}
