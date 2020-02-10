parser grammar JSParser;

options { tokenVocab = JSLexer; }

@header
{
package org.lastrix.rscan.lang.javascript.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.lastrix.rscan.model.*;
import org.lastrix.rscan.model.tokens.RScanToken;
import org.lastrix.rscan.vfs.*;
import org.lastrix.rscan.model.operation.*;
import org.lastrix.rscan.model.operation.raw.*;
import org.lastrix.rscan.model.operation.std.*;
import org.lastrix.rscan.lang.javascript.meta.*;
import org.lastrix.rscan.lang.javascript.parser.model.operation.*;

import java.util.*;
}

//////////////////////////////////////// Main Entry Points /////////////////////////////////////////////////////////////
startJavaScript returns [@NotNull RLangOp result]
    :   { boolean functionMode = false; }
        (HashBangLine { functionMode = true; })?
        jsFileBody[functionMode] EOF
        { $result = opLang($jsFileBody.start, $jsFileBody.stop, $jsFileBody.result); }
    |   EOF { $result = opEmptyLang(); }
    ;

//////////////////////////////////////// Secondary Entry Points ////////////////////////////////////////////////////////
startTemplate returns [@NotNull ROp result]
locals[List<ROp> list]
@init{ $list = new ArrayList<>(); }
    :   { Boolean _yield = boolOption("yield"); Boolean await = boolOption("await"); }
        TemplateLiteralStart
        { if (!StringUtils.isBlank($TemplateLiteralStart.text)) $list.add(stringLiteral($TemplateLiteralStart)); }
        (
            expression[true,_yield,await] { $list.add($expression.result); }
            (
                TemplateLiteralMiddle
                { if (!StringUtils.isBlank($TemplateLiteralMiddle.text)) $list.add(stringLiteral($TemplateLiteralMiddle)); }
                expression[true,_yield,await] { $list.add($expression.result); }
            )*
            TemplateLiteralStop
            { if (!StringUtils.isBlank($TemplateLiteralStop.text)) $list.add(stringLiteral($TemplateLiteralStop)); }
        )?
        EOF
        { $result = opNode(StdOpType.TEMPLATE_PARSED, $list); }
    ;

startFoldBlock returns [@NotNull ROp result]
   :   block[boolOption("yield"), boolOption("await"), boolOption("_return")]
        EOF
       { $result = $block.result; }
   ;

startObjectLiteral returns [@NotNull ROp result]
    :   objectLiteral[boolOption("yield"), boolOption("await")]
        EOF
        { $result = $objectLiteral.result; }
    ;

startSwitchBody returns [@NotNull ROp result]
    :   caseBlock[boolOption("yield"), boolOption("await"), boolOption("_return")]
        EOF
        { $result = $caseBlock.result; }
    ;

startClassMembers returns [@NotNull ROp result]
locals[List<ROp> list]
@init{ $list = new ArrayList<>(); }
    :   classMembers[boolOption("yield"), boolOption("await")]
        EOF
        { $result = $classMembers.result; }
    ;

startFunctionBody returns [@NotNull ROp result]
    :   functionBody[boolOption("yield"), boolOption("await")]
        EOF
        { $result = $functionBody.result; }
    ;

startObjectBinding returns [@NotNull ROp result]
    :   objectBinding[boolOption("yield"), boolOption("await")]
        EOF
        { $result = $objectBinding.result; }
    ;

startExportClause returns[@NotNull ROp result]
    :   exportClause
        EOF
        { $result = $exportClause.result; }
    ;

startNamedImports returns[@NotNull ROp result]
    :   namedImports
        EOF
        { $result = opNode(evalStatement($namedImports.start, $namedImports.stop), StdOpType.LIST, $namedImports.result); }
    ;

startExpressionListBlock returns[@NotNull ROp result]
    :   LBRACE
            expressionList[true, boolOption("yield"), boolOption("await")]
        RBRACE
        EOF
        { $result = $expressionList.result; }
    ;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Module
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
jsFileBody[boolean functionMode] returns [List<ROp> result]
@init{ $result = new ArrayList<>(); }
    :   (jsFileItem[$functionMode] { $result.add($jsFileItem.result);})+
    ;

jsFileItem[boolean functionMode] returns [@NotNull ROp result]
    :   importStmt                              { $result = $importStmt.result; }
    |   exportStmt                              { $result = $exportStmt.result; }
    |   statement[false,false,$functionMode]    { $result = $statement.result; }
    ;

importStmt returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), JsOpType.IMPORT_CLAUSE, $list); }
    :   KWF_IMPORT
        (   importClause                { $list.addAll($importClause.result); }
            fromClause                  { $list.add($fromClause.result); }
        |   StringLiteral               { $list.add(stringLiteral($StringLiteral)); }
        |   LPAREN StringLiteral RPAREN { $list.add(stringLiteral($StringLiteral)); }
            // moduleSpecifier
        )
        endOfStmt
    ;

importClause returns [@NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   importedBinding { $result.add( opNode($importedBinding.result.statement(), StdOpType.DEFAULT, $importedBinding.result) ); }
        (
            COMMA
            (   nameSpaceImport { $result.add($nameSpaceImport.result); }
            |   namedImports    { $result.addAll($namedImports.result); }
            )
        )?
    |   nameSpaceImport { $result.add($nameSpaceImport.result); }
    |   namedImports    { $result.addAll($namedImports.result); }
    ;

importedBinding returns [@NotNull ROp result]
    :   identifier[false,false]
        { $result = $identifier.result; }
    ;

nameSpaceImport returns [@NotNull ROp result]
    :   MUL
        (   KW_AS importedBinding
            {
                $result = opNode(evalStatement($MUL, $importedBinding.stop), JsOpType.IMPORT_NAMED,
                        Arrays.asList(
                            opUnresolvedId($MUL),
                            opNode($importedBinding.result.statement(), JsOpType.IMPORT_ALIAS, $importedBinding.result)
                        )
                );
            }
        |   { $result = opNode(JsOpType.ALL, $MUL); }
        )
    ;

namedImports returns [@NotNull List<ROp> result]
@init { $result = Collections.emptyList(); }
    :   LBRACE
            (importsList { $result = $importsList.result; } COMMA?)?
        RBRACE
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            $result = Collections.singletonList(opFoldBlock(token, "startNamedImports"));
        }
    ;


importsList returns [@NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   importSpecifier { $result.add($importSpecifier.result); }
        (COMMA importSpecifier { $result.add($importSpecifier.result); })*
    ;

importSpecifier returns [@NotNull ROp result]
    :   importedBinding { $result = opNode($importedBinding.result.statement(), JsOpType.IMPORT_NAMED, $importedBinding.result); }
    |   identifier[false,false] KW_AS importedBinding
        {
            $result = opNode(evalStatement($identifier.start, $importedBinding.stop), JsOpType.IMPORT_NAMED,
                    Arrays.asList(
                        opUnresolvedId($identifier.start),
                        opNode($importedBinding.result.statement(), JsOpType.IMPORT_ALIAS, $importedBinding.result)
                    )
            );
        }
    |   KW_DEFAULT KW_AS importedBinding
        {
            $result = opNode(evalStatement($KW_DEFAULT, $importedBinding.stop), JsOpType.IMPORT_NAMED,
                    Arrays.asList(
                        opUnresolvedId($KW_DEFAULT),
                        opNode($importedBinding.result.statement(), JsOpType.IMPORT_ALIAS, $importedBinding.result)
                    )
            );
        }
    ;

exportStmt returns [@NotNull ROp result]
    :   KWF_EXPORT
        (   KW_DEFAULT
            (   functionDeclaration[false,false, true]
                { $result = $functionDeclaration.result; }
            |   classDeclaration[false,false,true]
                { $result = $classDeclaration.result; }
            |   expression[true,false,false]
                { $result = $expression.result; }
            )
        |   (MUL | exportClause) fromClause
        |   exportClause
        |   variableStmt[false,false]  { $result = $variableStmt.result; }
        |   functionDeclaration[false,false, true]  { $result = $functionDeclaration.result; }
        |   classDeclaration[false,false,true] { $result = $classDeclaration.result; }
        )
        endOfStmt
        { $result = opNone($KWF_EXPORT); } // TODO: export declaration support
    ;

exportClause returns [@NotNull ROp result]
    :   LBRACE
            (exportList { $result = $exportList.result; } COMMA?)?
        RBRACE
        { if ( $result == null ) $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.NONE, Collections.emptyList()); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            $result = opFoldBlock(token, "startExportClause");
        }
    ;

