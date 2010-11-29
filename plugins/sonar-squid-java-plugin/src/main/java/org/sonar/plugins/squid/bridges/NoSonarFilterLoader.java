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
package org.sonar.plugins.squid.bridges;

import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.resources.Resource;
import org.sonar.java.squid.SquidVisitorNotifier;
import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;

import java.util.HashSet;
import java.util.Set;

public class NoSonarFilterLoader extends Bridge {
  private NoSonarFilter noSonarFilter;

  protected NoSonarFilterLoader(NoSonarFilter noSonarFilter) {
    super(false);
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    if (noSonarFilter != null) {
      // lines with NOSONAR tag
      Set<Integer> ignoredLines = new HashSet<Integer>(squidFile.getNoSonarTagLines());
      // classes and methods with annotation SuppressWarnings
      new SquidVisitorNotifier(squidFile, new SuppressWarningsVisitor(ignoredLines)).notifyVisitors();

      noSonarFilter.addResource(sonarFile, ignoredLines);
    }
  }

  private static class SuppressWarningsVisitor implements SquidVisitor {
    private Set<Integer> ignoredLines;

    public SuppressWarningsVisitor(Set<Integer> ignoredLines) {
      this.ignoredLines = ignoredLines;
    }

    public void visitFile(SourceFile sourceFile) {
    }

    public void visitClass(SourceClass sourceClass) {
      if (sourceClass.isSuppressWarnings()) {
      }
    }

    public void visitMethod(SourceMethod sourceMethod) {
      if (sourceMethod.isSuppressWarnings()) {
        visitLines(sourceMethod);
      }
    }

    private void visitLines(SourceCode sourceCode) {
      for (int line = sourceCode.getStartAtLine(); line <= sourceCode.getEndAtLine(); line++) {
        ignoredLines.add(line);
      }
    }
  }

}
