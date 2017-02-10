/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.mediumtest.coverage;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class CoverageMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public ScannerMediumTester tester = ScannerMediumTester.builder()
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

  @Test
  public void singleReport() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}");
    FileUtils.write(xooUtCoverageFile, "2:2:2:1\n3:1");

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

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2).getHits()).isTrue();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 2).getCoveredConditions()).isEqualTo(1);

    Map<String, List<org.sonar.scanner.protocol.output.ScannerReport.Measure>> allMeasures = result.allMeasures();
    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .contains(tuple(CoreMetrics.LINES_TO_COVER_KEY, 2),
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 0),
        tuple(CoreMetrics.CONDITIONS_TO_COVER_KEY, 2),
        tuple(CoreMetrics.UNCOVERED_CONDITIONS_KEY, 1));
  }

  @Test
  public void twoReports() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}");
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooUtCoverageFile, "2:2:2:2\n4:0");
    File xooItCoverageFile = new File(srcDir, "sample.xoo.itcoverage");
    FileUtils.write(xooItCoverageFile, "2:2:2:1\n3:1\n5:0");

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

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2).getHits()).isTrue();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 2).getCoveredConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 3).getHits()).isTrue();

    Map<String, List<org.sonar.scanner.protocol.output.ScannerReport.Measure>> allMeasures = result.allMeasures();
    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value", "stringValue.value")
      .contains(tuple(CoreMetrics.LINES_TO_COVER_KEY, 4, ""), // 2, 3, 4, 5
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 2, ""), // 4, 5
        tuple(CoreMetrics.CONDITIONS_TO_COVER_KEY, 2, ""), // 2 x 2
        tuple(CoreMetrics.UNCOVERED_CONDITIONS_KEY, 0, ""),
        tuple(CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, 0, "2=4;3=1;4=0;5=0"),
        tuple(CoreMetrics.CONDITIONS_BY_LINE_KEY, 0, "2=2"),
        tuple(CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, 0, "2=2"));
  }

  @Test
  public void exclusions() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}");
    FileUtils.write(xooUtCoverageFile, "2:2:2:1\n3:1");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.coverage.exclusions", "**/sample.xoo")
        .build())
      .start();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2)).isNull();

    Map<String, List<org.sonar.scanner.protocol.output.ScannerReport.Measure>> allMeasures = result.allMeasures();
    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey")
      .doesNotContain(CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.CONDITIONS_TO_COVER_KEY,
        CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);
  }

  @Test
  public void fallbackOnExecutableLines() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File measuresFile = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}");
    FileUtils.write(measuresFile, "executable_lines_data:2=1;3=1;4=0");

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

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 1)).isNull();

    assertThat(result.coverageFor(file, 2).getHits()).isFalse();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(0);
    assertThat(result.coverageFor(file, 2).getCoveredConditions()).isEqualTo(0);

    assertThat(result.coverageFor(file, 3).getHits()).isFalse();
    assertThat(result.coverageFor(file, 4)).isNull();

    Map<String, List<org.sonar.scanner.protocol.output.ScannerReport.Measure>> allMeasures = result.allMeasures();
    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey", "intValue.value")
      .contains(tuple(CoreMetrics.LINES_TO_COVER_KEY, 2),
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 2));

    assertThat(allMeasures.get("com.foo.project:src/sample.xoo")).extracting("metricKey").doesNotContain(CoreMetrics.CONDITIONS_TO_COVER_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY);
  }

}
