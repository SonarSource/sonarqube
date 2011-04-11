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

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.squid.api.AnalysisException;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.indexer.SquidIndex;
import org.sonar.squid.measures.Metric;

public class PackageVisitor extends JavaAstVisitor {

  private static final String ROOT_PACKAGE = "";
  
  private SquidIndex indexer;

  public PackageVisitor(SquidIndex indexer) {
    this.indexer = indexer;
  }

  @Override
  public void visitFile(DetailAST ast) {
    SourceCode packageRes = null;

    if (ast == null) {
      // ast can be null for empty files (all the file is commented-out)
      packageRes = guessPackage();
    } else {
      packageRes = createSourcePackage(ast);
    }
    if (peekSourceCode().hasChild(packageRes)) {
      packageRes = indexer.search(packageRes.getKey());
    }
    packageRes.setMeasure(Metric.PACKAGES, 1);
    addSourceCode(packageRes);
  }

  private SourcePackage guessPackage() {
    String directory = InputFileUtils.getRelativeDirectory(getInputFile());
    return new SourcePackage(directory);
  }

  @Override
  public void leaveFile(DetailAST ast) {
    popSourceCode();
  }

  private SourcePackage createSourcePackage(DetailAST ast) {
    String key = ROOT_PACKAGE;
    if (ast.getType() == TokenTypes.PACKAGE_DEF) {
      String packageName = FullIdent.createFullIdent(ast.getLastChild().getPreviousSibling()).getText();
      key = packageName.replace('.', '/');
    }
    checkPhysicalDirectory(key);
    return new SourcePackage(key);
  }

  /**
   * Check that package declaration is consistent with the physical location of Java file.
   * It aims to detect two cases :
   * - wrong package declaration : "package org.foo" stored in the directory "org/bar"
   * - source directory badly configured : src/ instead of src/main/java/
   *
   * @since 2.8
   */
  private void checkPhysicalDirectory(String key) {
    String relativeDirectory = InputFileUtils.getRelativeDirectory(getInputFile());
    // both relativeDirectory and key use slash '/' as separator
    if (!StringUtils.equals(relativeDirectory, key)) {
      String packageName = StringUtils.replace(key, "/", ".");
      if (StringUtils.contains(relativeDirectory, key) || StringUtils.contains(key, relativeDirectory)) {
        throw new AnalysisException(String.format("The source directory does not correspond to the package declaration %s", packageName));
      }
      throw new AnalysisException(String.format("The package declaration %s does not correspond to the file path %s",
          packageName, getInputFile().getRelativePath()));
    }
  }
}