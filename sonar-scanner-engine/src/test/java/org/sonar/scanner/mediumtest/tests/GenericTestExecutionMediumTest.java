/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.mediumtest.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class GenericTestExecutionMediumTest {
  private final List<String> logs = new ArrayList<>();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void singleReport() {

    File projectDir = new File("test-resources/mediumtest/xoo/sample-generic-test-exec");

    AnalysisResult result = tester
      .setLogOutput((msg, level) -> logs.add(msg))
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.testExecutionReportPaths", "unittest.xml")
      .execute();

    InputFile testFile = result.inputFile("testx/ClassOneTest.xoo");
    assertThat(result.allMeasures().get(testFile.key())).extracting("metricKey", "intValue.value", "longValue.value")
      .containsOnly(
        tuple(CoreMetrics.TESTS_KEY, 3, 0L),
        tuple(CoreMetrics.SKIPPED_TESTS_KEY, 1, 0L),
        tuple(CoreMetrics.TEST_ERRORS_KEY, 1, 0L),
        tuple(CoreMetrics.TEST_EXECUTION_TIME_KEY, 0, 1105L),
        tuple(CoreMetrics.TEST_FAILURES_KEY, 1, 0L));

    assertThat(logs).noneMatch(l -> l.contains("Please use 'sonar.testExecutionReportPaths'"));
  }

  @Test
  public void twoReports() {

    File projectDir = new File("test-resources/mediumtest/xoo/sample-generic-test-exec");

    AnalysisResult result = tester
      .setLogOutput((msg, level) -> logs.add(msg))
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.testExecutionReportPaths", "unittest.xml,unittest2.xml")
      .execute();

    InputFile testFile = result.inputFile("testx/ClassOneTest.xoo");
    assertThat(result.allMeasures().get(testFile.key())).extracting("metricKey", "intValue.value", "longValue.value")
      .containsOnly(
        tuple(CoreMetrics.TESTS_KEY, 4, 0L),
        tuple(CoreMetrics.SKIPPED_TESTS_KEY, 2, 0L),
        tuple(CoreMetrics.TEST_ERRORS_KEY, 1, 0L),
        tuple(CoreMetrics.TEST_EXECUTION_TIME_KEY, 0, 1610L),
        tuple(CoreMetrics.TEST_FAILURES_KEY, 1, 0L));

    assertThat(logs).noneMatch(l -> l.contains("Please use 'sonar.testExecutionReportPaths'"));
  }

}
