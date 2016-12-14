/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.mediumtest.BatchMediumTester;
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

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .build();

  @Before
  public void prepare() {
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Before
  public void setUp() throws Exception {
    baseDir = temp.newFolder();
    srcDir = new File(baseDir, "src");
    srcDir.mkdir();
  }

  @Test
  public void computeMeasuresOnTempProject() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    File xooMeasureFile = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(xooFile, "Sample xoo\ncontent");
    FileUtils.write(xooMeasureFile, "lines:20");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.skip", "true")
        .build())
      .start();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project")).extracting("metricKey", "stringValue.value").isEmpty();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value").containsOnly(
      tuple(CoreMetrics.LINES_KEY, 2));
  }

  @Test
  public void computeLinesOnAllFiles() throws IOException {
    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\n\ncontent");

    File otherFile = new File(srcDir, "sample.other");
    FileUtils.write(otherFile, "Sample other\ncontent\n");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.import_unknown_files", "true")
        .build())
      .start();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .contains(tuple("lines", 3));
    assertThat(allMeasures.get("com.foo.project:src/sample.other")).extracting("metricKey", "intValue.value")
      .contains(tuple("lines", 3), tuple("ncloc", 2));
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
      .start();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .containsOnly(tuple("lines", 3),
        tuple("lines_to_cover", 2));

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
      .start();

    allMeasures = result.allMeasures();
    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .containsOnly(tuple("lines", 3));
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
      .start();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .containsOnly(tuple("lines", 3),
        tuple("lines_to_cover", 2));

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
        .start();
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
      .start();

    Map<String, List<Measure>> allMeasures = result.allMeasures();

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value", "stringValue.value")
      .containsExactly(tuple("lines", 4, ""), tuple("ncloc_data", 0, "1=1;4=1"));
  }

}
