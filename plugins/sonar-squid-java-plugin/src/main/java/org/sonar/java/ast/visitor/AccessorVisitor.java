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

import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

import antlr.collections.AST;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class AccessorVisitor extends JavaAstVisitor {

  private static final List<Integer> TOKENS = Arrays.asList(TokenTypes.METHOD_DEF);

  @Override
  public List<Integer> getWantedTokens() {
    return TOKENS;
  }

  @Override
  public void visitToken(DetailAST methodAst) {
    SourceCode currentMethod = peekSourceCode();
    AstUtils.ensureResourceType(currentMethod, SourceMethod.class);
    Scope methodScope = AstUtils.getScope(methodAst);
    if (AstUtils.isScope(methodScope, Scope.PUBLIC) && isAccessor(methodAst, currentMethod.getName())) {
      currentMethod.setMeasure(Metric.ACCESSORS, 1);
    }
  }

  @Override
  public void leaveToken(DetailAST ast) {
    SourceMethod currentMethod = (SourceMethod) peekSourceCode();
    if (currentMethod.isAccessor()) {
      currentMethod.setMeasure(Metric.PUBLIC_API, 0);
      currentMethod.setMeasure(Metric.PUBLIC_DOC_API, 0);
      currentMethod.setMeasure(Metric.METHODS, 0);
      currentMethod.setMeasure(Metric.COMPLEXITY, 0);
    }
  }

  private boolean isAccessor(DetailAST methodAst, String methodName) {
    boolean methodWithoutParams = isMethodWithoutParameters(methodAst);
    return isValidGetter(methodAst, methodName, methodWithoutParams) || isValidSetter(methodAst, methodName, methodWithoutParams)
        || isValidBooleanGetter(methodAst, methodName, methodWithoutParams);
  }

  private boolean isMethodWithoutParameters(DetailAST methodAst) {
    return methodAst.findFirstToken(TokenTypes.PARAMETERS).getChildCount() == 0;
  }

  private boolean isValidBooleanGetter(DetailAST method, String methodName, boolean methodWithoutParams) {
    if (methodName.startsWith("is") && methodWithoutParams) {
      AST type = AstUtils.findType(method);
      if (isAstType(type, TokenTypes.LITERAL_BOOLEAN)) {
        return isValidGetter(method, methodName.replace("is", "get"), methodWithoutParams);
      }
    }
    return false;
  }

  private boolean isValidSetter(DetailAST method, String methodName, boolean methodWithoutParams) {
    if (methodName.startsWith("set") && !methodWithoutParams) {
      AST type = AstUtils.findType(method);
      if (isVoidMethodReturn(type)) {
        DetailAST methodParams = method.findFirstToken(TokenTypes.PARAMETERS);
        if (methodParams.getChildCount(TokenTypes.PARAMETER_DEF) == 1) {
          DetailAST methodBody = method.getLastChild();
          if (isAstType(methodBody, TokenTypes.SLIST) && methodBody.getChildCount() == 3) {
            return inspectSetterMethodBody(method, methodParams, methodBody);
          }
        }
      }
    }
    return false;
  }

  private boolean isValidGetter(DetailAST method, String methodName, boolean methodWithoutParams) {
    if (methodName.startsWith("get") && methodWithoutParams) {
      AST type = AstUtils.findType(method);
      if (!isVoidMethodReturn(type)) {
        DetailAST methodBody = method.getLastChild();
        if (isAstType(methodBody, TokenTypes.SLIST) && methodBody.getChildCount() == 2) {
          return inspectGetterMethodBody(method, methodBody);
        }
      }
    }
    return false;
  }

  private boolean inspectGetterMethodBody(DetailAST method, DetailAST methodBody) {
    AST returnAST = methodBody.getFirstChild();
    if (isAstType(returnAST, TokenTypes.LITERAL_RETURN)) {
      AST expr = returnAST.getFirstChild();
      if (isAstType(expr, TokenTypes.EXPR) && isAstType(expr.getNextSibling(), TokenTypes.SEMI)) {
        AST varReturned = expr.getFirstChild();
        if (isAstType(varReturned, TokenTypes.IDENT)) {
          return findPrivateClassVariable(method, varReturned.getText());
        }
      }
    }
    return false;
  }

  private boolean inspectSetterMethodBody(DetailAST method, DetailAST methodParams, DetailAST methodBody) {
    DetailAST expr = (DetailAST) methodBody.getFirstChild();
    if (isAstType(expr, TokenTypes.EXPR)) {
      DetailAST assignment = expr.findFirstToken(TokenTypes.ASSIGN);
      if (assignment != null) {
        DetailAST dotAst = assignment.findFirstToken(TokenTypes.DOT);
        DetailAST varAssigned = assignment.getLastChild();
        DetailAST varAssignedMethodParam = methodParams.findFirstToken(TokenTypes.PARAMETER_DEF).findFirstToken(TokenTypes.IDENT);
        // check that the var assigned is the var from the method param
        if (isAstType(varAssigned, TokenTypes.IDENT) && varAssigned.getText().equals(varAssignedMethodParam.getText())) {
          DetailAST varToAssign = dotAst != null ? dotAst.findFirstToken(TokenTypes.IDENT) : assignment.findFirstToken(TokenTypes.IDENT);
          return findPrivateClassVariable(method, varToAssign.getText());
        }
      }
    }
    return false;
  }

  private boolean findPrivateClassVariable(DetailAST method, String varName) {
    DetailAST objBlock = AstUtils.findParent(method, TokenTypes.OBJBLOCK);
    for (AST i = objBlock.getFirstChild(); i != null; i = i.getNextSibling()) {
      if (isAstType(i, TokenTypes.VARIABLE_DEF)) {
        DetailAST varDef = (DetailAST) i;
        if (AstUtils.isScope(AstUtils.getScope(varDef), Scope.PRIVATE) 
            && varDef.findFirstToken(TokenTypes.IDENT).getText().equals(varName)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isVoidMethodReturn(AST type) {
    return isAstType(type, TokenTypes.LITERAL_VOID);
  }

  private boolean isAstType(AST ast, int type) {
    return ast.getType() == type;
  }

}
