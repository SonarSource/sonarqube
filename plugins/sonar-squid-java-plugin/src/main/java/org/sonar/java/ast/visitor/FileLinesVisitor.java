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

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.plugins.squid.SonarAccessor;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.text.Source;

/**
 * Saves information about lines directly into Sonar index by using {@link FileLinesContext}.
 */
public class FileLinesVisitor extends JavaAstVisitor {

  private final SonarAccessor sonarAccessor;

  /**
   * Default constructor for case when {@link SonarAccessor} not available.
   */
  public FileLinesVisitor() {
    this.sonarAccessor = null;
  }

  public FileLinesVisitor(SonarAccessor sonarAccessor) {
    this.sonarAccessor = sonarAccessor;
  }

  @Override
  public void visitFile(DetailAST ast) {
    if (sonarAccessor != null) {
      processFile();
    }
  }

  private void processFile() {
    SourceFile file = (SourceFile) peekSourceCode();
    FileLinesContext measures = sonarAccessor.createFileLinesContext(file);

    Source source = getSource();
    for (int line = 1; line <= source.getNumberOfLines(); line++) {
      int lineOfCode = source.getMeasure(Metric.LINES_OF_CODE, line, line);
      measures.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, lineOfCode);

      int comment = source.getMeasure(Metric.COMMENT_LINES, line, line);
      measures.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, comment);
    }
    measures.save();
  }

}