exportList returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(JsOpType.EXPORT_LIST, $list); }
    :   exportSpecifier { $list.add($exportSpecifier.result); }
        (COMMA exportSpecifier { $list.add($exportSpecifier.result); })*
    ;

exportSpecifier returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(JsOpType.EXPORT_SPEC, $list); }
    :   identifier[false,false] { $list.add(opName($identifier.result)); }
        (   KW_AS identifier[false,false]
            { $list.add(opNode(evalStatement($KW_AS, $identifier.stop), JsOpType.AS, $identifier.result)); }
        )?
    ;

fromClause returns [@NotNull ROp result]
    :   KW_FROM StringLiteral
        { $result = opNode(evalStatement($KW_FROM, $StringLiteral), StdOpType.FROM, stringLiteral($StringLiteral)); }
    ;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Classes
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
classExpr[boolean yield, boolean await] returns [@NotNull ROp result]
    :   classDeclaration[$yield,$await,true]
        { $result = $classDeclaration.result; }
    ;

classDeclaration[boolean yield, boolean await, boolean _default] returns [@NotNull ROp result]
locals [ List<ROp> list, List<RModifier> modifiers ]
@init { $list = new ArrayList<>(); $modifiers = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start,$stop);
    if ( !$modifiers.isEmpty() ) $list.add(opModifiers(st,$modifiers));
    $result = opNode(st, RawOpType.RAW_DECL_CLASS, $list);
}
    :   KWF_CLASS
        (   identifier[$yield,$await]    { $list.add(opName($identifier.result));  }
        |   {$_default}?
        )
        (   KWF_EXTENDS
            expression[false,$yield,$await]
            { $list.add(opNode(evalStatement($KWF_EXTENDS, $expression.stop), StdOpType.EXTENDS, $expression.result)); }
        )?
        classMembers[$yield, $await]
        { $list.add($classMembers.result); }
    ;

classMembers[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ String className, List<ROp> list, Token _stop ]
@init { $list = new ArrayList<>(); }
    :   LBRACE
            (classElement[$yield,$await] { $list.add($classElement.result); } | SEMI )*
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), StdOpType.MEMBERS, $list); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            $result = opFoldBlock(token, "startClassMembers");
        }
    ;

classElement[boolean yield, boolean await] returns [@NotNull ROp result]
    :   classMethod[$yield,$await]      { $result = $classMethod.result; }
    |   classProperty[$yield,$await]    { $result = $classProperty.result; }
    ;

classProperty[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ List<ROp> list, boolean _private ]
@init { $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    if ( $_private ) $list.add(opModifiers(st, Collections.singletonList(JSModifiers.Private())));
    $result = opNode(st, RawOpType.RAW_DECL_PROPERTY, $list);
}
    :   (HASH { $_private = true; })?
        propertyName[$yield,$await]       { $list.add($propertyName.result); }
        ASSIGN
        expression[false,$yield,$await]   { $list.add($expression.result); }
    ;

classMethod[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ boolean async, boolean generator, List<RModifier> modifiers, List<ROp> list ]
@init { $modifiers = new ArrayList<>(); $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    if ( !$modifiers.isEmpty() ) $list.add(opModifiers(st, $modifiers));
    $result = opNode(st, RawOpType.RAW_DECL_METHOD, $list);
}
    :   (   KWS_STATIC {  $modifiers.add(JSModifiers.Static()); }
            (KWF_ASYNC { $async = true; $modifiers.add(JSModifiers.Async()); })?
        |   KWF_ASYNC { $async = true; $modifiers.add(JSModifiers.Async()); }
            (KWS_STATIC {  $modifiers.add(JSModifiers.Static()); })?
        )?
        (MUL { $generator = true; $modifiers.add(JSModifiers.Generator()); })?
        (HASH { $modifiers.add(JSModifiers.Private()); } )?
        (   GETTER propertyName[$yield,$await] LPAREN RPAREN
            { $list.add($propertyName.result); $modifiers.add(JSModifiers.Getter()); }
        |   SETTER propertyName[$yield,$await] LPAREN (parameterList[$yield,$await] { $list.addAll($parameterList.result); })? RPAREN
            { $list.add($propertyName.result); $modifiers.add(JSModifiers.Setter()); }
        |   propertyName[$yield,$await]       { $list.add($propertyName.result); }
            LPAREN (parameterList[$yield,$await] { $list.addAll($parameterList.result); })? RPAREN
        )
        functionBody[$generator,$async]   { $list.add($functionBody.result); }
    ;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Functions
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
functionExpr[boolean yield, boolean await]  returns [ @NotNull ROp result]
    :   functionDeclaration[$yield,$await, true]     { $result = $functionDeclaration.result; }
    ;

functionDeclaration[boolean yield, boolean await, boolean nameless] returns [ @NotNull ROp result]
locals [boolean async, boolean generator, List<ROp> list, List<RModifier> modifiers]
@init { $list = new ArrayList<>(); $modifiers = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    if ( !$modifiers.isEmpty() ) $list.add(opModifiers(st, $modifiers));
    $result = opNode(st, RawOpType.RAW_DECL_FUNCTION, $list);
}
    :   (KWF_ASYNC {$async=true; $modifiers.add(JSModifiers.Async()); })?
        KW_FUNCTION
        ( MUL {$generator=true; $modifiers.add(JSModifiers.Generator()); })?
        (   identifierName[$yield,$await]
            { $list.add(opName($identifierName.result)); }
        |   {$nameless}?
        )
        LPAREN (parameterList[$yield,$await] { $list.addAll($parameterList.result);})? RPAREN
        functionBody[$generator,$async]
        { $list.add($functionBody.result);  }
    ;

parameterList[boolean yield, boolean await] returns [@NotNull List<ROp> result]
locals [boolean optionalFlag]
@init { $result = new ArrayList<>(); }
    :   (parameter[$yield,$await,false]               { $result.add($parameter.result); $optionalFlag = $optionalFlag || $parameter.isOptional; })
        (COMMA parameter[$yield,$await,$optionalFlag] { $result.add($parameter.result); $optionalFlag = $optionalFlag || $parameter.isOptional; })*
        (COMMA restParameter[$yield,$await] { $result.add($restParameter.result); } | COMMA)?
    |   restParameter[$yield,$await] { $result.add($restParameter.result); }
    ;

parameter[boolean yield, boolean await, boolean optional] returns [@NotNull ROp result, boolean isOptional]
locals [List<RModifier> modifiers, List<ROp> list]
@init { $modifiers = new ArrayList<>(); $list = new ArrayList<>(); }
@after {
    Statement stmt = evalStatement($start,$stop);
    if ( $isOptional || $optional ) $list.add(opModifiers(stmt, Collections.singletonList(JSModifiers.Optional())));
    $result = opNode(stmt, RawOpType.RAW_DECL_PARAMETER, $list);
}
    :   assignable[$yield,$await]           { $list.add(opName($assignable.result)); }
        (   ASSIGN                          { $isOptional = true; }
            expression[false,$yield,$await] { $list.add(opNode($expression.result.statement(), StdOpType.INIT, $expression.result)); }
        )?
    ;

