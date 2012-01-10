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

import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class FileVisitor extends JavaAstVisitor {

  @Override
  public void visitFile(DetailAST ast) {
    String fileName = extractFileNameFromFilePath(getFileContents().getFilename());
    SourceFile sourceFile = createSourceFile(peekParentPackage(), fileName);
    sourceFile.setMeasure(Metric.FILES, 1);
    addSourceCode(sourceFile);
  }

  @Override
  public void leaveFile(DetailAST ast) {
    popSourceCode();
  }

  static String extractFileNameFromFilePath(String filePath) {
    int lastIndexOfSlashOrBackSlashChar = filePath.lastIndexOf('/');
    if (lastIndexOfSlashOrBackSlashChar != -1) {
      return filePath.substring(lastIndexOfSlashOrBackSlashChar + 1);
    }
    lastIndexOfSlashOrBackSlashChar = filePath.lastIndexOf('\\');
    if (lastIndexOfSlashOrBackSlashChar != -1) {
      return filePath.substring(lastIndexOfSlashOrBackSlashChar + 1);
    }
    return filePath;
  }

  static SourceFile createSourceFile(SourcePackage parentPackage, String fileName) {
    StringBuilder key = new StringBuilder();
    if (parentPackage != null && !"".equals(parentPackage.getKey())) {
      key.append(parentPackage.getKey());
      key.append("/");
    }
    key.append(fileName);
    return new SourceFile(key.toString(), fileName);
  }
}
