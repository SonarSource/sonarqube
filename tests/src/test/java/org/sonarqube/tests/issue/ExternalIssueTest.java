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

import com.sonar.orchestrator.Orchestrator;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Common.RuleScope;
import org.sonarqube.ws.Common.RuleType;
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalIssueTest {
  private static final String PROJECT_KEY = "project";

  // This class uses its own instance of the server because it creates external rules in it
  @ClassRule
  public static final Orchestrator ORCHESTRATOR = ItUtils.newOrchestratorBuilder()
    .addPlugin(ItUtils.xooPlugin())
    .build();

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR);

  @Before
  public void setUp() {
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/ExternalIssueTest/no-rules.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "no-rules");
  }

  @Test
  public void should_import_external_issues_and_create_external_rules() {
    noIssues();
    ruleDoesntExist("external_xoo:OneExternalIssuePerLine");

    ItUtils.runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.oneExternalIssuePerLine.activate", "true");
    List<Issue> issuesList = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    assertThat(issuesList).hasSize(17);

    assertThat(issuesList).allMatch(issue -> "external_xoo:OneExternalIssuePerLine".equals(issue.getRule()));
    assertThat(issuesList).allMatch(issue -> "This issue is generated on each line".equals(issue.getMessage()));
    assertThat(issuesList).allMatch(issue -> Severity.MAJOR.equals(issue.getSeverity()));
    assertThat(issuesList).allMatch(issue -> RuleType.BUG.equals(issue.getType()));
    assertThat(issuesList).allMatch(issue -> "sample:src/main/xoo/sample/Sample.xoo".equals(issue.getComponent()));
    assertThat(issuesList).allMatch(issue -> "OPEN".equals(issue.getStatus()));
    assertThat(issuesList).allMatch(issue -> issue.getExternalRuleEngine().equals("xoo"));

    ruleExists("external_xoo:OneExternalIssuePerLine");

    // second analysis, issue tracking should work
    ItUtils.runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.oneExternalIssuePerLine.activate", "true");
    issuesList = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    assertThat(issuesList).hasSize(17);
  }

  @Test
  public void should_import_external_issues_from_json_report_and_create_external_rules() {
    noIssues();
    ruleDoesntExist("external_externalXoo:rule1");
    ruleDoesntExist("external_externalXoo:rule2");

    ItUtils.runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.externalIssuesReportPaths", "externalIssues.json");

    List<Issue> issuesList = tester.wsClient().issues().search(new SearchRequest()
      .setRules(Collections.singletonList("external_externalXoo:rule1"))).getIssuesList();
    assertThat(issuesList).hasSize(1);

    assertThat(issuesList.get(0).getRule()).isEqualTo("external_externalXoo:rule1");
    assertThat(issuesList.get(0).getMessage()).isEqualTo("fix the issue here");
    assertThat(issuesList.get(0).getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(issuesList.get(0).getType()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(issuesList.get(0).getComponent()).isEqualTo("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issuesList.get(0).getStatus()).isEqualTo("OPEN");
    assertThat(issuesList.get(0).getEffort()).isEqualTo("50min");
    assertThat(issuesList.get(0).getExternalRuleEngine()).isEqualTo("externalXoo");

    issuesList = tester.wsClient().issues().search(new SearchRequest()
      .setRules(Collections.singletonList("external_externalXoo:rule2"))).getIssuesList();
    assertThat(issuesList).hasSize(1);

    assertThat(issuesList.get(0).getRule()).isEqualTo("external_externalXoo:rule2");
    assertThat(issuesList.get(0).getMessage()).isEqualTo("fix the bug here");
    assertThat(issuesList.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
    assertThat(issuesList.get(0).getType()).isEqualTo(RuleType.BUG);
    assertThat(issuesList.get(0).getComponent()).isEqualTo("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issuesList.get(0).getStatus()).isEqualTo("OPEN");
    assertThat(issuesList.get(0).getExternalRuleEngine()).isEqualTo("externalXoo");

    ruleExists("external_externalXoo:rule1");
    ruleExists("external_externalXoo:rule2");
  }

  private void ruleDoesntExist(String key) {
    List<org.sonarqube.ws.Rules.Rule> rulesList = tester.wsClient().rules()
      .search(new org.sonarqube.ws.client.rules.SearchRequest()
        .setRuleKey(key)
        .setIncludeExternal(Boolean.toString(true)))
      .getRulesList();
    assertThat(rulesList).isEmpty();

  }

  private void ruleExists(String key) {
    List<org.sonarqube.ws.Rules.Rule> rulesList = tester.wsClient().rules()
      .search(new org.sonarqube.ws.client.rules.SearchRequest()
        .setRuleKey(key)
        .setIncludeExternal(Boolean.toString(true)))
      .getRulesList();

    assertThat(rulesList).hasSize(1);
    assertThat(rulesList.get(0).getKey()).isEqualTo(key);
    assertThat(rulesList.get(0).getIsTemplate()).isFalse();
    assertThat(rulesList.get(0).getIsExternal()).isTrue();
    assertThat(rulesList.get(0).getTags().getTagsCount()).isEqualTo(0);
    assertThat(rulesList.get(0).getScope()).isEqualTo(RuleScope.ALL);
  }

  private void noIssues() {
    List<Issue> issuesList = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    assertThat(issuesList).isEmpty();
  }

}
