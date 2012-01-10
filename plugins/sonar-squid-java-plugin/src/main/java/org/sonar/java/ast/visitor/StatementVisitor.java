/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.visitor;

import java.util.Arrays;
import java.util.List;

import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class StatementVisitor extends JavaAstVisitor {

  private static final List<Integer> WANTED_TOKENS = Arrays.asList(TokenTypes.VARIABLE_DEF, TokenTypes.CTOR_CALL, TokenTypes.LITERAL_IF,
                                                      TokenTypes.LITERAL_ELSE, TokenTypes.LITERAL_WHILE, TokenTypes.LITERAL_DO,
                                                      TokenTypes.LITERAL_FOR, TokenTypes.LITERAL_SWITCH, TokenTypes.LITERAL_BREAK,
                                                      TokenTypes.LITERAL_CONTINUE, TokenTypes.LITERAL_RETURN, TokenTypes.LITERAL_THROW,
                                                      TokenTypes.LITERAL_SYNCHRONIZED, TokenTypes.LITERAL_CATCH,
                                                      TokenTypes.LITERAL_FINALLY, TokenTypes.EXPR, TokenTypes.LABELED_STAT,
                                                      TokenTypes.LITERAL_CASE, TokenTypes.LITERAL_DEFAULT, TokenTypes.LITERAL_ASSERT);

  @Override
  public List<Integer> getWantedTokens() {
    return WANTED_TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (isCountable(ast)) {
      peekSourceCode().add(Metric.STATEMENTS, 1);
    }
  }

  /**
   * Checks if a token is countable for the ncss metric
   * 
   * @param ast
   *          the AST
   * @return true if the token is countable
   */
  private boolean isCountable(DetailAST ast) {
    boolean countable = true;
    final int tokenType = ast.getType();
    // check if an expression is countable
    if (TokenTypes.EXPR == tokenType) {
      countable = isExpressionCountable(ast);
    }
    // check if an variable definition is countable, only for non class
    // variables
    else if (TokenTypes.VARIABLE_DEF == tokenType) {
      countable = isVariableDefCountable(ast);
    }
    return countable;
  }

  /**
   * Checks if a variable definition is countable.
   * 
   * @param aAST
   *          the AST
   * @return true if the variable definition is countable, false otherwise
   */
  private boolean isVariableDefCountable(DetailAST aAST) {
    boolean countable = false;
    if (!AstUtils.isClassVariable(aAST) && !AstUtils.isInterfaceVariable(aAST)) {
      // count variable defs only if they are direct child to a slist or
      // object block
      final int parentType = aAST.getParent().getType();
      if ((TokenTypes.SLIST == parentType) || (TokenTypes.OBJBLOCK == parentType)) {
        final DetailAST prevSibling = aAST.getPreviousSibling();
        // is countable if no previous sibling is found or
        // the sibling is no COMMA.
        // This is done because multiple assignment on one line are
        // countes as 1
        countable = (prevSibling == null) || (TokenTypes.COMMA != prevSibling.getType());
      }
    }
    return countable;
  }

  /**
   * Checks if an expression is countable for the ncss metric.
   * 
   * @param aAST the AST
   * @return true if the expression is countable, false otherwise
   */
  private boolean isExpressionCountable(DetailAST aAST) {
    boolean countable;
    // count expressions only if they are direct child to a slist (method
    // body, for loop...)
    // or direct child of label,if,else,do,while,for
    final int parentType = aAST.getParent().getType();
    switch (parentType) {
      case TokenTypes.SLIST:
      case TokenTypes.LABELED_STAT:
      case TokenTypes.LITERAL_FOR:
      case TokenTypes.LITERAL_DO:
      case TokenTypes.LITERAL_WHILE:
      case TokenTypes.LITERAL_IF:
      case TokenTypes.LITERAL_ELSE:
        // don't count if or loop conditions
        final DetailAST prevSibling = aAST.getPreviousSibling();
        countable = (prevSibling == null) || (TokenTypes.LPAREN != prevSibling.getType());
        break;
      default:
        countable = false;
        break;
    }
    return countable;
  }
}
