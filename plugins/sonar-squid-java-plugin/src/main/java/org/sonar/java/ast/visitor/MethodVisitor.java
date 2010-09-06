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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonar.java.signature.JvmJavaType;
import org.sonar.java.signature.MethodSignature;
import org.sonar.java.signature.MethodSignaturePrinter;
import org.sonar.java.signature.Parameter;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

import antlr.collections.AST;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class MethodVisitor extends JavaAstVisitor {

  private static final String CONSTRUCTOR = "<init>";

  private static final List<Integer> wantedTokens = Arrays.asList(TokenTypes.CTOR_DEF, TokenTypes.METHOD_DEF);
  private static final Map<Integer, JvmJavaType> tokenJavaTypeMapping = new HashMap<Integer, JvmJavaType>();

  static {
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_BYTE, JvmJavaType.B);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_CHAR, JvmJavaType.C);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_SHORT, JvmJavaType.S);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_INT, JvmJavaType.I);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_LONG, JvmJavaType.J);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_BOOLEAN, JvmJavaType.Z);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_FLOAT, JvmJavaType.F);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_DOUBLE, JvmJavaType.D);
    tokenJavaTypeMapping.put(TokenTypes.LITERAL_VOID, JvmJavaType.V);
  }

  @Override
  public List<Integer> getWantedTokens() {
    return wantedTokens;
  }

  @Override
  public void visitToken(DetailAST ast) {
    boolean isConstructor = isConstructor(ast);
    String methodName = buildMethodSignature(ast, isConstructor);
    SourceMethod sourceMethod = new SourceMethod(peekParentClass(), methodName, ast.getLineNo());
    sourceMethod.setMeasure(Metric.METHODS, 1);
    if (isConstructor) {
      sourceMethod.setMeasure(Metric.CONSTRUCTORS, 1);
    }
    addSourceCode(sourceMethod);
  }

  private boolean isConstructor(DetailAST ast) {
    return ast.getType() == TokenTypes.CTOR_DEF;
  }

  @Override
  public void leaveToken(DetailAST ast) {
    popSourceCode();
  }

  private String buildMethodSignature(DetailAST ast, boolean isConstructor) {
    String methodName = extractMethodName(ast, isConstructor);
    Parameter returnType = extractMethodReturnType(ast, isConstructor);
    List<Parameter> argumentTypes = extractMethodArgumentTypes(ast);
    MethodSignature signature = new MethodSignature(methodName, returnType, argumentTypes);
    return MethodSignaturePrinter.print(signature);
  }

  private List<Parameter> extractMethodArgumentTypes(DetailAST ast) {
    List<Parameter> argumentTypes = new ArrayList<Parameter>();
    DetailAST child = ast.findFirstToken(TokenTypes.PARAMETERS).findFirstToken(TokenTypes.PARAMETER_DEF);
    while (child != null) {
      if (child.getType() == TokenTypes.PARAMETER_DEF) {
        Parameter argumentType = extractArgumentAndReturnType(child.findFirstToken(TokenTypes.TYPE));
        argumentTypes.add(new Parameter(argumentType));
      }
      child = (DetailAST) child.getNextSibling();
    }
    return argumentTypes;
  }

  private String extractMethodName(DetailAST ast, boolean isConstructor) {
    if (isConstructor) {
      return CONSTRUCTOR;
    }
    return ast.findFirstToken(TokenTypes.IDENT).getText();
  }

  private Parameter extractMethodReturnType(DetailAST ast, boolean isConstructor) {
    if (isConstructor) {
      return new Parameter(JvmJavaType.V, false);
    }
    Parameter returnType = extractArgumentAndReturnType(ast.findFirstToken(TokenTypes.TYPE));
    return new Parameter(returnType);
  }

  private Parameter extractArgumentAndReturnType(DetailAST ast) {
    boolean isArray = isArrayType(ast);
    for (Integer tokenType : tokenJavaTypeMapping.keySet()) {
      if (ast.branchContains(tokenType)) {
        return new Parameter(tokenJavaTypeMapping.get(tokenType), isArray);
      }
    }
    if (isObjectType(ast)) {
      String className;
      if (isArray) {
        className = extractClassName(drilldownToLastArray(ast));
      } else {
        className = extractClassName(ast);
      }
      return new Parameter(className, isArray);
    }
    throw new IllegalStateException("No Known TokenType has been found at line " + ast.getLineNo() + " of file "
        + getFileContents().getFilename());
  }

  private DetailAST drilldownToLastArray(DetailAST ast) {
    while (ast.findFirstToken(TokenTypes.ARRAY_DECLARATOR) != null) {
      ast = ast.findFirstToken(TokenTypes.ARRAY_DECLARATOR);
    }
    return ast;
  }

  private String extractClassName(DetailAST ast) {
    if (ast.findFirstToken(TokenTypes.DOT) != null) {
      return findLastToken(ast.findFirstToken(TokenTypes.DOT), TokenTypes.IDENT).getText();
    } else {
      return findLastToken(ast, TokenTypes.IDENT).getText();
    }
  }

  private DetailAST findLastToken(DetailAST astNode, int tokenType) {
    DetailAST retVal = null;
    for (AST child = astNode.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getType() == tokenType) {
        retVal = (DetailAST) child;
      }
    }
    return retVal;
  }

  private boolean isObjectType(DetailAST ast) {
    return ast.branchContains(TokenTypes.IDENT);
  }

  private boolean isArrayType(DetailAST ast) {
    return (ast.findFirstToken(TokenTypes.IDENT) == null) && (ast.branchContains(TokenTypes.ARRAY_DECLARATOR));
  }
}