restParameter[boolean yield, boolean await] returns [@NotNull ROp result]
locals [List<ROp> list, Token _stop]
@init { $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start,$stop);
    $list.add(opModifiers(st, Arrays.asList(JSModifiers.VarArgs(), JSModifiers.Optional())));
    $result = opNode( st, RawOpType.RAW_DECL_PARAMETER, $list);
}
    :   ELLIPSIS assignable[$yield,$await]   { $list.add(opName($assignable.result)); }
        COMMA?
    ;

functionBody[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   { List<ROp> list = Collections.emptyList(); }
        LBRACE
            (stmtList[$yield, $await, true] { list = $stmtList.result; })?
        RBRACE
        { $result = opBlock($LBRACE, $RBRACE, list); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            $result = opFoldBlock(token, "startFunctionBody");
        }
    ;

arrowFunction[boolean _in, boolean yield, boolean await] returns [ @NotNull ROp result]
locals [boolean async, List<ROp> list, List<RModifier> modifiers]
@init { $list = new ArrayList<>(); $modifiers = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    if ( !$modifiers.isEmpty() ) $list.add(opModifiers(st, $modifiers));
    $result = opNode(st, RawOpType.RAW_DECL_LAMBDA, $list);
}
    :   (KWF_ASYNC { $async=true; $modifiers.add(JSModifiers.Async()); })?
        arrowParameters[$yield,$async] { $list.addAll($arrowParameters.result); }
        ARROW
        (   functionBody[false,$async] { $list.add($functionBody.result); }
        |   expression[true,false,$async]   { $list.add($expression.result); }
        )
    ;

arrowParameters[boolean yield, boolean await] returns [ @NotNull List<ROp> result]
    :   parameter[$yield,$await,false]
        { $result = Collections.singletonList($parameter.result); }
    |   LPAREN
            (
                parameterList[$yield,$await]
                { $result = $parameterList.result; }
            |   { $result = Collections.emptyList(); }
            )
        RPAREN
    ;

arguments[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [List<ROp> list]
@init{ $list = new ArrayList<>(); }
    :   LPAREN
        (
            argument[$yield,$await] { $list.add($argument.result); }
            (COMMA argument[$yield,$await] { $list.add($argument.result); })*
            COMMA?
        )?
        RPAREN
        { $result = opCall($LPAREN, $RPAREN, $list); }
    ;

argument[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [Token spread]
    :   (ELLIPSIS { $spread = $ELLIPSIS; })? expression[true, $yield,$await]
        { $result = $spread == null ? $expression.result : opUnary($spread, $expression.stop, UnaryType.SPREAD, $expression.result); }
    ;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Statements
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
stmtList[boolean yield, boolean await, boolean _return] returns [ @NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   (  statement[$yield,$await,$_return] { $result.add($statement.result); } )+
    ;

statement[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   SEMI                                                { $result = noOp($SEMI); }
    |   classDeclaration[$yield, $await, false]             { $result = $classDeclaration.result; }
    |   functionDeclaration[$yield, $await, false]          { $result = $functionDeclaration.result; }
    |   blockStmt[$yield,$await,$_return]                   { $result = $blockStmt.result; }
    |   continueStmt[$yield,$await]                         { $result = $continueStmt.result; }
    |   breakStmt[$yield,$await]                            { $result = $breakStmt.result; }
    |   {$_return}? returnStmt[$yield,$await]               { $result = $returnStmt.result; }
    |   withStmt[$yield,$await,$_return]                    { $result = $withStmt.result; }
    |   switchStmt[$yield,$await,$_return]                  { $result = $switchStmt.result; }
    |   throwStmt[$yield,$await]                            { $result = $throwStmt.result; }
    |   tryStmt[$yield,$await,$_return]                     { $result = $tryStmt.result; }
    |   ifStmt[$yield,$await,$_return]                      { $result = $ifStmt.result; }
    |   labelledStmt[$yield,$await,$_return]                { $result = $labelledStmt.result; }
    |   KW_DEBUGGER endOfStmt                               { $result = opNode(StdOpType.DEBUG, $KW_DEBUGGER); }
    |   iterationStmt[$yield,$await,$_return]               { $result = $iterationStmt.result; }
    |   variableStmt[$yield,$await]                         { $result = $variableStmt.result; }
    |   expressionStmt[$yield,$await]                       { $result = $expressionStmt.result; }
    ;

blockStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   block[$yield,$await,$_return] { $result = $block.result; }
    ;

block[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   { List<ROp> list = Collections.emptyList(); }
        LBRACE
            (stmtList[$yield,$await,$_return] { list = $stmtList.result; })?
        RBRACE
        { $result = opBlock($LBRACE, $RBRACE, list ); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            token.option("_return", String.valueOf($_return));
            $result = opFoldBlock(token, "startFoldBlock");
        }
    ;

throwStmt[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   KW_THROW expressionList[true, $yield, $await]
        { $result = opThrow($KW_THROW, $expressionList.stop, $expressionList.result); }
        endOfStmt
    ;

continueStmt[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   KW_CONTINUE
        (   {checkNoLineTerm()}?
            identifier[$yield,$await]
            {
                Statement stmt = evalStatement($KW_CONTINUE, $identifier.stop);
                $result = opNode(stmt, StdOpType.CONTINUE, opProps(stmt, Collections.singletonMap("label", $identifier.text)));
            }
        |   { $result = opContinue($KW_CONTINUE); }
        )
        endOfStmt
    ;

breakStmt[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   KW_BREAK
        (   {checkNoLineTerm()}?
            identifier[$yield,$await]
            {
                Statement stmt = evalStatement($KW_BREAK, $identifier.stop);
                $result = opNode(stmt, StdOpType.BREAK, opProps(stmt, Collections.singletonMap("label", $identifier.text)));
            }
        |   { $result = opBreak($KW_BREAK); }
        )
        endOfStmt
    ;

returnStmt[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   KW_RETURN
        (   {checkNoLineTerm()}?
            expressionList[true, $yield, $await]
            { $result = opReturn($KW_RETURN, $expressionList.stop, $expressionList.result); }
        |   { $result = opReturn($KW_RETURN); }
        )
        endOfStmt
    ;

withStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   KW_WITH
        LPAREN expressionList[true, $yield, $await] RPAREN
        statement[$yield, $await, $_return]
        { $result = opConditionalBlock($KW_WITH, $statement.stop, ConditionalType.WITH, opCondition($expressionList.result), $statement.result); }
    ;

ifStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
locals [ List<ROp> conditional, ROp unconditional, Token _start, Token _stop]
@init { $conditional = new ArrayList<>(); }
@after { $result = opIfStmt($_start, $_stop, $conditional, $unconditional); }
    :   KW_IF { $_start = $KW_IF; }
        LPAREN
            me=expressionList[true, $yield, $await]
        RPAREN
            mb=statement[$yield, $await, $_return]
            {
                $_stop = $mb.stop;
                $conditional.add(opConditionalBlock($LPAREN,$_stop,ConditionalType.IF_ITEM, opCondition($me.result), $mb.result));
            }
        (
            KW_ELSE KW_IF
            LPAREN
                se=expressionList[true, $yield, $await]
            RPAREN
                sb=statement[$yield, $await, $_return]
                {
                    $_stop = $sb.stop;
                    $conditional.add(opConditionalBlock($LPAREN,$_stop,ConditionalType.IF_ITEM, opCondition($se.result), $sb.result));
                }
        )*
        (
            KW_ELSE ub=statement[$yield, $await, $_return]
            { $unconditional = $ub.result; $_stop = $ub.stop; }
        )?
    ;

switchStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   KW_SWITCH
        LPAREN expressionList[true, $yield, $await] RPAREN
        caseBlock[$yield, $await, $_return]
        { $result = opSwitch($KW_SWITCH, $caseBlock.stop, opCondition($expressionList.result), $caseBlock.result ); }
    ;

caseBlock[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   { List<ROp> blocks = new ArrayList<>(); }
        LBRACE
            (caseClause[$yield, $await, $_return] { blocks.add($caseClause.result); } )*
            (
                (defaultClause[$yield, $await, $_return] { blocks.add($defaultClause.result); })
                (caseClause[$yield, $await, $_return] { blocks.add($caseClause.result); } )*
            )?
        RBRACE
        { $result = opBlock($LBRACE, $RBRACE, blocks); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            token.option("_return", String.valueOf($_return));
            $result = opFoldBlock(token, "startSwitchBody");
        }
    ;

caseClause[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
locals [Token _stop, List<ROp> list]
@init { $list = Collections.emptyList(); }
    :   KW_CASE
        expressionList[true, $yield, $await]
        COLON { $_stop = $COLON; }
        (stmtList[$yield, $await, $_return] { $list = $stmtList.result; $_stop = $stmtList.stop; })?
        { $result = opCaseItem($KW_CASE, $_stop, opCondition($expressionList.result), $list); }
    ;

defaultClause[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
locals [Token _stop, List<ROp> list]
@init { $list = Collections.emptyList(); }
    :   KW_DEFAULT
        COLON { $_stop = $COLON; }
        (stmtList[$yield, $await, $_return] { $list = $stmtList.result; $_stop = $stmtList.stop; })?
        { $result = opDefaultCaseItem($KW_DEFAULT, $_stop, $list ); }
    ;

tryStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
locals [ROp cth, ROp fin, ROp body]
@after { $result = opTry($start, $stop, $body, $cth == null ? Collections.emptyList() :Collections.singletonList($cth), $fin); }
    :   KW_TRY block[$yield,$await,$_return] { $body = $block.result; }
        (   catchBlock[$yield,$await,$_return]
            { $cth = $catchBlock.result; }
            (
                finallyBlock[$yield,$await,$_return]
                { $fin = $finallyBlock.result; }
            )?
        |   finallyBlock[$yield,$await,$_return]
            { $fin = $finallyBlock.result; }
        )
    ;

catchBlock[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
locals [ ROp condition ]
    :   KW_CATCH
        (   LPAREN assignable[$yield,$await] RPAREN { $condition = $assignable.result; }
        |   { $condition = opNone($KW_CATCH); }
        )
        block[$yield,$await,$_return]
        { $result = opConditionalBlock($KW_CATCH, $block.stop, ConditionalType.CATCH, opCondition($condition), $block.result); }
    ;

finallyBlock[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   KW_FINALLY block[$yield,$await,$_return]
        { $result = opNode(StdOpType.FINALLY, $block.result); }
    ;

labelledStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   identifier[$yield,$await]
        COLON
        statement[$yield, $await, $_return]
        { $result = opLabel($identifier.start, $statement.stop, $identifier.text, $statement.result); }
    ;

expressionStmt[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   {!checkLBrace() && !checkFunction()}?
        expressionList[true, $yield, $await]
        { $result = $expressionList.result;}
        endOfStmt
    ;

variableStmt[boolean yield, boolean await] returns [ @NotNull ROp result]
    :   varModifier
        variableDeclarationList[true,$yield,$await,$varModifier.result]
        { $result = $variableDeclarationList.result; }
        endOfStmt
    ;

varModifier returns [@NotNull RModifier result]
    :   KW_VAR     { $result = JSModifiers.Var(); }
    |   KWF_CONST   { $result = JSModifiers.Const(); }
    |   KWS_LET     { $result = JSModifiers.Let(); }
    ;

variableDeclarationList[boolean _in, boolean yield, boolean await, RModifier declType] returns [ @NotNull ROp result]
locals [List<ROp> list]
@init { $list = new ArrayList<>(); }
@after { $result = $list.size() == 1 ? $list.get(0) : opNode(StdOpType.EXPR_LIST, $list); }
    :   variableDeclarationItem[$_in, $yield, $await, $declType] { $list.add($variableDeclarationItem.result); }
        (COMMA variableDeclarationItem[$_in, $yield, $await, $declType] { $list.add($variableDeclarationItem.result); })*
    ;

variableDeclarationItem[boolean _in, boolean yield, boolean await, RModifier declTypeModifier] returns [ @NotNull ROp result]
locals [ List<ROp> list]
@init { $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    $list.add(opModifiers(st, Collections.singletonList($declTypeModifier)));
    $result = opNode(st, RawOpType.RAW_DECL_LOCAL, $list);
}
    :   assignable[$yield,$await]          { $list.add(opName($assignable.result)); }
        (   ASSIGN
            expression[$_in,$yield,$await]
            { $list.add(opNode($expression.result.statement(), StdOpType.INIT, $expression.result)); }
        )?
    ;

iterationStmt[boolean yield, boolean await, boolean _return] returns [ @NotNull ROp result]
    :   KW_DO statement[$yield,$await,$_return] KW_WHILE LPAREN expressionList[true,$yield,$await] RPAREN SEMI?
        { $result = opConditionalBlock(
                        $KW_DO,
                        $RPAREN,
                        ConditionalType.DO_WHILE,
                        opCondition($expressionList.result),
                        blockWrapOrNull(Collections.singletonList($statement.result)) );
        }
    |   KW_WHILE LPAREN expressionList[true,$yield,$await] RPAREN statement[$yield,$await,$_return]
        { $result = opConditionalBlock(
                        $KW_WHILE,
                        $statement.stop,
                        ConditionalType.WHILE,
                        opCondition($expressionList.result),
                        blockWrapOrNull(Collections.singletonList($statement.result)) );
        }
    |   {$await}? KW_FOR KWF_AWAIT
        forConditionAwait[$yield,$await]
        statement[$yield,$await,$_return]
        { $result = opConditionalBlock(
                        $KW_FOR,
                        $statement.stop,
                        ConditionalType.FOR,
                        $forConditionAwait.result,
                        blockWrapOrNull(Collections.singletonList($statement.result)) );
          $result.add(opProps(evalStatement($KW_FOR, $statement.stop), Collections.singletonMap("await", "true")));
        }
    |   KW_FOR
        forCondition[$yield,$await]
        statement[$yield,$await,$_return]
        { $result = opConditionalBlock(
                        $KW_FOR,
                        $statement.stop,
                        ConditionalType.FOR,
                        $forCondition.result,
                        blockWrapOrNull(Collections.singletonList($statement.result)) );
        }
    ;

forConditionAwait[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [List<ROp> list, ROp from]
@init { $list = new ArrayList<>(); }
    :   LPAREN
            (   varModifier variableDeclarationList[true,$yield,$await,$varModifier.result]
                { $from = $variableDeclarationList.result; }
            |   expression[false, $yield,$await]
                { $from = $expression.result; }
            )
            OF expressionList[false,$yield,$await]
            { $list.add(opNode(StdOpType.FOR_OF, Arrays.asList($from, $expressionList.result))); }
        RPAREN
        { $result = opNode(evalStatement($LPAREN, $RPAREN), StdOpType.CONDITION, asArray($list)); }
    ;

forCondition[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [List<ROp> list, ROp from]
@init { $list = new ArrayList<>(); }
    :   LPAREN
        (
        // the for in variant
            (   varModifier variableDeclarationList[true,$yield,$await,$varModifier.result]
                { $list.add(opNode(StdOpType.FOR_INIT, $variableDeclarationList.result)); }
            |   expression[false, $yield,$await]
                { $list.add(opNode(StdOpType.FOR_INIT, $expression.result)); }
            )
            KW_IN expressionList[true, $yield,$await]
            { $list.add(opNode(StdOpType.FOR_IN, $expressionList.result)); }

        // the for of variant
        |   (   varModifier variableDeclarationList[true,$yield,$await,$varModifier.result]
                { $list.add(opNode(StdOpType.FOR_INIT, $variableDeclarationList.result)); }
            |   expression[false, $yield,$await]
                { $list.add(opNode(StdOpType.FOR_INIT, $expression.result)); }
            )
            OF expressionList[true,$yield,$await]
            { $list.add(opNode(StdOpType.FOR_OF, $expressionList.result)); }
        // c style for
            // init
        |   (
                varModifier variableDeclarationList[true,$yield,$await,$varModifier.result]
                { $list.add(opNode(StdOpType.FOR_INIT, $variableDeclarationList.result)); }
            |   expressionList[false, $yield,$await]
                { $list.add(opNode(StdOpType.FOR_INIT, $expressionList.result)); }
            )?
            SEMI
            // condition
            (expressionList[true, $yield,$await] { $list.add(opNode(StdOpType.FOR_CONDITION, $expressionList.result)); })? SEMI
            // update
            (expressionList[true, $yield,$await] { $list.add(opNode(StdOpType.FOR_UPDATE, $expressionList.result)); })?

        )
        RPAREN
        { $result = opNode(evalStatement($LPAREN, $RPAREN), StdOpType.CONDITION, asArray($list)); }
    ;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Expressions
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
expressionList[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = ( $list.size() == 1 ) ? $list.get(0) : opNode(StdOpType.EXPR_LIST, $list); }
    :   expression[$_in, $yield, $await]        { $list.add($expression.result); }
        (COMMA expression[$_in, $yield, $await] { $list.add($expression.result); })*
    ;

expression[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
@after { $result = opNode(StdOpType.EXPR, $result); }
    :   {$yield}? yieldExpr[true,$await]                    { $result = $yieldExpr.result; }
    |   // plain assignments
        left=atomExpr[$yield,$await]
        operator=assignOperator
        right=expression[$_in, $yield,$await]
        { $result = opAssign($left.result, $operator.result, $right.result); }
    |   arrowFunction[$_in, $yield,$await] { $result = $arrowFunction.result; }
    |   ternary[$_in, $yield,$await] { $result = $ternary.result; }
    ;

yieldExpr[boolean _in, boolean await] returns[@NotNull ROp result]
    :   KWS_YIELD
        (   {checkNoLineTerm()}?
            { boolean mul = false; }
            (MUL { mul = true; })?
            expression[$_in, true, $await]
            { $result = opBuiltin($KWS_YIELD, $expression.stop, $KWS_YIELD.text + (mul?"*":""), $expression.result); }
        |   { $result = opBuiltin($KWS_YIELD, $KWS_YIELD, $KWS_YIELD.text, null); }
        )
    ;

assignOperator returns [@NotNull  AssignType result ]
    :   ASSIGN             { $result = AssignType.DEFAULT; }
    |   ASSIGN_MUL         { $result = AssignType.MUL; }
    |   ASSIGN_DIV         { $result = AssignType.DIV; }
    |   ASSIGN_MOD         { $result = AssignType.MOD; }
    |   ASSIGN_ADD         { $result = AssignType.ADD; }
    |   ASSIGN_SUB         { $result = AssignType.SUB; }
    |   ASSIGN_SHL         { $result = AssignType.SHL; }
    |   ASSIGN_SHR         { $result = AssignType.SHR; }
    |   ASSIGN_SHR_LOGICAL { $result = AssignType.SHR_LOGICAL; }
    |   ASSIGN_BW_AND      { $result = AssignType.BW_AND; }
    |   ASSIGN_BW_XOR      { $result = AssignType.BW_XOR; }
    |   ASSIGN_BW_OR       { $result = AssignType.BW_OR; }
    |   ASSIGN_POW         { $result = AssignType.POW; }
    ;

ternary[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
    :   condition = orExpr[$_in, $yield,$await] { $result = $condition.result; }
        (
            QUESTION
                trueExpr = expression[$_in, $yield,$await]
            COLON
                falseExpr = expression[$_in, $yield,$await]
            { $result = opTernary($condition.result, $trueExpr.result, $falseExpr.result); }
        )?
    ;

orExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.OR, $list); }
    :   left = andExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($left.result);
            }
            (
                operator = OR
                right = orExpr[$_in, $yield,$await]
                { $list.add($right.result); }
            )+
        )?
    ;

andExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.AND, $list); }
    :   left = coalesceExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($left.result);
            }
            (
                operator = AND
                right = coalesceExpr[$_in, $yield,$await]
                { $list.add($right.result); }
            )+
        )?
    ;

coalesceExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
    :   left = bwOrExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            operator = COALESCE
            right = bwOrExpr[$_in, $yield,$await]
            { $result = opBinary(BinaryType.COALESCE, Arrays.asList($left.result, $right.result)); }
        )?
    ;

bwOrExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.BW_OR, $list); }
    :   left = bwXorExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($left.result);
            }
            (
                operator = BW_OR
                right = bwXorExpr[$_in, $yield,$await]
                { $list.add($right.result); }
            )+
        )?
    ;

bwXorExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.BW_XOR, $list); }
    :   left = bwAndExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($left.result);
            }
            (
                operator = BW_XOR
                right = bwAndExpr[$_in, $yield,$await]
                { $list.add($right.result); }
            )+
        )?
    ;

bwAndExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
locals [List<ROp> list]
@after { if ( $list != null) $result = opBinary(BinaryType.BW_AND, $list); }
    :   left = eqExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            {
                $list = new ArrayList<>();
                $list.add($left.result);
            }
            (
                operator = BW_AND
                right = eqExpr[$_in, $yield,$await]
                { $list.add($right.result); }
            )+
        )?
    ;

eqExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
    :   left = relationalExpr[$_in, $yield,$await] { $result = $left.result; }
        (
            operator = eqOperator
            right = eqExpr[$_in, $yield,$await]
            { $result = opBinary($operator.result, $left.result, $right.result); }
        )?
    ;

eqOperator returns [BinaryType result]
    :   EQUAL       { $result = BinaryType.EQ; }
    |   NOT_EQUAL   { $result = BinaryType.NEQ; }
    |   ID_EQUAL    { $result = BinaryType.EQ_ID; }
    |   ID_NOT_EQUAL{ $result = BinaryType.NEQ_ID; }
    ;

relationalExpr[boolean _in, boolean yield, boolean await] returns[@NotNull ROp result]
    :   left = shiftExpr[$yield,$await] { $result = $left.result; }
        (
            operator=relationalOperator
            right=relationalExpr[$_in, $yield,$await]
            { $result = opBinary($operator.result, $left.result, $right.result); }
        |   {$_in}?
            KW_IN
            rightExpr=expression[$_in, $yield,$await]
            { $result = opBinary(BinaryType.IN, $left.result, $rightExpr.result); }
        )?
    ;

relationalOperator returns [BinaryType result]
    :   LT             { $result = BinaryType.LT; }
    |   LE             { $result = BinaryType.LE; }
    |   {checkNoWs(2)}?
        GT ASSIGN      { $result = BinaryType.GE; }
    |   GT             { $result = BinaryType.GT; }
    |   KW_INSTANCEOF  { $result = BinaryType.INSTANCEOF; }
    ;

shiftExpr[boolean yield, boolean await] returns[@NotNull ROp result]
    :   left = additiveExpr[$yield,$await]  { $result = $left.result; }
        (
            operator = shiftOperator
            right = shiftExpr[$yield,$await]
            { $result = opBinary($operator.result, $left.result, $right.result); }
        )?
    ;

shiftOperator returns [BinaryType result]
    :   {checkNoWs(2)}? GT GT    { $result = BinaryType.SHR; }
    |   {checkNoWs(3)}? GT GT GT { $result = BinaryType.SHR_LOGICAL; }
    |   SHL                      { $result = BinaryType.SHL; }
    ;


additiveExpr[boolean yield, boolean await] returns[@NotNull ROp result]
    :   left = multExpr[$yield,$await]  { $result = $left.result; }
        (
            operator = additiveOperator
            right = additiveExpr[$yield,$await]
            { $result = opBinary($operator.result, $left.result, $right.result); }
        )?
    ;

additiveOperator returns [BinaryType result]
    :   ADD { $result = BinaryType.ADD; }
    |   SUB { $result = BinaryType.SUB; }
    ;

multExpr[boolean yield, boolean await] returns [@NotNull ROp result]
    :   left = expExpr[$yield,$await] { $result = $left.result; }
        (
            operator = multOperator
            right = multExpr[$yield,$await]
            { $result = opBinary($operator.result, $left.result, $right.result); }
        )?
    ;

multOperator returns [BinaryType result]
    :   MUL { $result = BinaryType.MUL; }
    |   DIV { $result = BinaryType.DIV; }
    |   MOD { $result = BinaryType.MOD; }
    ;

expExpr[boolean yield, boolean await] returns [@NotNull ROp result]
    :   updateExpr[$yield,$await] POW expExpr[$yield,$await]
        { $result = opBinary(BinaryType.POW, $updateExpr.result, $expExpr.result); }
    |   unaryExpr[$yield,$await]
        { $result = $unaryExpr.result; }
    ;

unaryExpr[boolean yield, boolean await] returns [@NotNull ROp result]
    :   {$await}? KWF_AWAIT unaryExpr[$yield,true]
        { $result = opBuiltin($KWF_AWAIT, $unaryExpr.stop, $KWF_AWAIT.text, $unaryExpr.result); }
    |   kwof=KW_TYPEOF
        atomExpr[$yield, $await] { $result = opBuiltin($kwof, $atomExpr.stop, $kwof.text.toLowerCase(), $atomExpr.result); }
    |   custom=(KW_DELETE|KW_VOID) unaryExpr[$yield,$await]
        { $result = opBuiltin($custom, $unaryExpr.stop, $custom.text, $unaryExpr.result); }
    |   unaryOperator unaryExpr[$yield,$await]
        { $result = opUnary($unaryOperator.start, $unaryExpr.stop, $unaryOperator.result, $unaryExpr.result); }
    |   updateExpr [$yield,$await]
        { $result = $updateExpr.result; }
    ;

unaryOperator returns [UnaryType result]
    :   NOT    { $result = UnaryType.NOT; }
    |   BW_NOT { $result = UnaryType.BW_NOT; }
    |   ADD    { $result = UnaryType.ADD; }
    |   SUB    { $result = UnaryType.SUB; }
    ;

updateExpr[boolean yield, boolean await] returns [@NotNull ROp result]
    :   atomExpr[$yield,$await] { $result = $atomExpr.result; }
        (   {checkNoLineTerm()}?
            postfixOperator
            { $result = opUnary(
                $atomExpr.start,
                $postfixOperator.stop,
                $postfixOperator.result,
                $atomExpr.result);
            }
        )?
    |   prefixOperator unaryExpr[$yield,$await]
        { $result = opUnary(
            $prefixOperator.start,
            $unaryExpr.stop,
            $prefixOperator.result,
            $unaryExpr.result);
        }
    ;

postfixOperator returns [UnaryType result]
    :   INC { $result = UnaryType.INC_POSTFIX; }
    |   DEC { $result = UnaryType.DEC_POSTFIX; }
    ;

prefixOperator returns [UnaryType result]
    :   INC    { $result = UnaryType.INC; }
    |   DEC    { $result = UnaryType.DEC; }
    ;

atomExpr[boolean yield, boolean await] returns [@NotNull ROp result]
locals [List<ROp> list, boolean doChain]
@init { $list = new ArrayList<>(); }
@after { if ( $doChain ) $result = opChain($list); }
    :   KW_NEW atomExpr[$yield,$await]  { $list.add($atomExpr.result); }
        (arguments[$yield,$await] { $list.add($arguments.result); })?
        { $result = opNode(evalStatement($KW_NEW, $atomExpr.stop), RawOpType.RAW_NEW, $list); }
    |   (   atomExprFirst[$yield,$await] { $result = $atomExprFirst.result; $list.add($atomExprFirst.result); }
        |   KW_NEW DOT TARGET { $doChain = true; $list.add(opUnresolvedId($KW_NEW)); $list.add(opUnresolvedId($TARGET)); }
        )
        (atomExprRest[$yield,$await] { $list.addAll($atomExprRest.result); $doChain = true; })*
    ;

atomExprFirst[boolean yield, boolean await] returns [@NotNull ROp result]
    :   classExpr[$yield,$await]                           { $result = $classExpr.result; }
    |   LPAREN expressionList[true,$yield,$await] RPAREN   { $result = opParen($LPAREN, $RPAREN, $expressionList.result); }
    |   KW_THIS                                            { $result = opUnresolvedId($KW_THIS); }
    |   KWF_SUPER                                          { $result = opUnresolvedId($KWF_SUPER); }
    |   scalarLiteral                                      { $result = $scalarLiteral.result; }
    |   arrayLiteral[$yield,$await]                        { $result = $arrayLiteral.result; }
    |   objectLiteral[$yield,$await]                       { $result = $objectLiteral.result; }
    |   templateLiteral[$yield,$await,false]               { $result = $templateLiteral.result; }
    |   functionExpr[$yield,$await]                        { $result = $functionExpr.result; }
    |   {jsxAllowed()}? jsxElement[$yield,$await]          { $result = $jsxElement.result; }
    |   identifier[$yield,$await]                          { $result = $identifier.result; }
    ;

atomExprRest[boolean yield, boolean await] returns [List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   (QUESTION { $result.add(opNode(JsOpType.NULL_SAFE,$QUESTION)); })?
        DOT HASH? identifierName[false,false]            { $result.add($identifierName.result); }
    |   LBRACK expressionList[true,$yield,$await] RBRACK { $result.add(opArrayAccessor($LBRACK, $RBRACK, $expressionList.result)); }
    |   arguments[$yield,$await]                         { $result.add($arguments.result); }
    |   templateLiteral[$yield,$await,true]              { $result.add($templateLiteral.result); }
    ;

assignable[boolean yield, boolean await] returns [ROp result]
    :   identifierName[$yield,$await]          { $result = $identifierName.result; }
    |   arrayBinding[$yield,$await]    { $result = $arrayBinding.result; }
    |   objectBinding[$yield,$await]   { $result = $objectBinding.result; }
    ;

arrayBinding[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
    :   LBRACK
        (   arrayBindingList[$yield,$await] { $list.addAll($arrayBindingList.result); }
            (
                COMMA
                (COMMA { $list.add(opNone($COMMA)); })*
                (arrayBindingRest[$yield,$await] { $list.add($arrayBindingRest.result); })?
            )?
        |   (COMMA { $list.add(opNone($COMMA)); })*
            (arrayBindingRest[$yield,$await] { $list.add($arrayBindingRest.result); })?
        )
        RBRACK
        { $result = opNode(evalStatement($LBRACK, $RBRACK), RawOpType.RAW_ARRAY_BINDING, $list); }
    ;


arrayBindingList[boolean yield, boolean await] returns [ @NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   (COMMA { $result.add(opNone($COMMA)); })*
        arrayBindingElement[$yield,$await] { $result.add($arrayBindingElement.result); }
        (   COMMA
            (COMMA { $result.add(opNone($COMMA)); })*
            arrayBindingElement[$yield,$await]
            { $result.add($arrayBindingElement.result); }
        )*
    ;

arrayBindingElement[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_ARRAY_BINDING_ITEM, $list); }
    :   assignable[$yield, $await]    { $list.add($assignable.result); }
        (
            ASSIGN expression[true,$yield,$await]
            { $list.add(opNode(StdOpType.INIT, $expression.result)); }
        )?
    ;

arrayBindingRest[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [ ROp item ]
@after { $result = opNode(RawOpType.RAW_ARRAY_BINDING_ITEM, opUnary($start, $stop, UnaryType.SPREAD, $item)); }   // TODO: move spread to modifiers
    :   ELLIPSIS assignable[$yield,$await] { $item = $assignable.result; }
    ;


objectBinding[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [List<ROp> list]
@init { $list = new ArrayList<>(); }
    :   LBRACE
        (   objectBindingList[$yield,$await] { $list.addAll($objectBindingList.result); }
            COMMA?
            (COMMA+ objectBindingRest[$yield,$await] { $list.add($objectBindingRest.result); } )?
        |   COMMA* objectBindingRest[$yield,$await] { $list.add($objectBindingRest.result); }
        )?
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), RawOpType.RAW_OBJECT_BINDING, $list); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            $result = opFoldBlock(token, "startObjectBinding");
        }
    ;

objectBindingRest[boolean yield, boolean await] returns [ @NotNull ROp result]
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_OBJECT_BINDING_ITEM, $result); }
    :   ELLIPSIS assignable[$yield,$await]
        { $result = opUnary($ELLIPSIS, $assignable.stop, UnaryType.SPREAD, $assignable.result); }
        // TODO: to modifiers
    ;

objectBindingList[boolean yield, boolean await] returns [ @NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   objectBindingItem[$yield,$await] { $result.add($objectBindingItem.result); }
        (COMMA objectBindingItem[$yield,$await] { $result.add($objectBindingItem.result); } )*
    ;

objectBindingItem[boolean yield, boolean await] returns [ @NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_OBJECT_BINDING_ITEM, $list); }
    :   propertyName[$yield,$await] { $list.add($propertyName.result); }
        (
            COLON
            assignable[$yield,$await] { $list.add(opNode(JsOpType.DEEP, $assignable.result)); }
        )?
        (
            ASSIGN expression[true, $yield,$await]
            { $list.add($expression.result); }
        )?
    ;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Literals
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
scalarLiteral returns [@NotNull ROp result]
    :   NullLiteral                            { $result = nullLiteral($NullLiteral); }
    |   UNDEFINED                              { $result = undefinedLiteral($UNDEFINED); }
    |   BooleanLiteral                         { $result = boolLiteral($BooleanLiteral); }
    |   StringLiteral                          { $result = stringLiteral($StringLiteral); }
    |   numLiteral                             { $result = $numLiteral.result; }
    |   bigintLiteral                          { $result = $bigintLiteral.result; }
    |   RegexpLiteral                          { $result = regExpLiteral($RegexpLiteral); }
    ;


numLiteral returns [@NotNull ROp result]
locals [ boolean negate ]
    :   (SUB { $negate = true; })?
        (   BinIntLiteral                          { $result = numLiteral($negate, $BinIntLiteral, 2); }
        |   OctIntLiteral                          { $result = numLiteral($negate, $OctIntLiteral, 8); }
        |   HexIntLiteral                          { $result = numLiteral($negate, $HexIntLiteral, 16); }
        |   DecimalLiteral                         { $result = floatLiteral($negate, $DecimalLiteral); }
        )
    ;

bigintLiteral returns [@NotNull ROp result]
    : BigDecIntLiteral                             { $result = bigNumLiteral(false, $BigDecIntLiteral, 10); }
    | BigHexIntLiteral                             { $result = bigNumLiteral(false, $BigHexIntLiteral, 16); }
    | BigOctIntLiteral                             { $result = bigNumLiteral(false, $BigOctIntLiteral, 8); }
    | BigBinIntLiteral                             { $result = bigNumLiteral(false, $BigBinIntLiteral, 2); }
    ;

templateLiteral[boolean yield, boolean await, boolean tagged] returns [@NotNull ROp result]
    :   TemplateLiteral
        { $result = _templateLiteral($TemplateLiteral, $yield, $await, $tagged); }
    ;

/////////// Object Literal
objectLiteral[boolean yield, boolean await] returns [@NotNull ROp result]
locals [List<ROp> list ]
@init { $list = new ArrayList<>(); }
    :   LBRACE
        (
            objectProperty[$yield, $await] { $list.add($objectProperty.result); }
            (COMMA objectProperty[$yield, $await] { $list.add($objectProperty.result); })*
        )?
        COMMA?
        RBRACE
        { $result = opNode(evalStatement($LBRACE, $RBRACE), RawOpType.RAW_OBJECT, $list); }
    |   FoldBlock
        {
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            $result = opFoldBlock(token, "startObjectLiteral");
        }
    ;

objectProperty[boolean yield, boolean await] returns [@NotNull ROp result]
    :   propertyAssign[$yield,$await]               { $result = $propertyAssign.result; }
    |   objectGetter[$yield,$await]                 { $result = $objectGetter.result; }
    |   objectSetter[$yield,$await]                 { $result = $objectSetter.result; }
    |   objectMethod[$yield,$await]                 { $result = $objectMethod.result; }
    |   ELLIPSIS expression[false,$yield,$await]    { $result = opUnary($ELLIPSIS, $expression.stop, UnaryType.SPREAD, $expression.result); }
    |   expression[false,$yield,$await]             { $result = $expression.result; }
    ;

propertyAssign[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start, $stop), RawOpType.RAW_DECL_PROPERTY, $list); }
    :   propertyName[$yield,$await]       { $list.add($propertyName.result); }
        COLON
        expression[true,$yield,$await]   { $list.add($expression.result); }
    ;

