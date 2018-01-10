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
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.ChangelogWsResponse.Changelog;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.ChangelogRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;

public class IssueChangelogTest extends AbstractIssueTest {

  private static WsClient adminClient;

  @Before
  public void prepareData() {
    ORCHESTRATOR.resetData();
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueChangelogTest/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject("sample", "sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    adminClient = newAdminWsClient(ORCHESTRATOR);
  }

  @Test
  public void update_changelog_when_assigning_issue_by_user() {
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");
    Issue issue = searchRandomIssue();
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
  public void update_changelog_when_reopening_unresolved_issue_by_scan() {
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");
    Issue issue = searchRandomIssue();
    assertIssueHasNoChange(issue.key());

    // re analyse the project after resolving an issue in order to reopen it
    adminIssueClient().doTransition(issue.key(), "resolve");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

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

  @Test
  public void display_file_name_in_changelog_during_file_move() {
    // version 1
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1");

    // version 2
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v3");

    Issue issue = searchRandomIssue();
    List<Changelog> changes = changelog(issue.key()).getChangelogList();
    assertThat(changes).hasSize(1);
    Changelog change = changes.get(0);
    assertThat(change.hasUser()).isFalse();
    assertThat(change.getCreationDate()).isNotNull();
    assertThat(change.getDiffsList())
      .extracting(Changelog.Diff::getKey, Changelog.Diff::getOldValue, Changelog.Diff::getNewValue)
      .containsOnly(tuple("file", "src/main/xoo/sample/Sample.xoo", "src/main/xoo/sample/Sample2.xoo"));
  }

  private void assertIssueHasNoChange(String issueKey) {
    assertThat(changelog(issueKey).getChangelogList()).isEmpty();
  }

  private static Issues.ChangelogWsResponse changelog(String issueKey) {
    return adminClient.issues().changelog(new ChangelogRequest().setIssue(issueKey));
  }

}
