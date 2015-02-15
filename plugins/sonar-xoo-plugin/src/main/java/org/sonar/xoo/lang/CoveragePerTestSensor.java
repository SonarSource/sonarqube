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
package org.sonar.xoo.lang;

import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parse files *.xoo.coveragePerTest
 */
public class CoveragePerTestSensor implements Sensor {

  private static final Logger LOG = Loggers.get(CoveragePerTestSensor.class);

  private static final String COVER_PER_TEST_EXTENSION = ".coveragePerTest";

  private void processCoveragePerTest(InputFile inputFile, SensorContext context) {
    File ioFile = inputFile.file();
    File coverPerTestFile = new File(ioFile.getParentFile(), ioFile.getName() + COVER_PER_TEST_EXTENSION);
    if (coverPerTestFile.exists()) {
      LOG.debug("Processing " + coverPerTestFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(coverPerTestFile, context.fileSystem().encoding().name());
        int lineNumber = 0;
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line) || line.startsWith("#")) {
            continue;
          }
          processLine(coverPerTestFile, lineNumber, context, line, inputFile);
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void processLine(File coverPerTest, int lineNumber, SensorContext context, String line, InputFile testFile) {
    try {
      Iterator<String> split = Splitter.on(":").split(line).iterator();
      String testCaseName = split.next();
      String mainFileRelativePath = split.next();
      FileSystem fs = context.fileSystem();
      InputFile mainFile = fs.inputFile(fs.predicates().hasRelativePath(mainFileRelativePath));
      if (mainFile == null) {
        throw new IllegalStateException("Unable to find file " + mainFileRelativePath);
      }
      List<Integer> coveredLines = new ArrayList<Integer>();
      Iterator<String> lines = Splitter.on(",").split(split.next()).iterator();
      while (lines.hasNext()) {
        coveredLines.add(Integer.parseInt(lines.next()));
      }
      context.newTestCaseCoverage()
        .testFile(testFile)
        .testName(testCaseName)
        .cover(mainFile)
        .onLines(coveredLines)
        .save();
    } catch (Exception e) {
      throw new IllegalStateException("Error processing line " + lineNumber + " of file " + coverPerTest.getAbsolutePath(), e);
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Coverage Per Test Sensor")
      .onlyOnLanguages(Xoo.KEY)
      .onlyOnFileType(InputFile.Type.TEST);
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(InputFile.Type.TEST)))) {
      processCoveragePerTest(file, context);
    }
  }
}
