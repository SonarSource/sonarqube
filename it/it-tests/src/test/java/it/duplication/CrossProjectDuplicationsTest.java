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
package it.duplication;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

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

  @BeforeClass
  public static void analyzeProjects() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/duplication/xoo-duplication-profile.xml"));

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
    assertThat(getMeasure(DUPLICATE_PROJECT, "duplicated_lines")).isEqualTo(27);
    assertThat(getMeasure(DUPLICATE_PROJECT, "duplicated_blocks")).isEqualTo(1);
    assertThat(getMeasure(DUPLICATE_PROJECT, "duplicated_files")).isEqualTo(1);
    assertThat(getComponent(DUPLICATE_PROJECT, "duplicated_lines_density").getMeasureValue("duplicated_lines_density")).isEqualTo(45d);
  }

  @Test
  public void issue_on_duplicated_blocks_is_generated_on_file() throws Exception {
    List<Issue> issues = orchestrator.getServer().wsClient().issueClient()
      .find(IssueQuery.create()
        .components(DUPLICATE_FILE)
        .rules("common-xoo:DuplicatedBlocks"))
      .list();
    assertThat(issues).hasSize(1);
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
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("duplications-viewer",
        "/duplication/CrossProjectDuplicationsTest/cross-project-duplications-viewer.html")
        .build())
      .runOn(orchestrator);
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
      ArrayUtils.addAll(
        new String[] {
          "sonar.cpd.cross_project", "true",
          "sonar.projectKey", projectKey,
          "sonar.projectName", projectKey
        },
        additionalProperties));
  }

  private static int getMeasure(String projectKey, String metricKey) {
    Integer intMeasure = getComponent(projectKey, metricKey).getMeasureIntValue(metricKey);
    assertThat(intMeasure).isNotNull();
    return intMeasure;
  }

  private static Resource getComponent(String projectKey, String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, metricKey));
  }

  private static void assertProjectHasNoDuplication(String projectKey) {
    assertThat(getMeasure(projectKey, "duplicated_lines")).isZero();
  }

  private static void verifyWsResultOnDuplicateFile(String ws, String expectedFilePath) throws Exception {
    String duplication = orchestrator.getServer().adminWsClient().get(ws, "key", DUPLICATE_FILE);
    assertEquals(IOUtils.toString(CrossProjectDuplicationsTest.class.getResourceAsStream("/duplication/CrossProjectDuplicationsTest/" + expectedFilePath), "UTF-8"), duplication,
      false);
  }

}
