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
package org.sonarqube.tests.duplication;

import com.google.common.collect.ObjectArrays;
import com.sonar.orchestrator.Orchestrator;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;
import util.issue.IssueRule;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.runProjectAnalysis;

public class DuplicationsTest {

  private static final String DUPLICATIONS = "file-duplications";
  private static final String DUPLICATIONS_WITH_EXCLUSIONS = "file-duplications-with-exclusions";
  private static final String WITHOUT_ENOUGH_TOKENS = "project_without_enough_tokens";

  @ClassRule
  public static Orchestrator orchestrator = DuplicationSuite.ORCHESTRATOR;

  @ClassRule
  public static final IssueRule issueRule = IssueRule.from(orchestrator);

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void analyzeProjects() {
    ItUtils.restoreProfile(orchestrator, DuplicationsTest.class.getResource("/duplication/xoo-duplication-profile.xml"));
    analyzeProject(DUPLICATIONS);
    analyzeProject(DUPLICATIONS_WITH_EXCLUSIONS, "sonar.cpd.exclusions", "**/File*");

    // Set minimum tokens to a big value in order to not get duplications
    tester.settings().setGlobalSettings("sonar.cpd.xoo.minimumTokens", "1000");
    analyzeProject(WITHOUT_ENOUGH_TOKENS);
  }

  private static Map<String, Double> getMeasures(String key) {
    return getMeasuresAsDoubleByMetricKey(orchestrator, key, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density");
  }

  private static void verifyDuplicationMeasures(String componentKey, int duplicatedBlocks, int duplicatedLines, int duplicatedFiles, double duplicatedLinesDensity) {
    Map<String, Double> measures = getMeasures(componentKey);
    assertThat(measures.get("duplicated_blocks").intValue()).isEqualTo(duplicatedBlocks);
    assertThat(measures.get("duplicated_lines").intValue()).isEqualTo(duplicatedLines);
    assertThat(measures.get("duplicated_files").intValue()).isEqualTo(duplicatedFiles);
    assertThat(measures.get("duplicated_lines_density")).isEqualTo(duplicatedLinesDensity);
  }

  private static void analyzeProject(String projectKey, String... additionalProperties) {
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "xoo", "xoo-duplication-profile");

    runProjectAnalysis(orchestrator, "duplications/file-duplications",
      ObjectArrays.concat(
        new String[] {
          "sonar.projectKey", projectKey,
          "sonar.projectName", projectKey
        },
        additionalProperties, String.class));
  }

  private static void verifyWsResultOnDuplicateFile(String fileKey, String ws, String expectedFilePath) throws Exception {
    String duplication = orchestrator.getServer().adminWsClient().get(ws, "key", fileKey);
    assertEquals(IOUtils.toString(CrossProjectDuplicationsTest.class.getResourceAsStream("/duplication/DuplicationsTest/" + expectedFilePath), "UTF-8"), duplication,
      false);
  }

  @Test
  public void duplicated_lines_within_same_file() {
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_within_same_file/DuplicatedLinesInSameFile.xoo",
      2,
      30 * 2, // 2 blocks with 30 lines
      1,
      84.5);
  }

  @Test
  public void duplicated_same_lines_within_3_classes() {
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_same_lines_within_3_files/File1.xoo", 1, 33, 1, 78.6);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_same_lines_within_3_files/File2.xoo", 1, 31, 1, 75.6);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_same_lines_within_3_files/File3.xoo", 1, 31, 1, 70.5);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_same_lines_within_3_files", 3, 95, 3, 74.8);
  }

  @Test
  public void duplicated_lines_within_directory() {
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_within_dir/DuplicatedLinesInSameDirectory1.xoo", 1, 30, 1, 28.3);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_within_dir/DuplicatedLinesInSameDirectory2.xoo", 1, 30, 1, 41.7);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_within_dir", 2, 60, 2, 33.7);
  }

  @Test
  public void duplicated_lines_with_other_directory() {
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_with_other_dir1/DuplicatedLinesWithOtherDirectory.xoo", 1, 39, 1, 92.9);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_with_other_dir1", 1, 39, 1, 92.9);

    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_with_other_dir2/DuplicatedLinesWithOtherDirectory.xoo", 1, 39, 1, 92.9);
    verifyDuplicationMeasures(DUPLICATIONS + ":src/main/xoo/duplicated_lines_with_other_dir2", 1, 39, 1, 92.9);
  }

  @Test
  public void duplication_measures_on_project() {
    verifyDuplicationMeasures(DUPLICATIONS, 9, 293, 8, 63.7);
  }

  @Test
  public void project_without_enough_tokens_has_duplication() {
    verifyDuplicationMeasures(WITHOUT_ENOUGH_TOKENS, 0, 0, 0, 0d);
  }

  /**
   * SONAR-3108
   */
  @Test
  public void use_duplication_exclusions() {
    verifyDuplicationMeasures(DUPLICATIONS_WITH_EXCLUSIONS, 6, 198, 5, 43d);
  }

  @Test
  public void issues_on_duplicated_blocks_are_generated_on_each_file() {
    assertThat(issueRule.search(new SearchRequest().setRules(singletonList("common-xoo:DuplicatedBlocks"))).getIssuesList()).hasSize(13);
  }

  @Test
  public void verify_sources_lines_ws_duplication_information() throws Exception {
    verifyWsResultOnDuplicateFile(DUPLICATIONS + ":src/main/xoo/duplicated_lines_within_same_file/DuplicatedLinesInSameFile.xoo",
      "api/sources/lines", "sources_lines_duplication-expected.json");
  }

  @Test
  public void verify_duplications_show_ws() throws Exception {
    verifyWsResultOnDuplicateFile(DUPLICATIONS + ":src/main/xoo/duplicated_lines_within_same_file/DuplicatedLinesInSameFile.xoo",
      "api/duplications/show", "duplications_show-expected.json");
  }

  // SONAR-9441
  @Test
  public void fail_properly_when_no_parameter() {
    WsResponse result = tester.wsClient().wsConnector().call(new GetRequest("api/duplications/show"));

    assertThat(result.code()).isEqualTo(HTTP_BAD_REQUEST);
    assertThat(result.content()).contains("Either 'uuid' or 'key' must be provided");
  }

}
