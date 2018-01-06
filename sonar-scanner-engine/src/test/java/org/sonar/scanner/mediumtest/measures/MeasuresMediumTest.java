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
package org.sonar.scanner.mediumtest.measures;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.output.ScannerReport.Measure;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.fail;

public class MeasuresMediumTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File baseDir;
  private File srcDir;

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Before
  public void setUp() throws Exception {
    baseDir = temp.newFolder();
    srcDir = new File(baseDir, "src");
    srcDir.mkdir();
  }

  @Test
  public void applyExclusionsOnCoverageMeasures() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\n\ncontent");

    File measures = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(measures, "lines_to_cover:2");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .containsOnly(tuple("lines_to_cover", 2));

    result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.coverage.exclusions", "src/sample.xoo")
        .build())
      .execute();

    allMeasures = result.allMeasures();
    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .isEmpty();
  }

  @Test
  public void deprecatedCoverageMeasuresAreConverted() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\n\ncontent");

    File measures = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(measures, "it_lines_to_cover:2");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .containsOnly(tuple("lines_to_cover", 2));

    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Coverage measure for metric 'lines_to_cover' should not be saved directly by a Sensor. Plugin should be updated to use SensorContext::newCoverage instead.");
  }

  @Test
  public void failIfTryingToSaveServerSideMeasure() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\n\ncontent");

    File measures = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(measures, "new_lines:2");

    try {
      tester.newTask()
        .properties(ImmutableMap.<String, String>builder()
          .put("sonar.task", "scan")
          .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
          .put("sonar.projectKey", "com.foo.project")
          .put("sonar.projectName", "Foo Project")
          .put("sonar.projectVersion", "1.0-SNAPSHOT")
          .put("sonar.projectDescription", "Description of Foo Project")
          .put("sonar.sources", "src")
          .build())
        .execute();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e)
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasStackTraceContaining("Metric 'new_lines' should not be computed by a Sensor");
    }
  }

  @Test
  public void lineMeasures() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\n\n\ncontent");

    File lineMeasures = new File(srcDir, "sample.xoo.linemeasures");
    FileUtils.write(lineMeasures, "ncloc_data:1=1;2=0;4=1");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value", "stringValue.value")
      .containsExactly(tuple("ncloc_data", 0, "1=1;4=1"));
  }

  @Test
  public void projectLevelMeasures() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\n\n\ncontent");

    File projectMeasures = new File(baseDir, "module.measures");
    FileUtils.write(projectMeasures, "tests:10");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project"))
      .extracting("metricKey", "intValue.value", "stringValue.value")
      .containsExactly(tuple("tests", 10, ""));
  }

}
