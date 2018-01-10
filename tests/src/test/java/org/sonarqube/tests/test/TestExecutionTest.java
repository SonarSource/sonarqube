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
package org.sonarqube.tests.test;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.projectDir;

public class TestExecutionTest {

  @ClassRule
  public static Orchestrator orchestrator = TestSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

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

    WsRequest getRequest = new GetRequest("api/tests/list").setParam("testFileKey", "sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    String json = tester.wsClient().wsConnector().call(getRequest).content();
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/TestExecutionTest/expected.json"), "UTF-8"), json, false);
  }

  @Test
  public void test_execution_measures() {
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
