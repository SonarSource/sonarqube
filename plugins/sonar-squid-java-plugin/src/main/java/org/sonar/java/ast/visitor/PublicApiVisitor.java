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

import org.sonar.java.ast.check.UndocumentedApiCheck;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.AnnotationUtility;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.Scope;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class PublicApiVisitor extends JavaAstVisitor {

  final static String OVERRIDE_ANNOTATION_KEYWORD = "Override";

  public static final List<Integer> TOKENS = Arrays.asList(TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.METHOD_DEF,
      TokenTypes.CTOR_DEF, TokenTypes.ANNOTATION_DEF, TokenTypes.ANNOTATION_FIELD_DEF, TokenTypes.VARIABLE_DEF);

  public PublicApiVisitor() {
  }

  @Override
  public List<Integer> getWantedTokens() {
    return TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    if (isPublicApi(ast)) {
      currentResource.add(Metric.PUBLIC_API, 1);
      if (isDocumentedApi(ast)) {
        currentResource.add(Metric.PUBLIC_DOC_API, 1);
      }
    }
  }

  private static boolean isEmptyDefaultConstructor(DetailAST ast) {
    return (isConstructorWithoutParameters(ast)) && (ast.getLastChild().getChildCount() == 1);
  }

  private static boolean isConstructorWithoutParameters(DetailAST ast) {
    return ast.getType() == TokenTypes.CTOR_DEF && ast.findFirstToken(TokenTypes.PARAMETERS).getChildCount() == 0;
  }

  private static boolean isMethodWithOverrideAnnotation(DetailAST ast) {
    if (isMethod(ast)) {
      return AnnotationUtility.containsAnnotation(ast, OVERRIDE_ANNOTATION_KEYWORD)
          || AnnotationUtility.containsAnnotation(ast, "java.lang." + OVERRIDE_ANNOTATION_KEYWORD);
    }
    return false;
  }

  private static boolean isMethod(DetailAST ast) {
    return ast.getType() == TokenTypes.METHOD_DEF;
  }

  private boolean isDocumentedApi(DetailAST ast) {
    return isDocumentedApi(ast, getFileContents());
  }

  private static boolean isPublic(DetailAST ast) {
    return (AstUtils.isScope(AstUtils.getScope(ast), Scope.PUBLIC) || AstUtils.isType(ast, TokenTypes.ANNOTATION_FIELD_DEF));
  }

  private static boolean isStaticFinalVariable(DetailAST ast) {
    return (AstUtils.isClassVariable(ast) || AstUtils.isInterfaceVariable(ast)) && AstUtils.isFinal(ast) && AstUtils.isStatic(ast);
  }

  /**
   * Also used by {@link UndocumentedApiCheck}
   */
  public static boolean isDocumentedApi(DetailAST ast, FileContents fileContents) {
    return fileContents.getJavadocBefore(ast.getLineNo()) != null;
  }

  /**
   * Also used by {@link UndocumentedApiCheck}
   */
  public static boolean isPublicApi(DetailAST ast) {
    return isPublic(ast) && !isStaticFinalVariable(ast) && !isMethodWithOverrideAnnotation(ast) && !isEmptyDefaultConstructor(ast);
  }

}
