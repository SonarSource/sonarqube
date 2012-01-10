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

import com.puppycrawl.tools.checkstyle.api.AnnotationUtility;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Inspired by {@link com.puppycrawl.tools.checkstyle.checks.annotation.SuppressWarningsCheck}
 */
public final class SuppressWarningsAnnotationUtils {

  private final static String SUPPRESS_WARNINGS_ANNOTATION_NAME = "SuppressWarnings";
  private final static String SUPPRESS_WARNINGS_ANNOTATION_FQ_NAME = "java.lang." + SUPPRESS_WARNINGS_ANNOTATION_NAME;
  private final static String VALUE = "\"all\"";

  public static boolean isSuppressAllWarnings(DetailAST ast) {
    DetailAST suppressWarningsAnnotation = getSuppressWarningsAnnotation(ast);
    if (suppressWarningsAnnotation != null) {
      DetailAST warningHolder = findWarningsHolder(suppressWarningsAnnotation);
      for (DetailAST warning = warningHolder.findFirstToken(TokenTypes.EXPR); warning != null; warning = warning.getNextSibling()) {
        if (warning.getType() == TokenTypes.EXPR) {
          DetailAST fChild = warning.getFirstChild();
          if (fChild.getType() == TokenTypes.STRING_LITERAL) {
            String text = warning.getFirstChild().getText();
            if (VALUE.equals(text)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static DetailAST findWarningsHolder(DetailAST aAnnotation) {
    DetailAST annValuePair = aAnnotation.findFirstToken(TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR);
    DetailAST annArrayInit;
    if (annValuePair != null) {
      annArrayInit = annValuePair.findFirstToken(TokenTypes.ANNOTATION_ARRAY_INIT);
    } else {
      annArrayInit = aAnnotation.findFirstToken(TokenTypes.ANNOTATION_ARRAY_INIT);
    }
    if (annArrayInit != null) {
      return annArrayInit;
    }
    return aAnnotation;
  }

  private static DetailAST getSuppressWarningsAnnotation(DetailAST ast) {
    DetailAST annotation = AnnotationUtility.getAnnotation(ast, SUPPRESS_WARNINGS_ANNOTATION_NAME);
    if (annotation == null) {
      annotation = AnnotationUtility.getAnnotation(ast, SUPPRESS_WARNINGS_ANNOTATION_FQ_NAME);
    }
    return annotation;
  }

  private SuppressWarningsAnnotationUtils() {
  }

}
