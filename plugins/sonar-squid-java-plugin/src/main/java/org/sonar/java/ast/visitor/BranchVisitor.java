/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

public class BranchVisitor extends JavaAstVisitor {

  private static final List<Integer> TOKENS = Arrays.asList(TokenTypes.LITERAL_WHILE, TokenTypes.LITERAL_DO, TokenTypes.LITERAL_FOR,
                                                      TokenTypes.LITERAL_IF, TokenTypes.LITERAL_CASE, TokenTypes.LITERAL_CATCH,
                                                      TokenTypes.QUESTION, TokenTypes.LAND, TokenTypes.LOR, TokenTypes.LITERAL_THROW,
                                                      TokenTypes.LITERAL_RETURN);

  @Override
  public List<Integer> getWantedTokens() {
    return TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (isLastMethodReturnStatement(ast)) {
      return;
    }
    peekSourceCode().add(Metric.BRANCHES, 1);
  }

  private boolean isLastMethodReturnStatement(DetailAST ast) {
    if (ast.getType() == TokenTypes.LITERAL_RETURN) {
      DetailAST parent = ast.getParent();
      if (parent.getType() == TokenTypes.SLIST) {
        DetailAST method = AstUtils.findParent(ast, TokenTypes.METHOD_DEF);
        if (method != null && method.findFirstToken(TokenTypes.SLIST) == parent) {
          return true;
        }
      }
    }
    return false;
  }
}