objectGetter[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    $list.add(opModifiers(st, Collections.singletonList(JSModifiers.Getter())));
    $result = opNode(st, RawOpType.RAW_DECL_METHOD, $list);
}
    :   GETTER propertyName[$yield,$await] { $list.add($propertyName.result); }
        LPAREN RPAREN
        functionBody[false,false]          { $list.add($functionBody.result); }
    ;

objectSetter[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    $list.add(opModifiers(st, Collections.singletonList(JSModifiers.Setter())));
    $result = opNode(st, RawOpType.RAW_DECL_METHOD, $list);
}
    :   SETTER propertyName[$yield,$await]              { $list.add($propertyName.result); }
        LPAREN parameter[$yield,$await,false] RPAREN    { $list.add($parameter.result); }
        functionBody[false,false]                       { $list.add($functionBody.result); }
    ;

objectMethod[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ boolean async, boolean generator, List<RModifier> modifiers, List<ROp> list ]
@init { $modifiers = new ArrayList<>(); $list = new ArrayList<>(); }
@after {
    Statement st = evalStatement($start, $stop);
    if ( !$modifiers.isEmpty() ) $list.add(opModifiers(st, $modifiers));
    $result = opNode(st, RawOpType.RAW_DECL_METHOD, $list);
}
    :   (KWF_ASYNC { $async = true; $modifiers.add(JSModifiers.Async()); })?
        (MUL { $generator = true; $modifiers.add(JSModifiers.Generator()); })?
        propertyName[$yield,$await]       { $list.add($propertyName.result); }
        LPAREN
            (parameterList[$yield,$await] { $list.addAll($parameterList.result); })?
        RPAREN
        functionBody[$generator,$async]   { $list.add($functionBody.result); }
    ;

