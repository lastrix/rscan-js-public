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

package org.lastrix.rscan.model.operation.raw;

import org.lastrix.rscan.model.operation.ROpType;

public enum RawOpType implements ROpType {
    RAW_CALLABLE,
    RAW_CALL,
    RAW_NEW,
    RAW_DECL_NAMESPACE,
    RAW_DECL_CONSTRUCTOR,
    RAW_DECL_PROPERTY,
    RAW_DECL_FUNCTION,
    RAW_DECL_METHOD,
    RAW_DECL_LAMBDA,
    RAW_DECL_PARAMETER,
    RAW_DECL_LOCAL,
    RAW_DECL_INDEX,
    RAW_DECL_CLASS,
    RAW_DECL_ENUM,
    RAW_DECL_ENUM_MEMBER,
    RAW_DECL_TYPE_PARAMETER,
    RAW_ARRAY,
    RAW_ARRAY_ITEM,
    RAW_DECL_INTERFACE,
    RAW_OBJECT,
    RAW_OBJECT_BINDING,
    RAW_OBJECT_BINDING_ITEM,
    RAW_OBJECT_MEMBER,
    RAW_ARRAY_BINDING,
    RAW_ARRAY_BINDING_ITEM,
    RAW_MODULE,
    RAW_ARRAY_TYPE,
    RAW_TYPE,
    RAW_TYPE_REFERENCE,
    RAW_TYPE_FUNCTION,
    RAW_TYPE_CONSTRUCTOR,
    RAW_TYPE_TUPLE,
    RAW_TYPE_SELECT,
    RAW_TYPE_PREDICATE,
    RAW_TYPE_CONSTRAINT,
    RAW_TYPE_CONSTRAINT_0,
    RAW_TYPE_CONSTRAINT_1,
    RAW_TYPE_CONSTRAINT_2,
    RAW_TYPE_CONSTRAINT_3,
    RAW_TYPE_CONSTRAINT_4,

    RAW_DECL_ALIAS,
    ;


    @Override
    public boolean isRaw() {
        return true;
    }

    @Override
    public boolean hasOwnScope() {
        return false;
    }

    @Override
    public boolean isDecl() {
        return false;
    }

    @Override
    public boolean isDiscarded() {
        return false;
    }
}
