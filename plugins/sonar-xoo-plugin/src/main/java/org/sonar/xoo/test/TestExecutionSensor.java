/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.test;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase.Status;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.xoo.Xoo;

/**
 * Parse files *.xoo.test
 */
@DependedUpon("test-exec")
public class TestExecutionSensor implements Sensor {
  private static final Logger LOG = Loggers.get(TestExecutionSensor.class);

  private static final String TEST_EXTENSION = ".test";

  private final FileSystem fs;
  private final ResourcePerspectives perspectives;

  public TestExecutionSensor(FileSystem fileSystem, ResourcePerspectives perspectives) {
    this.fs = fileSystem;
    this.perspectives = perspectives;
  }

  private void processTestFile(InputFile inputFile, SensorContext context) {
    File testExecutionFile = new File(inputFile.file().getParentFile(), inputFile.file().getName() + TEST_EXTENSION);
    if (testExecutionFile.exists()) {
      LOG.debug("Processing " + testExecutionFile.getAbsolutePath());
      try {
        List<String> lines = FileUtils.readLines(testExecutionFile, fs.encoding().name());
        int lineNumber = 0;
        MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, inputFile);
        for (String line : lines) {
          lineNumber++;
          if (StringUtils.isBlank(line)) {
            continue;
          }
          if (line.startsWith("#")) {
            continue;
          }
          try {
            Iterator<String> split = Splitter.on(":").split(line).iterator();
            String name = split.next();
            String durationStr = split.next();
            Long duration = StringUtils.isNotBlank(durationStr) ? Long.parseLong(durationStr) : null;
            String msg = split.next();
            String stack = split.next();
            String status = split.next();
            String type = split.next();
            MutableTestCase testCase = testPlan.addTestCase(name);
            testCase.setDurationInMs(duration);
            testCase.setMessage(msg);
            testCase.setStackTrace(stack);
            testCase.setStatus(Status.valueOf(status));
            testCase.setType(type);
          } catch (Exception e) {
            throw new IllegalStateException("Error processing line " + lineNumber + " of file " + testExecutionFile.getAbsolutePath(), e);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Xoo Test Execution Sensor")
      .onlyOnLanguages(Xoo.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    FilePredicates p = context.fileSystem().predicates();
    for (InputFile file : context.fileSystem().inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(Type.TEST)))) {
      processTestFile(file, context);
    }
  }

}
