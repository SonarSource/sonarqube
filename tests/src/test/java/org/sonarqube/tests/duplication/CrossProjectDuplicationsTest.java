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
package org.sonarqube.tests.duplication;

import com.google.common.collect.ObjectArrays;
import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category4Suite;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import util.ItUtils;
import util.issue.IssueRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static util.ItUtils.getMeasureAsDouble;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;
import static util.selenium.Selenese.runSelenese;

public class CrossProjectDuplicationsTest {

  static final String ORIGIN_PROJECT = "origin-project";
  static final String DUPLICATE_PROJECT = "duplicate-project";
  static final String PROJECT_WITH_EXCLUSION = "project-with-exclusion";
  static final String PROJECT_WITHOUT_ENOUGH_TOKENS = "project_without_enough_tokens";

  static final String DUPLICATE_FILE = DUPLICATE_PROJECT + ":src/main/xoo/sample/File1.xoo";
  static final String BRANCH = "with-branch";

  static final String ORIGIN_PATH = "duplications/cross-project/origin";
  static final String DUPLICATE_PATH = "duplications/cross-project/duplicate";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @ClassRule
  public static final IssueRule issueRule = IssueRule.from(orchestrator);

  @BeforeClass
  public static void analyzeProjects() {
    orchestrator.resetData();
    ItUtils.restoreProfile(orchestrator, CrossProjectDuplicationsTest.class.getResource("/duplication/xoo-duplication-profile.xml"));

    analyzeProject(ORIGIN_PROJECT, ORIGIN_PATH);
    analyzeProject(DUPLICATE_PROJECT, DUPLICATE_PATH);
    analyzeProjectWithBranch(DUPLICATE_PROJECT, DUPLICATE_PATH, BRANCH);
    analyzeProject(PROJECT_WITH_EXCLUSION, DUPLICATE_PATH, "sonar.cpd.exclusions", "**/File*");

    // Set minimum tokens to a big value in order to not get duplications
    setServerProperty(orchestrator, "sonar.cpd.xoo.minimumTokens", "1000");
    analyzeProject(PROJECT_WITHOUT_ENOUGH_TOKENS, DUPLICATE_PATH);
  }

  @AfterClass
  public static void resetServerProperties() throws Exception {
    setServerProperty(orchestrator, "sonar.cpd.xoo.minimumTokens", null);
  }

  @Test
  public void origin_project_has_no_duplication_as_it_has_not_been_analyzed_twice() throws Exception {
    assertProjectHasNoDuplication(ORIGIN_PROJECT);
  }

  @Test
  public void duplicate_project_has_duplication_as_it_has_been_analyzed_twice() throws Exception {
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, DUPLICATE_PROJECT, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density");
    assertThat(measures.get("duplicated_lines").intValue()).isEqualTo(27);
    assertThat(measures.get("duplicated_blocks").intValue()).isEqualTo(1);
    assertThat(measures.get("duplicated_files").intValue()).isEqualTo(1);
    assertThat(measures.get("duplicated_lines_density")).isEqualTo(45d);
  }

  @Test
  public void issue_on_duplicated_blocks_is_generated_on_file() throws Exception {
    assertThat(issueRule.search(new SearchWsRequest().setComponentKeys(singletonList(DUPLICATE_FILE)).setRules(singletonList("common-xoo:DuplicatedBlocks"))).getIssuesList())
      .hasSize(1);
  }

  @Test
  public void verify_sources_lines_ws_duplication_information() throws Exception {
    verifyWsResultOnDuplicateFile("api/sources/lines", "sources_lines_duplication-expected.json");
  }

  @Test
  public void verify_duplications_show_ws() throws Exception {
    verifyWsResultOnDuplicateFile("api/duplications/show", "duplications_show-expected.json");
  }

  @Test
  public void project_with_branch_has_no_duplication() throws Exception {
    assertProjectHasNoDuplication(DUPLICATE_PROJECT + ":" + BRANCH);
  }

  @Test
  public void project_with_exclusion_has_no_duplication() throws Exception {
    assertProjectHasNoDuplication(PROJECT_WITH_EXCLUSION);
  }

  @Test
  public void project_without_enough_tokens_has_duplication() throws Exception {
    assertProjectHasNoDuplication(PROJECT_WITHOUT_ENOUGH_TOKENS);
  }

  @Test
  public void verify_viewer() {
    runSelenese(orchestrator, "/duplication/CrossProjectDuplicationsTest/cross-project-duplications-viewer.html");
  }

  private static void analyzeProject(String projectKey, String path, String... additionalProperties) {
    initProject(projectKey);
    executeAnalysis(projectKey, path, additionalProperties);
  }

  private static void analyzeProjectWithBranch(String projectKey, String path, String branch) {
    initProject(projectKey + ":" + branch);
    executeAnalysis(projectKey, path, "sonar.branch", branch);
  }

  private static void initProject(String effectiveProjectKey) {
    orchestrator.getServer().provisionProject(effectiveProjectKey, effectiveProjectKey);
    orchestrator.getServer().associateProjectToQualityProfile(effectiveProjectKey, "xoo", "xoo-duplication-profile");
  }

  private static void executeAnalysis(String projectKey, String path, String... additionalProperties) {
    runProjectAnalysis(orchestrator, path,
      ObjectArrays.concat(
        new String[] {
          "sonar.cpd.cross_project", "true",
          "sonar.projectKey", projectKey,
          "sonar.projectName", projectKey
        },
        additionalProperties, String.class));
  }

  private static void assertProjectHasNoDuplication(String projectKey) {
    assertThat(getMeasureAsDouble(orchestrator, projectKey, "duplicated_lines")).isZero();
  }

  private static void verifyWsResultOnDuplicateFile(String ws, String expectedFilePath) throws Exception {
    String duplication = newAdminWsClient(orchestrator).wsConnector().call(new GetRequest(ws).setParam("key", DUPLICATE_FILE)).content();
    assertEquals(IOUtils.toString(CrossProjectDuplicationsTest.class.getResourceAsStream("/duplication/CrossProjectDuplicationsTest/" + expectedFilePath), "UTF-8"), duplication,
      false);
  }

}