/////////// Array Literal
arrayLiteral[boolean yield, boolean await] returns [@NotNull ROp result]
locals [List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = opNode(evalStatement($start,$stop), RawOpType.RAW_ARRAY, $list); }
    :   LBRACK
        (   (COMMA { $list.add(opNone($COMMA)); })*
            (   arrayItem[$yield,$await] { $list.add($arrayItem.result); }
                (COMMA (COMMA { $list.add(opNone($COMMA)); } )* arrayItem[$yield,$await] { $list.add($arrayItem.result); })*
            )?
            (COMMA { $list.add(opNone($COMMA)); })*
        )
        RBRACK
    ;

arrayItem[boolean yield, boolean await] returns [@NotNull ROp result]
@after { $result = opNode($result.statement(), RawOpType.RAW_ARRAY_ITEM, $result); }
    :   ELLIPSIS expression[false,$yield,$await]  { $result = opUnary($ELLIPSIS, $expression.stop, UnaryType.SPREAD, $expression.result); }
    |   expression[false,$yield,$await]           { $result = $expression.result; }
    ;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Identifiers
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
propertyName[boolean yield, boolean await] returns [@NotNull ROp result]
@after { $result = opName($result); }
    :   identifierName[false,false]          { $result = $identifierName.result; }
    |   numLiteral                             { $result = $numLiteral.result; }
    |   StringLiteral                          { $result = stringLiteral($StringLiteral); }
    |   computedPropertyName[$yield, $await]   { $result = $computedPropertyName.result; }
    ;

