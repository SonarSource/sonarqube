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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsRequest;

import static com.codeborne.selenide.Condition.text;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.projectDir;

public class CoverageTest {

  private static final String[] ALL_COVERAGE_METRICS = new String[]{
    "line_coverage", "lines_to_cover", "uncovered_lines", "branch_coverage", "conditions_to_cover", "uncovered_conditions", "coverage",
    "it_line_coverage", "it_lines_to_cover", "it_uncovered_lines", "it_branch_coverage", "it_conditions_to_cover", "it_uncovered_conditions", "it_coverage",
    "overall_line_coverage", "overall_lines_to_cover", "overall_uncovered_lines", "overall_branch_coverage", "overall_conditions_to_cover", "overall_uncovered_conditions",
    "overall_coverage"};

  @ClassRule
  public static Orchestrator orchestrator = TestSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void coverage() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-ut-coverage")));

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "sample-ut-coverage", ALL_COVERAGE_METRICS);
    assertThat(measures.get("line_coverage")).isEqualTo(50.0);
    assertThat(measures.get("lines_to_cover")).isEqualTo(4d);
    assertThat(measures.get("uncovered_lines")).isEqualTo(2d);
    assertThat(measures.get("branch_coverage")).isEqualTo(50.0);
    assertThat(measures.get("conditions_to_cover")).isEqualTo(2d);
    assertThat(measures.get("uncovered_conditions")).isEqualTo(1d);
    assertThat(measures.get("coverage")).isEqualTo(50.0);

    assertThat(measures.get("it_coverage")).isNull();

    assertThat(measures.get("overall_coverage")).isNull();

    GetRequest getRequest = new GetRequest("api/sources/lines").setParam("key", "sample-ut-coverage:src/main/xoo/sample/Sample.xoo");
    String coverage = cleanupScmAndDuplication(tester.wsClient().wsConnector().call(getRequest).content());
    // Use strict checking to be sure IT coverage is not present
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/unit_test_coverage-expected.json"), "UTF-8"), coverage, true);

    verifyComputeEngineTempDirIsEmpty();
  }
  
  private String cleanupScmAndDuplication(String coverage) {
    JsonObject root = new JsonParser().parse(coverage).getAsJsonObject();
    JsonArray sources = root.getAsJsonArray("sources");
    
    for(JsonElement e : sources) {
      JsonObject line = e.getAsJsonObject();
      line.remove("scmDate");
      line.remove("scmAuthor");
      line.remove("scmRevision");
      line.remove("duplicated");
    }
    
    
    return root.toString();
  }

  @Test
  public void coverage_no_condition() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-ut-coverage-no-condition")));

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "sample-ut-coverage", ALL_COVERAGE_METRICS);
    assertThat(measures.get("line_coverage")).isEqualTo(50.0);
    assertThat(measures.get("lines_to_cover")).isEqualTo(4d);
    assertThat(measures.get("uncovered_lines")).isEqualTo(2d);
    assertThat(measures.get("branch_coverage")).isNull();
    assertThat(measures.get("conditions_to_cover")).isNull();
    assertThat(measures.get("uncovered_conditions")).isNull();
    assertThat(measures.get("coverage")).isEqualTo(50.0);

    assertThat(measures.get("it_coverage")).isNull();

    assertThat(measures.get("overall_coverage")).isNull();

    WsRequest getRequest = new GetRequest("api/sources/lines").setParam("key", "sample-ut-coverage:src/main/xoo/sample/Sample.xoo");
    String coverage = cleanupScmAndDuplication(tester.wsClient().wsConnector().call(getRequest).content());
    // Use strict checking to be sure IT coverage is not present
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/unit_test_coverage_no_condition-expected.json"), "UTF-8"), coverage,
      true);

    verifyComputeEngineTempDirIsEmpty();
  }

  @Test
  public void it_coverage_imported_as_coverage() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-it-coverage")));

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "sample-it-coverage", ALL_COVERAGE_METRICS);

    // Since SQ 6.2 all coverage reports are merged as coverage

    assertThat(measures.get("line_coverage")).isEqualTo(50.0);
    assertThat(measures.get("lines_to_cover")).isEqualTo(4d);
    assertThat(measures.get("uncovered_lines")).isEqualTo(2d);
    assertThat(measures.get("branch_coverage")).isEqualTo(50.0);
    assertThat(measures.get("conditions_to_cover")).isEqualTo(2d);
    assertThat(measures.get("uncovered_conditions")).isEqualTo(1d);
    assertThat(measures.get("coverage")).isEqualTo(50.0);

    assertThat(measures.get("it_coverage")).isNull();

    assertThat(measures.get("overall_coverage")).isNull();

    WsRequest getRequest = new GetRequest("api/sources/lines").setParam("key", "sample-it-coverage:src/main/xoo/sample/Sample.xoo");
    String coverage = cleanupScmAndDuplication(tester.wsClient().wsConnector().call(getRequest).content());
    // Use strict checking to be sure UT coverage is not present
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/it_coverage-expected.json"), "UTF-8"), coverage, true);

    verifyComputeEngineTempDirIsEmpty();
  }

  @Test
  public void ut_and_it_coverage_merged_in_coverage() throws Exception {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-overall-coverage")));

    // Since SQ 6.2 all coverage reports are merged as coverage

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "sample-overall-coverage", ALL_COVERAGE_METRICS);
    assertThat(measures.get("line_coverage")).isEqualTo(75.0);
    assertThat(measures.get("lines_to_cover")).isEqualTo(4);
    assertThat(measures.get("uncovered_lines")).isEqualTo(1);
    assertThat(measures.get("branch_coverage")).isEqualTo(50.0);
    assertThat(measures.get("conditions_to_cover")).isEqualTo(4);
    assertThat(measures.get("uncovered_conditions")).isEqualTo(2);
    assertThat(measures.get("coverage")).isEqualTo(62.5);

    assertThat(measures.get("it_coverage")).isNull();

    assertThat(measures.get("overall_coverage")).isNull();

    WsRequest getRequest = new GetRequest("api/sources/lines").setParam("key", "sample-overall-coverage:src/main/xoo/sample/Sample.xoo");
    String coverage = cleanupScmAndDuplication(tester.wsClient().wsConnector().call(getRequest).content());
    // Use strict checking to be sure no extra coverage is present
    JSONAssert.assertEquals(IOUtils.toString(this.getClass().getResourceAsStream("/test/CoverageTest/ut_and_it_coverage-expected.json"), "UTF-8"), coverage, true);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_compute_coverage_on_project() {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-half-covered")));

    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, "xoo-half-covered", ALL_COVERAGE_METRICS).get("coverage")).isEqualTo(50.0);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_ignore_coverage_on_full_path() {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-half-covered"))
      .setProperty("sonar.coverage.exclusions", "src/main/xoo/org/sonar/tests/halfcovered/UnCovered.xoo"));

    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, "xoo-half-covered", ALL_COVERAGE_METRICS).get("coverage")).isEqualTo(100.0);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_ignore_coverage_on_pattern() {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-half-covered"))
      .setProperty("sonar.coverage.exclusions", "**/UnCovered*"));

    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, "xoo-half-covered", ALL_COVERAGE_METRICS).get("coverage")).isEqualTo(100.0);

    verifyComputeEngineTempDirIsEmpty();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_not_have_coverage_at_all() {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-half-covered"))
      .setProperty("sonar.coverage.exclusions", "**/*"));

    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, "xoo-half-covered", ALL_COVERAGE_METRICS).get("coverage")).isNull();

    verifyComputeEngineTempDirIsEmpty();
  }

  @Test
  public void component_viewer_should_show_uncovered_conditions() {
    orchestrator.executeBuilds(SonarScanner.create(projectDir("testing/xoo-sample-new-coverage-v2")));

    tester.openBrowser()
      .openCode("sample-new-coverage", "sample-new-coverage:src/main/xoo/sample/Sample.xoo")
      .getSourceViewer()
      .openCoverageDetails(7)
      .shouldHave(text("2 of 3"));
  }

  private void verifyComputeEngineTempDirIsEmpty() {
    File ceTempDirectory = new File(new File(orchestrator.getServer().getHome(), "temp"), "ce");
    assertThat(FileUtils.listFiles(ceTempDirectory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)).isEmpty();
  }

}
