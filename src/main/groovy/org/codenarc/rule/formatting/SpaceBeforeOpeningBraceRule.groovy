/*
 * Copyright 2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.formatting

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.codenarc.util.AstUtil
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.ConstructorNode

/**
 * Check that there is at least one space (blank) or whitespace before each opening brace ("{").
 * This checks method/class/interface declarations, as well as try/for/while/if statements.
 *
 * @author Chris Mair
 */
class SpaceBeforeOpeningBraceRule extends AbstractAstVisitorRule {

    String name = 'SpaceBeforeOpeningBrace'
    int priority = 3
    Class astVisitorClass = SpaceBeforeOpeningBraceAstVisitor
    boolean checkClosureMapEntryValue = true
}

class SpaceBeforeOpeningBraceAstVisitor extends AbstractAstVisitor {

    @Override
    protected void visitClassEx(ClassNode node) {
        def line = sourceLineOrEmpty(node)
        def indexOfBrace = line.indexOf('{')
        if (indexOfBrace > 1) {
            if (!Character.isWhitespace(line[indexOfBrace - 1] as char)) {
                def typeName = node.isInterface() ? 'interface' : (node.isEnum() ? 'enum' : 'class')
                addViolation(node, "The opening brace for $typeName $currentClassName is not preceded by a space or whitespace")
            }
        }
        super.visitClassEx(node)
    }

    @Override
    void visitConstructor(ConstructorNode node) {
        // The AST for constructors is "special". The BlockStatement begins on the line following the
        // opening brace if it is a multi-line method. Bug?

        isFirstVisit(node.code)   // Register the code block so that it will be ignored in visitBlockStatement()
        def line = sourceLineOrEmpty(node)
        if (line.contains('){')) {
            addOpeningBraceViolation(node, 'block')
        }
        super.visitConstructor(node)
    }

    @Override
    void visitBlockStatement(BlockStatement block) {
        if (isFirstVisit(block) && !AstUtil.isFromGeneratedSourceCode(block)) {
            def line = sourceLineOrEmpty(block)
            def startCol = block.columnNumber
            if (startCol > 1 && !Character.isWhitespace(line[startCol - 2] as char)) {
                addOpeningBraceViolation(block, 'block')
            }
        }
        super.visitBlockStatement(block)
    }

    @Override
    void visitClosureExpression(ClosureExpression expression) {
        isFirstVisit(expression.code)   // Register the code block so that it will be ignored in visitBlockStatement()
        if (isFirstVisit(expression)) {
            def line = sourceLineOrEmpty(expression)
            def startCol = expression.columnNumber
            if (startCol > 1 && !Character.isWhitespace(line[startCol - 2] as char)) {
                addOpeningBraceViolation(expression, 'closure')
            }
        }
        super.visitClosureExpression(expression)
    }

    @Override
    void visitMapEntryExpression(MapEntryExpression expression) {
        if (!rule.checkClosureMapEntryValue && expression.valueExpression instanceof ClosureExpression) {
            isFirstVisit(expression.valueExpression)   // Register the closure so that it will be ignored in visitClosureExpression()
        }
        super.visitMapEntryExpression(expression)
    }

    private String sourceLineOrEmpty(node) {
        node.lineNumber == -1 ? '' : sourceLine(node)
    }

    private void addOpeningBraceViolation(ASTNode node, String keyword) {
        addViolation(node, "The opening brace for the $keyword in class $currentClassName is not preceded by a space or whitespace")
    }

}