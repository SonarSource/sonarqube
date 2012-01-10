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

import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class ClassVisitor extends JavaAstVisitor {

  public static final List<Integer> WANTED_TOKENS = Arrays.asList(TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.ENUM_DEF,
      TokenTypes.ANNOTATION_DEF);

  @Override
  public List<Integer> getWantedTokens() {
    return WANTED_TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    String className = ast.findFirstToken(TokenTypes.IDENT).getText();
    SourceClass unit;
    if (peekSourceCode().isType(SourceClass.class)) {
      unit = createSourceClass((SourceClass) peekSourceCode(), className);
    } else {
      unit = createSourceClass(peekParentPackage(), className);
    }
    addSourceCode(unit);
    unit.setStartAtLine(ast.getLineNo());
    unit.setMeasure(Metric.CLASSES, 1);
    if (isInterface(ast.getType())) {
      unit.setMeasure(Metric.INTERFACES, 1);
    }
    if (isAbstract(ast)) {
      unit.setMeasure(Metric.ABSTRACT_CLASSES, 1);
    }
    unit.setSuppressWarnings(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(ast));
  }

  @Override
  public void leaveToken(DetailAST ast) {
    popSourceCode();
  }

  private boolean isAbstract(DetailAST ast) {
    final DetailAST abstractAST = ast.findFirstToken(TokenTypes.MODIFIERS).findFirstToken(TokenTypes.ABSTRACT);
    return abstractAST != null;
  }

  private boolean isInterface(int type) {
    return type == TokenTypes.INTERFACE_DEF;
  }

  static SourceClass createSourceClass(SourcePackage parentPackage, String className) {
    StringBuilder key = new StringBuilder();
    if (parentPackage != null && !"".equals(parentPackage.getKey())) {
      key.append(parentPackage.getKey());
      key.append("/");
    }
    key.append(className);
    return new SourceClass(key.toString(), className);
  }

  static SourceClass createSourceClass(SourceClass parentClass, String innerClassName) {
    return new SourceClass(parentClass.getKey() + "$" + innerClassName, innerClassName);
  }

}
