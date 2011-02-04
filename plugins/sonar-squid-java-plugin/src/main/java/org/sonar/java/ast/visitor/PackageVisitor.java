/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.indexer.SquidIndex;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class PackageVisitor extends JavaAstVisitor {

  private SquidIndex indexer;

  public PackageVisitor(SquidIndex indexer) {
    this.indexer = indexer;
  }

  @Override
  public void visitFile(DetailAST ast) {
    if (ast == null) {
      // ast can be null for empty classes
      return;
    }
    SourceCode packageRes = createSourcePackage(ast);
    if (peekSourceCode().hasChild(packageRes)) {
      packageRes = indexer.search(packageRes.getKey());
    }
    packageRes.setMeasure(Metric.PACKAGES, 1);
    addSourceCode(packageRes);
  }

  @Override
  public void leaveFile(DetailAST ast) {
    if (ast == null) {
      // ast can be null for empty classes
      return;
    }
    popSourceCode();
  }

  private SourcePackage createSourcePackage(DetailAST ast) {
    SourcePackage packageRes;
    if (ast.getType() != TokenTypes.PACKAGE_DEF) {
      packageRes = new SourcePackage("");
    } else {
      String packageName = FullIdent.createFullIdent(ast.getLastChild().getPreviousSibling()).getText();
      packageRes = new SourcePackage(packageName.replace('.', '/'));
    }
    return packageRes;
  }
}