computedPropertyName[boolean yield, boolean await] returns [@NotNull ROp result]
    :   LBRACK expression[true,$yield,$await] RBRACK
        { $result = $expression.result; }
    ;

qualifiedIdentifierName[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ List<ROp> list ]
@init { $list = new ArrayList<>(); }
@after { $result = $list.size() == 1 ? $list.get(0) : opChain($list); }
    :   identifierName[$yield,$await]      { $list.add($identifierName.result); }
        (DOT identifierName[$yield,$await] { $list.add($identifierName.result); } )*
    ;

identifier[boolean yield, boolean await] returns [@NotNull ROp result]
    :   Identifier           { $result = opUnresolvedId($Identifier); }
    |   {!$await}? KWF_AWAIT { $result = opUnresolvedId($KWF_AWAIT); }
    |   {!$yield}? KWS_YIELD { $result = opUnresolvedId($KWS_YIELD); }
    |   nonReserved[$yield]  { $result = opUnresolvedId($nonReserved.start); }
    ;

identifierName[boolean yield, boolean await] returns [@NotNull ROp result]
    :   identifier[$yield,$await]             { $result = $identifier.result; }
    |   reservedWord[$yield,$await]    { $result = $reservedWord.result; }
    ;

reservedWord[boolean yield, boolean await] returns [@NotNull ROp result]
    :   keywords[$yield,$await] { $result = opUnresolvedId($keywords.start); }
    |   NullLiteral             { $result = opUnresolvedId($NullLiteral); }
    |   BooleanLiteral          { $result = opUnresolvedId($BooleanLiteral); }
    ;

