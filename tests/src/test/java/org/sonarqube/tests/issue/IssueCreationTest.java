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
package org.sonarqube.tests.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

public class IssueCreationTest extends AbstractIssueTest {

  private static final String SAMPLE_PROJECT_KEY = "sample";

  @Before
  public void resetData() {
    ORCHESTRATOR.resetData();
  }

  /**
   * See SONAR-4785
   */
  @Test
  public void use_rule_name_if_issue_has_no_message() {
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueCreationTest/with-custom-message.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "with-custom-message");

    // First analysis, the issue is generated with a message
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample", "sonar.customMessage.message", "a message");
    Issue issue = issueClient().find(IssueQuery.create()).list().get(0);
    assertThat(issue.message()).isEqualTo("a message");

    // Second analysis, the issue is generated without any message, the name of the rule is used
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");
    issue = issueClient().find(IssueQuery.create()).list().get(0);
    assertThat(issue.message()).isEqualTo("Issue With Custom Message");
  }

  @Test
  public void plugin_can_override_profile_severity() throws Exception {
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);

    // The rule "OneBlockerIssuePerFile" is enabled with severity "INFO"
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueCreationTest/override-profile-severity.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "override-profile-severity");

    // But it's hardcoded "blocker" when plugin generates the issue
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    Issues issues = search(IssueQuery.create().rules("xoo:OneBlockerIssuePerFile"));
    assertThat(issues.size()).isGreaterThan(0);
    for (Issue issue : issues.list()) {
      assertThat(issue.severity()).isEqualTo("BLOCKER");
    }
  }
}
