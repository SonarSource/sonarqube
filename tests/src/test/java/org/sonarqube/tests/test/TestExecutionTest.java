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
package org.sonarqube.tests.test;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category2Suite;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.projectDir;

public class TestExecutionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @Before
  public void delete_data() {
    orchestrator.resetData();
  }

  @Test
  public void test_execution_details() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-with-tests-execution-details")));

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "sample-with-tests",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time");
    assertThat(measures.get("test_success_density")).isEqualTo(33.3);
    assertThat(measures.get("test_failures")).isEqualTo(1);
    assertThat(measures.get("test_errors")).isEqualTo(1);
    assertThat(measures.get("tests")).isEqualTo(3);
    assertThat(measures.get("skipped_tests")).isEqualTo(1);
    assertThat(measures.get("test_execution_time")).isEqualTo(8);

    String json = orchestrator.getServer().adminWsClient().get("api/tests/list", "testFileKey", "sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/TestExecutionTest/expected.json"), "UTF-8"), json, false);
  }

  @Test
  public void test_execution_measures() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-with-tests-execution-measures")));

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "sample-with-tests",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time");
    assertThat(measures.get("test_success_density")).isEqualTo(33.3);
    assertThat(measures.get("test_failures")).isEqualTo(1);
    assertThat(measures.get("test_errors")).isEqualTo(1);
    assertThat(measures.get("tests")).isEqualTo(3);
    assertThat(measures.get("skipped_tests")).isEqualTo(1);
    assertThat(measures.get("test_execution_time")).isEqualTo(8);
  }
}
