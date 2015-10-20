/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package issue.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueChange;
import org.sonar.wsclient.issue.IssueChangeDiff;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static issue.suite.IssueTestSuite.ORCHESTRATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class IssueChangelogTest {

  @ClassRule
  public static Orchestrator orchestrator = ORCHESTRATOR;

  Issue issue;
  SonarRunner scan;

  @Before
  public void resetData() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/IssueChangelogTest/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    scan = SonarRunner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(scan);
    issue = searchRandomIssue();
  }

  @Test
  public void update_changelog_when_assigning_issue_by_user() throws Exception {
    assertIssueHasNoChange(issue.key());

    adminIssueClient().assign(issue.key(), "admin");

    List<IssueChange> changes = retrieveChangeForIssue(issue.key());
    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);
    assertThat(change.user()).isEqualTo("admin");
    assertThat(change.creationDate()).isNotNull();
    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("assignee");
    assertThat(changeDiff.oldValue()).isNull();
    assertThat(changeDiff.newValue()).isEqualTo("Administrator");
  }

  @Test
  public void update_changelog_when_reopening_unresolved_issue_by_scan() throws Exception {
    assertIssueHasNoChange(issue.key());

    // re analyse the project after resolving an issue in order to reopen it
    adminIssueClient().doTransition(issue.key(), "resolve");
    orchestrator.executeBuild(scan);

    List<IssueChange> changes = retrieveChangeForIssue(issue.key());
    assertThat(changes).hasSize(2);

    // Change done by the user (first change is be the oldest one)
    IssueChange change1 = changes.get(0);
    assertThat(change1.user()).isEqualTo("admin");
    assertThat(change1.creationDate()).isNotNull();
    assertThat(change1.diffs()).hasSize(2);

    IssueChangeDiff change1Diff1 = change1.diffs().get(0);
    assertThat(change1Diff1.key()).isEqualTo("resolution");
    assertThat(change1Diff1.oldValue()).isNull();
    assertThat(change1Diff1.newValue()).isEqualTo("FIXED");

    IssueChangeDiff change1Diff2 = change1.diffs().get(1);
    assertThat(change1Diff2.key()).isEqualTo("status");
    assertThat(change1Diff2.oldValue()).isEqualTo("OPEN");
    assertThat(change1Diff2.newValue()).isEqualTo("RESOLVED");

    // Change done by scan
    IssueChange change2 = changes.get(1);
    assertThat(change2.user()).isNull();
    assertThat(change2.creationDate()).isNotNull();
    assertThat(change2.diffs()).hasSize(2);

    IssueChangeDiff changeDiff1 = change2.diffs().get(0);
    assertThat(changeDiff1.key()).isEqualTo("resolution");
    assertThat(changeDiff1.oldValue()).isNull();
    assertThat(changeDiff1.newValue()).isNull();

    IssueChangeDiff changeDiff2 = change2.diffs().get(1);
    assertThat(changeDiff2.key()).isEqualTo("status");
    assertThat(changeDiff2.oldValue()).isEqualTo("RESOLVED");
    assertThat(changeDiff2.newValue()).isEqualTo("REOPENED");
  }

  private static Issue searchRandomIssue() {
    List<Issue> issues = issueClient().find(IssueQuery.create()).list();
    assertThat(issues).isNotEmpty();
    return issues.get(0);
  }

  private void assertIssueHasNoChange(String issueKey) {
    assertThat(retrieveChangeForIssue(issueKey)).isEmpty();
  }

  private List<IssueChange> retrieveChangeForIssue(String issueKey) {
    return issueClient().changes(issueKey);
  }

  private static IssueClient issueClient() {
    return orchestrator.getServer().wsClient().issueClient();
  }

  private static IssueClient adminIssueClient() {
    return orchestrator.getServer().adminWsClient().issueClient();
  }
}