nonReserved[boolean yield]
    :   UNDEFINED
    |   OF
    |   TARGET
    |   MODULE
    |   UNIQUE
    |   GETTER
    |   SETTER
    |   KW_AS
    |   KW_FROM
    |   KWF_ASYNC
    |   {!isStrictMode()}?
        (   KWS_IMPLEMENTS
        |   KWS_LET
        |   KWS_PRIVATE
        |   KWS_PUBLIC
        |   KWS_INTERFACE
        |   KWS_PACKAGE
        |   KWS_PROTECTED
        |   KWS_STATIC
        )
    ;

keywords[boolean yield, boolean await]
    :   KW_BREAK
    |   KW_DO
    |   KW_INSTANCEOF
    |   KW_TYPEOF
    |   KW_CASE
    |   KW_ELSE
    |   KW_NEW
    |   KW_VAR
    |   KW_CATCH
    |   KW_FINALLY
    |   KW_RETURN
    |   KW_VOID
    |   KW_CONTINUE
    |   KW_FOR
    |   KW_SWITCH
    |   KW_WHILE
    |   KW_DEBUGGER
    |   KW_FUNCTION
    |   KW_THIS
    |   KW_WITH
    |   KW_DEFAULT
    |   KW_IF
    |   KW_THROW
    |   KW_DELETE
    |   KW_IN
    |   KW_TRY
    |   KW_AS
    |   KW_FROM

    |   KWF_CLASS
    |   KWF_ENUM
    |   KWF_EXTENDS
    |   KWF_SUPER
    |   KWF_CONST
    |   KWF_EXPORT
    |   KWF_IMPORT
    |   KWF_AWAIT
