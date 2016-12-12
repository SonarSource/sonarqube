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
package it.issue;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.ChangelogWsResponse.Changelog;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.newAdminWsClient;

public class IssueChangelogTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  Issue issue;
  ProjectAnalysis xooSampleAnalysis;

  @Before
  public void resetData() {
    xooSampleAnalysis = projectAnalysisRule
      .newProjectAnalysis(projectAnalysisRule.registerProject("shared/xoo-sample"))
      .withQualityProfile(projectAnalysisRule.registerProfile("/issue/IssueChangelogTest/one-issue-per-line-profile.xml"));
    xooSampleAnalysis.run();
    issue = searchRandomIssue();
  }

  @Test
  public void update_changelog_when_assigning_issue_by_user() throws Exception {
    assertIssueHasNoChange(issue.key());

    adminIssueClient().assign(issue.key(), "admin");

    List<Changelog> changes = changelog(issue.key()).getChangelogList();
    assertThat(changes).hasSize(1);
    Changelog change = changes.get(0);
    assertThat(change.getUser()).isEqualTo("admin");
    assertThat(change.getCreationDate()).isNotNull();
    assertThat(change.getDiffsList())
      .extracting(Changelog.Diff::getKey, Changelog.Diff::hasOldValue, Changelog.Diff::getNewValue)
      .containsOnly(tuple("assignee", false, "Administrator"));
  }

  @Test
  public void update_changelog_when_reopening_unresolved_issue_by_scan() throws Exception {
    assertIssueHasNoChange(issue.key());

    // re analyse the project after resolving an issue in order to reopen it
    adminIssueClient().doTransition(issue.key(), "resolve");
    xooSampleAnalysis.run();

    List<Changelog> changes = changelog(issue.key()).getChangelogList();
    assertThat(changes).hasSize(2);

    // Change done by the user (first change is be the oldest one)
    Changelog change1 = changes.get(0);
    assertThat(change1.getUser()).isEqualTo("admin");
    assertThat(change1.getCreationDate()).isNotNull();
    assertThat(change1.getDiffsList())
      .extracting(Changelog.Diff::getKey, Changelog.Diff::getOldValue, Changelog.Diff::getNewValue)
      .containsOnly(tuple("resolution", "", "FIXED"), tuple("status", "OPEN", "RESOLVED"));

    // Change done by scan
    Changelog change2 = changes.get(1);
    assertThat(change2.hasUser()).isFalse();
    assertThat(change2.getCreationDate()).isNotNull();
    assertThat(change2.getDiffsList())
      .extracting(Changelog.Diff::getKey, Changelog.Diff::getOldValue, Changelog.Diff::getNewValue)
      .containsOnly(tuple("resolution", "", ""), tuple("status", "RESOLVED", "REOPENED"));
  }

  private void assertIssueHasNoChange(String issueKey) {
    assertThat(changelog(issueKey).getChangelogList()).isEmpty();
  }

  private static Issues.ChangelogWsResponse changelog(String issueKey) {
    return newAdminWsClient(ORCHESTRATOR).issues().changelog(issueKey);
  }

}
