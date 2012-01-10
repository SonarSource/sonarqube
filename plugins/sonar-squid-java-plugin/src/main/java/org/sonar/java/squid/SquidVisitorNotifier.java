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
package org.sonar.java.squid;

import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;

public class SquidVisitorNotifier {

  private final SourceFile sourceFile;
  private final SquidVisitor[] squidVisitors;

  public SquidVisitorNotifier(SourceFile sourceFile, SquidVisitor... squidVisitors) {
    this.sourceFile = sourceFile;
    this.squidVisitors = new SquidVisitor[squidVisitors.length];
    System.arraycopy(squidVisitors, 0, this.squidVisitors, 0, squidVisitors.length);
  }

  public void notifyVisitors() {
    callVisitFile();
  }

  private void callVisitFile() {
    for (SquidVisitor visitor : squidVisitors) {
      visitor.visitFile(sourceFile);
    }
    visitChildren(sourceFile);
  }

  private void visitChildren(SourceCode sourceCode) {
    if (sourceCode instanceof SourceClass) {
      for (SquidVisitor visitor : squidVisitors) {
        visitor.visitClass((SourceClass) sourceCode);
      }
    } else if (sourceCode instanceof SourceMethod) {
      for (SquidVisitor visitor : squidVisitors) {
        visitor.visitMethod((SourceMethod) sourceCode);
      }
    }

    if (sourceCode.hasChildren()) {
      for (SourceCode child : sourceCode.getChildren()) {
        visitChildren(child);
      }
    }
  }
}