//    |   KWF_ASYNC

    |   KWS_IMPLEMENTS
    |   KWS_LET
    |   KWS_PRIVATE
    |   KWS_PUBLIC
    |   KWS_INTERFACE
    |   KWS_PACKAGE
    |   KWS_PROTECTED
    |   KWS_STATIC
    |   KWS_YIELD
    ;

endOfStmt
    :   SEMI
    |   EOF
    |   {checkLineTerm()}?
    |   {checkRBrace()}?
    ;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// JSX
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
jsxElement[boolean yield, boolean await] returns [@NotNull ROp result]
locals[ List<ROp> list ]
@init { $list = new ArrayList<>(); }
    :   startTag=LT (jsxAttributes[$yield,$await] { $list.addAll($jsxAttributes.result); })? GT
            jsxElementBody[$list,$yield,$await]
        LT DIV endTag=GT
        { $result = opJsx($startTag, $endTag, "", $list); }
    |   startTag=LT
            tagName=jsxTagName
            (jsxAttributes[$yield,$await] { $list.addAll($jsxAttributes.result); })?
        GT
            jsxElementBody[$list,$yield,$await]
        LT DIV endTagName=jsxTagName {$tagName.text.equals($endTagName.text)}? endTag=GT
        { $result = opJsx($startTag, $endTag, $tagName.text, $list); } // TODO: use tagName.result instead
    |   startTag=LT
            tagName=jsxTagName
            (jsxAttributes[$yield,$await] { $list.addAll($jsxAttributes.result); })?
        DIV endTag=GT
        { $result = opJsx($startTag, $endTag, $tagName.text, $list); }  // TODO: use tagName.result instead
    ;

jsxTagName returns [List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   identifierName[false,false]        { $result.add($identifierName.result); }
        (DOT identifierName[false,false])* { $result.add($identifierName.result); }
    ;

jsxElementBody[List<ROp> list, boolean yield, boolean await]
    :   (   jsxElement[$yield,$await]
            { $list.add($jsxElement.result); }
        |   FoldBlock
            {
                RScanToken token = (RScanToken)$FoldBlock;
                token.option("yield", String.valueOf($yield));
                token.option("await", String.valueOf($await));
                $list.add(opFoldBlock(token, "startExpressionListBlock"));
            }
        |   LBRACE expressionList[true,$yield,$await] RBRACE
            { $list.add($expressionList.result); }
        |   . // just ignore this tokens
        )*?
    ;

jsxAttributes[boolean yield, boolean await] returns [@NotNull List<ROp> result]
@init { $result = new ArrayList<>(); }
    :   (jsxAttribute[$yield,$await] { $result.add($jsxAttribute.result); })+
    ;

jsxAttribute[boolean yield, boolean await] returns [@NotNull ROp result]
locals [ List<ROp> list, Token _stop, String attrName ]
@init { $list = new ArrayList<>(); }
@after { $result = opJsxAttr($start, $stop, $attrName, $list); }
    :   jsxAttributeName[$yield,$await] { $attrName = $jsxAttributeName.text; }
        (   ASSIGN
            (   FoldBlock
                {
                    $_stop = $FoldBlock;
                    RScanToken token = (RScanToken)$FoldBlock;
                    token.option("yield", String.valueOf($yield));
                    token.option("await", String.valueOf($await));
                    $list.add(opFoldBlock(token, "startExpressionListBlock"));
                }
            |   LBRACE expressionList[true,$yield,$await] RBRACE
                { $list.add($expressionList.result); }
            |   scalarLiteral
                { $list.add($scalarLiteral.result); }
            )
        )?
    |   FoldBlock { $attrName = "#__default__#"; }
        {
            $_stop = $FoldBlock;
            RScanToken token = (RScanToken)$FoldBlock;
            token.option("yield", String.valueOf($yield));
            token.option("await", String.valueOf($await));
            $list.add(opFoldBlock(token, "startExpressionListBlock"));
        }
    |   LBRACE expressionList[true,$yield,$await] RBRACE
        { $attrName = "#__default__#"; }
        { $list.add($expressionList.result); }
    ;

jsxAttributeName[boolean yield, boolean await]
    :   identifierName[false,false] (SUB identifierName[false,false])*
    ;
