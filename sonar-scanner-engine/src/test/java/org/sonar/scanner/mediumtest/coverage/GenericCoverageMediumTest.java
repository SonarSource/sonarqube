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
package org.sonar.scanner.mediumtest.coverage;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.scanner.mediumtest.BatchMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class GenericCoverageMediumTest {

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

  @Test
  public void singleReport() throws IOException {

    File projectDir = new File("src/test/resources/mediumtest/xoo/sample-generic-coverage");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.coverageReportPaths", "coverage.xml")
      .start();

    InputFile noConditions = result.inputFile("xources/hello/NoConditions.xoo");
    assertThat(result.coverageFor(noConditions, 6).getHits()).isTrue();
    assertThat(result.coverageFor(noConditions, 6).getConditions()).isEqualTo(0);
    assertThat(result.coverageFor(noConditions, 6).getCoveredConditions()).isEqualTo(0);

    assertThat(result.coverageFor(noConditions, 7).getHits()).isFalse();

    assertThat(result.allMeasures().get(noConditions.key())).extracting("metricKey", "intValue.value", "stringValue.value")
      .containsOnly(
        tuple(CoreMetrics.LINES_KEY, 8, ""),
        tuple(CoreMetrics.LINES_TO_COVER_KEY, 2, ""),
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 1, ""),
        tuple(CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, 0, "6=1;7=0"));

    InputFile withConditions = result.inputFile("xources/hello/WithConditions.xoo");
    assertThat(result.coverageFor(withConditions, 3).getHits()).isTrue();
    assertThat(result.coverageFor(withConditions, 3).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(withConditions, 3).getCoveredConditions()).isEqualTo(1);

    assertThat(result.allMeasures().get(withConditions.key())).extracting("metricKey", "intValue.value", "stringValue.value")
      .containsOnly(
        tuple(CoreMetrics.LINES_KEY, 6, ""),
        tuple(CoreMetrics.LINES_TO_COVER_KEY, 1, ""),
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 0, ""),
        tuple(CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, 0, "3=1"),
        tuple(CoreMetrics.CONDITIONS_TO_COVER_KEY, 2, ""),
        tuple(CoreMetrics.UNCOVERED_CONDITIONS_KEY, 1, ""),
        tuple(CoreMetrics.CONDITIONS_BY_LINE_KEY, 0, "3=2"),
        tuple(CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, 0, "3=1")

    );
  }

  @Test
  public void twoReports() throws IOException {

    File projectDir = new File("src/test/resources/mediumtest/xoo/sample-generic-coverage");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.coverageReportPaths", "coverage.xml,coverage2.xml")
      .start();

    InputFile noConditions = result.inputFile("xources/hello/NoConditions.xoo");
    assertThat(result.coverageFor(noConditions, 6).getHits()).isTrue();
    assertThat(result.coverageFor(noConditions, 6).getConditions()).isEqualTo(0);
    assertThat(result.coverageFor(noConditions, 6).getCoveredConditions()).isEqualTo(0);

    assertThat(result.coverageFor(noConditions, 7).getHits()).isTrue();

    assertThat(result.allMeasures().get(noConditions.key())).extracting("metricKey", "intValue.value", "stringValue.value")
      .containsOnly(
        tuple(CoreMetrics.LINES_KEY, 8, ""),
        tuple(CoreMetrics.LINES_TO_COVER_KEY, 2, ""),
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 0, ""),
        tuple(CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, 0, "6=1;7=1"));

    InputFile withConditions = result.inputFile("xources/hello/WithConditions.xoo");
    assertThat(result.coverageFor(withConditions, 3).getHits()).isTrue();
    assertThat(result.coverageFor(withConditions, 3).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(withConditions, 3).getCoveredConditions()).isEqualTo(2);

    assertThat(result.allMeasures().get(withConditions.key())).extracting("metricKey", "intValue.value", "stringValue.value")
      .containsOnly(
        tuple(CoreMetrics.LINES_KEY, 6, ""),
        tuple(CoreMetrics.LINES_TO_COVER_KEY, 1, ""),
        tuple(CoreMetrics.UNCOVERED_LINES_KEY, 0, ""),
        tuple(CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, 0, "3=2"),
        tuple(CoreMetrics.CONDITIONS_TO_COVER_KEY, 2, ""),
        tuple(CoreMetrics.UNCOVERED_CONDITIONS_KEY, 0, ""),
        tuple(CoreMetrics.CONDITIONS_BY_LINE_KEY, 0, "3=2"),
        tuple(CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY, 0, "3=2")

    );
  }

}
