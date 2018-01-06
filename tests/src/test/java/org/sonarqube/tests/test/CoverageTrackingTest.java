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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsRequest;

import static util.ItUtils.projectDir;

public class CoverageTrackingTest {

  @ClassRule
  public static Orchestrator orchestrator = TestSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void test_coverage_per_test() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-with-coverage-per-test")));

    WsRequest getRequest = new GetRequest("api/tests/list").setParam("testFileKey", "sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    String tests = tester.wsClient().wsConnector().call(getRequest).content();
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTrackingTest/tests-expected.json"), "UTF-8"), tests, false);

    String covered_files = orchestrator.getServer().adminWsClient()
      .get("api/tests/covered_files", "testId", extractSuccessfulTestId(tests));
    JSONAssert
      .assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTrackingTest/covered_files-expected.json"), "UTF-8"), covered_files, false);
  }

  private String extractSuccessfulTestId(String json) {
    Matcher jsonObjectMatcher = Pattern.compile(".*\\{((.*?)success(.*?))\\}.*", Pattern.MULTILINE).matcher(json);
    jsonObjectMatcher.find();

    Matcher idMatcher = Pattern.compile(".*\"id\"\\s*?:\\s*?\"(\\S*?)\".*", Pattern.MULTILINE).matcher(jsonObjectMatcher.group(1));
    return idMatcher.find() ? idMatcher.group(1) : "";
  }
}
