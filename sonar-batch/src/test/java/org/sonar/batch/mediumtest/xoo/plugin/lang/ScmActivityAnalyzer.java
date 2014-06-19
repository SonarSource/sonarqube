/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.mediumtest.xoo.plugin.lang;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.analyzer.Analyzer;
import org.sonar.api.batch.analyzer.AnalyzerContext;
import org.sonar.api.batch.analyzer.AnalyzerDescriptor;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.mediumtest.xoo.plugin.base.Xoo;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ScmActivityAnalyzer implements Analyzer {

  private static final Logger LOG = LoggerFactory.getLogger(ScmActivityAnalyzer.class);

  private static final String SCM_EXTENSION = ".scm";

  private final FileSystem fs;
  private final FileLinesContextFactory fileLinesContextFactory;

  public ScmActivityAnalyzer(FileLinesContextFactory fileLinesContextFactory, FileSystem fileSystem) {
    this.fs = fileSystem;
    this.fileLinesContextFactory = fileLinesContextFactory;
  }

  @Override
  public void describe(AnalyzerDescriptor descriptor) {
    descriptor
      .name(this.getClass().getSimpleName())
      .provides(CoreMetrics.SCM_AUTHORS_BY_LINE,
        CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
        CoreMetrics.SCM_REVISIONS_BY_LINE)
      .workOnLanguages(Xoo.KEY);
  }

  @Override
  public void analyse(AnalyzerContext context) {
    for (InputFile inputFile : fs.inputFiles(fs.predicates().hasLanguage(Xoo.KEY))) {
      processFile(inputFile);
    }

  }

  @VisibleForTesting
  protected void processFile(InputFile inputFile) {
    File ioFile = inputFile.file();
    File scmDataFile = new java.io.File(ioFile.getParentFile(), ioFile.getName() + SCM_EXTENSION);
    if (!scmDataFile.exists()) {
      LOG.debug("Skipping SCM data injection for " + inputFile.relativePath());
      return;
    }

    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
    try {
      List<String> lines = FileUtils.readLines(scmDataFile, Charsets.UTF_8.name());
      int lineNumber = 0;
      for (String line : lines) {
        lineNumber++;
        if (StringUtils.isNotBlank(line)) {
          // revision,author,dateTime
          String[] fields = StringUtils.split(line, ',');
          if (fields.length < 3) {
            throw new IllegalStateException("Not enough fields on line " + lineNumber);
          }
          String revision = fields[0];
          String author = fields[1];
          // Will throw an exception, when date is not in format "yyyy-MM-dd"
          Date date = DateUtils.parseDate(fields[2]);

          fileLinesContext.setStringValue(CoreMetrics.SCM_REVISIONS_BY_LINE_KEY, lineNumber, revision);
          fileLinesContext.setStringValue(CoreMetrics.SCM_AUTHORS_BY_LINE_KEY, lineNumber, author);
          fileLinesContext.setStringValue(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY, lineNumber, DateUtils.formatDateTime(date));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    fileLinesContext.save();
  }
}
