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
import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperties;

public class IssueFilterOnCommonRulesTest extends AbstractIssueTest {

  private static WsClient adminWsClient;

  private static final String PROJECT_KEY = "common-rules-project";
  private static final String PROJECT_DIR = "issue/common-rules";

  private static final String FILE_KEY = "common-rules-project:src/Sample.xoo";

  @Before
  public void resetData() {
    ORCHESTRATOR.resetData();
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueFilterOnCommonRulesTest/xoo-common-rules-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, "Sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "xoo-common-rules");

    adminWsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @Test
  public void ignore_all() {
    executeAnalysis("sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "*",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/*.xoo");

    assertThat(findAllIssues()).hasSize(0);
  }

  @Test
  public void ignore_some_rule_and_file() {
    executeAnalysis(
      "sonar.issue.ignore.multicriteria", "1,2",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "common-xoo:DuplicatedBlocks",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/Sample.xoo",
      "sonar.issue.ignore.multicriteria.2.ruleKey", "common-xoo:SkippedUnitTests",
      "sonar.issue.ignore.multicriteria.2.resourceKey", "**/SampleTest.xoo");

    assertThat(findAllIssues()).hasSize(4);
    assertThat(findIssuesByRuleKey("common-xoo:DuplicatedBlocks")).isEmpty();
    assertThat(findIssuesByRuleKey("common-xoo:SkippedUnitTests")).isEmpty();
  }

  @Test
  public void enforce_one_file() {
    executeAnalysis(
      "sonar.issue.enforce.multicriteria", "1",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "*",
      // Only issues on this file will be accepted
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/Sample.xoo");

    assertThat(findAllIssues()).hasSize(4);
  }

  @Test
  public void enforce_on_rules() {
    executeAnalysis(
      "sonar.issue.enforce.multicriteria", "1,2",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "common-xoo:DuplicatedBlocks",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/Sample.xoo",
      // This rule should only be applied on a file that do not exist => no issue for this rule
      "sonar.issue.enforce.multicriteria.2.ruleKey", "common-xoo:InsufficientCommentDensity",
      "sonar.issue.enforce.multicriteria.2.resourceKey", "**/OtherFile.xoo");

    assertThat(findAllIssues()).hasSize(5);
    assertThat(findIssuesByRuleKey("common-xoo:DuplicatedBlocks")).hasSize(1);
    assertThat(findIssuesByRuleKey("common-xoo:InsufficientCommentDensity")).isEmpty();
  }

  private void executeAnalysis(String... serverProperties) {
    String[] cpdProperties = new String[] {
      "sonar.cpd.xoo.minimumTokens", "2",
      "sonar.cpd.xoo.minimumLines", "2"
    };
    setServerProperties(ORCHESTRATOR, PROJECT_KEY, (String[]) ArrayUtils.addAll(serverProperties, cpdProperties));
    runProjectAnalysis(ORCHESTRATOR, PROJECT_DIR);
  }

  private List<Issues.Issue> findIssuesByRuleKey(String ruleKey) {
    return adminWsClient.issues().search(
      new SearchRequest()
        .setComponentKeys(singletonList(FILE_KEY))
        .setRules(singletonList(ruleKey)))
      .getIssuesList();
  }

  private List<Issues.Issue> findAllIssues() {
    return adminWsClient.issues().search(new SearchRequest()).getIssuesList();
  }

}
