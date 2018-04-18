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

import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
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

public class ExternalIssueTest extends AbstractIssueTest {
  private static final String PROJECT_KEY = "project";

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
    noExternalRuleAndNoIssues();

    SonarScanner sonarScanner = ItUtils.runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.oneExternalIssuePerLine.activate", "true");
    List<Issue> issuesList = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    assertThat(issuesList).hasSize(17);

    assertThat(issuesList).allMatch(issue -> "external_xoo:OneExternalIssuePerLine".equals(issue.getRule()));
    assertThat(issuesList).allMatch(issue -> "This issue is generated on each line".equals(issue.getMessage()));
    assertThat(issuesList).allMatch(issue -> "This issue is generated on each line".equals(issue.getMessage()));
    assertThat(issuesList).allMatch(issue -> Severity.MAJOR.equals(issue.getSeverity()));
    assertThat(issuesList).allMatch(issue -> RuleType.CODE_SMELL.equals(issue.getType()));
    assertThat(issuesList).allMatch(issue -> "sample:src/main/xoo/sample/Sample.xoo".equals(issue.getComponent()));
    assertThat(issuesList).allMatch(issue -> "OPEN".equals(issue.getStatus()));
    assertThat(issuesList).allMatch(issue -> issue.getExternalRuleEngine().equals("xoo"));

    List<org.sonarqube.ws.Rules.Rule> rulesList = tester.wsClient().rules()
      .search(new org.sonarqube.ws.client.rules.SearchRequest().setIsExternal(Boolean.toString(true))).getRulesList();
    List<org.sonarqube.ws.Rules.Rule> externalRules = rulesList.stream().filter(rule -> rule.getIsExternal()).collect(Collectors.toList());

    assertThat(externalRules).hasSize(1);
    assertThat(externalRules.get(0).getKey()).isEqualTo("external_xoo:OneExternalIssuePerLine");
    assertThat(externalRules.get(0).getIsTemplate()).isFalse();
    assertThat(externalRules.get(0).getIsExternal()).isTrue();
    assertThat(externalRules.get(0).getTags().getTagsCount()).isEqualTo(0);
    assertThat(externalRules.get(0).getScope()).isEqualTo(RuleScope.ALL);

    // second analysis, issue tracking should work
    sonarScanner = ItUtils.runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample",
      "sonar.oneExternalIssuePerLine.activate", "true");
    issuesList = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    assertThat(issuesList).hasSize(17);
  }

  private void noExternalRuleAndNoIssues() {
    List<org.sonarqube.ws.Rules.Rule> rulesList = tester.wsClient().rules()
      .search(new org.sonarqube.ws.client.rules.SearchRequest().setIsExternal(Boolean.toString(true))).getRulesList();
    assertThat(rulesList).noneMatch(rule -> rule.getIsExternal());

    List<Issue> issuesList = tester.wsClient().issues().search(new SearchRequest()).getIssuesList();
    assertThat(issuesList).isEmpty();
  }

}
