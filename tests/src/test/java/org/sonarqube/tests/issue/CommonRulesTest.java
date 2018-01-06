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
package org.sonarqube.tests.issue;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;

public class CommonRulesTest extends AbstractIssueTest {

  public static final String FILE_KEY = "common-rules-project:src/Sample.xoo";
  public static final String TEST_FILE_KEY = "common-rules-project:test/SampleTest.xoo";

  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    ItUtils.restoreProfile(ORCHESTRATOR, CommonRulesTest.class.getResource("/issue/CommonRulesTest/xoo-common-rules-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject("common-rules-project", "Sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("common-rules-project", "xoo", "xoo-common-rules");
    runProjectAnalysis(ORCHESTRATOR, "issue/common-rules",
      "sonar.cpd.xoo.minimumTokens", "2",
      "sonar.cpd.xoo.minimumLines", "2");

    adminWsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @Test
  public void test_rule_on_duplicated_blocks() {
    List<Issue> issues = findIssues(FILE_KEY, "common-xoo:DuplicatedBlocks");
    assertThat(issues).hasSize(1);
  }

  @Test
  public void test_rule_on_comments() {
    List<Issue> issues = findIssues(FILE_KEY, "common-xoo:InsufficientCommentDensity");
    assertThat(issues.size()).isEqualTo(1);
  }

  @Test
  public void test_rule_on_coverage() {
    List<Issue> issues = findIssues(FILE_KEY, "common-xoo:InsufficientBranchCoverage");
    assertThat(issues.size()).isEqualTo(1);

    issues = findIssues(FILE_KEY, "common-xoo:InsufficientLineCoverage");
    assertThat(issues.size()).isEqualTo(1);
  }

  @Test
  public void test_rule_on_skipped_tests() {
    List<Issue> issues = findIssues(TEST_FILE_KEY, "common-xoo:SkippedUnitTests");
    assertThat(issues.size()).isEqualTo(1);
  }

  @Test
  public void test_rule_on_test_errors() {
    List<Issue> issues = findIssues(TEST_FILE_KEY, "common-xoo:FailedUnitTests");
    assertThat(issues.size()).isEqualTo(1);
  }

  private List<Issue> findIssues(String componentKey, String ruleKey) {
    return adminWsClient.issues().search(
      new SearchRequest()
        .setComponentKeys(singletonList(componentKey))
        .setRules(singletonList(ruleKey)))
      .getIssuesList();
  }
}
