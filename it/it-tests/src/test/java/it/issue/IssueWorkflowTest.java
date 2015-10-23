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
package it.issue;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueWorkflowTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  private ProjectAnalysis analysisWithIssues;
  private ProjectAnalysis analysisWithoutIssues;
  private Issue issue;

  @Before
  public void before() {
    String oneIssuePerFileProfileKey = projectAnalysisRule.registerProfile("/issue/IssueWorkflowTest/xoo-one-issue-per-line-profile.xml");
    String analyzedProjectKey = projectAnalysisRule.registerProject("issue/workflow");
    analysisWithIssues = projectAnalysisRule.newProjectAnalysis(analyzedProjectKey).withQualityProfile(oneIssuePerFileProfileKey);
    analysisWithoutIssues = analysisWithIssues.withXooEmptyProfile();
    analysisWithIssues.run();

    issue = searchRandomIssue();
  }

  /**
   * Issue on a disabled rule (uninstalled plugin or rule deactivated from quality profile) must 
   * be CLOSED with resolution REMOVED
   */
  @Test
  public void issue_is_closed_as_removed_when_rule_is_disabled() throws Exception {
    List<Issue> issues = searchIssues(IssueQuery.create().rules("xoo:OneIssuePerLine"));
    assertThat(issues).isNotEmpty();

    // re-analyze with profile "empty". The rule is disabled so the issues must be closed
    analysisWithoutIssues.run();
    issues = searchIssues(IssueQuery.create().rules("xoo:OneIssuePerLine"));
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.status()).isEqualTo("CLOSED");
      assertThat(issue.resolution()).isEqualTo("REMOVED");
    }
  }

  /**
   * SONAR-4329
   */
  @Test
  public void user_should_confirm_issue() {
    // mark as confirmed
    adminIssueClient().doTransition(issue.key(), "confirm");

    Issue confirmed = searchIssueByKey(issue.key());
    assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    assertThat(confirmed.resolution()).isNull();
    assertThat(confirmed.creationDate()).isEqualTo(issue.creationDate());

    // user unconfirm the issue
    assertThat(adminIssueClient().transitions(confirmed.key())).contains("unconfirm");
    adminIssueClient().doTransition(confirmed.key(), "unconfirm");

    Issue unconfirmed = searchIssueByKey(issue.key());
    assertThat(unconfirmed.status()).isEqualTo("REOPENED");
    assertThat(unconfirmed.resolution()).isNull();
    assertThat(unconfirmed.creationDate()).isEqualTo(confirmed.creationDate());
  }

  /**
   * SONAR-4329
   */
  @Test
  public void user_should_mark_as_false_positive_confirmed_issue() {
    // mark as confirmed
    adminIssueClient().doTransition(issue.key(), "confirm");

    Issue confirmed = searchIssueByKey(issue.key());
    assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    assertThat(confirmed.resolution()).isNull();
    assertThat(confirmed.creationDate()).isEqualTo(issue.creationDate());

    // user mark the issue as false-positive
    assertThat(adminIssueClient().transitions(confirmed.key())).contains("falsepositive");
    adminIssueClient().doTransition(confirmed.key(), "falsepositive");

    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(confirmed.creationDate());
  }

  /**
   * SONAR-4329
   */
  @Test
  public void scan_should_close_no_more_existing_confirmed() {
    // mark as confirmed
    adminIssueClient().doTransition(issue.key(), "confirm");
    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("CONFIRMED");
    assertThat(falsePositive.resolution()).isNull();
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // scan without any rules -> confirmed is closed
    analysisWithoutIssues.run();
    Issue closed = searchIssueByKey(issue.key());
    assertThat(closed.status()).isEqualTo("CLOSED");
    assertThat(closed.resolution()).isEqualTo("REMOVED");
    assertThat(closed.creationDate()).isEqualTo(issue.creationDate());
  }

  /**
   * SONAR-4288
   */
  @Test
  public void scan_should_reopen_unresolved_issue_but_marked_as_resolved() {
    // mark as resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Issue resolvedIssue = searchIssueByKey(issue.key());
    assertThat(resolvedIssue.status()).isEqualTo("RESOLVED");
    assertThat(resolvedIssue.resolution()).isEqualTo("FIXED");
    assertThat(resolvedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(resolvedIssue.updateDate().before(resolvedIssue.creationDate())).isFalse();
    assertThat(resolvedIssue.updateDate().before(issue.updateDate())).isFalse();

    // re-execute scan, with the same Q profile -> the issue has not been fixed
    analysisWithIssues.run();

    // reload issue
    Issue reopenedIssue = searchIssueByKey(issue.key());

    // the issue has been reopened
    assertThat(reopenedIssue.status()).isEqualTo("REOPENED");
    assertThat(reopenedIssue.resolution()).isNull();
    assertThat(reopenedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reopenedIssue.updateDate().before(issue.updateDate())).isFalse();
  }

  /**
   * SONAR-4288
   */
  @Test
  public void scan_should_close_resolved_issue() {
    // mark as resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Issue resolvedIssue = searchIssueByKey(issue.key());
    assertThat(resolvedIssue.status()).isEqualTo("RESOLVED");
    assertThat(resolvedIssue.resolution()).isEqualTo("FIXED");
    assertThat(resolvedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(resolvedIssue.closeDate()).isNull();

    // re-execute scan without rules -> the issue is removed with resolution "REMOVED"
    analysisWithoutIssues.run();

    // reload issue
    Issue closedIssue = searchIssueByKey(issue.key());
    assertThat(closedIssue.status()).isEqualTo("CLOSED");
    assertThat(closedIssue.resolution()).isEqualTo("REMOVED");
    assertThat(closedIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(closedIssue.updateDate().before(resolvedIssue.updateDate())).isFalse();
    assertThat(closedIssue.closeDate()).isNotNull();
    assertThat(closedIssue.closeDate().before(closedIssue.creationDate())).isFalse();
  }

  /**
   * SONAR-4288
   */
  @Test
  public void user_should_reopen_issue_marked_as_resolved() {
    // user marks issue as resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Issue resolved = searchIssueByKey(issue.key());
    assertThat(resolved.status()).isEqualTo("RESOLVED");
    assertThat(resolved.resolution()).isEqualTo("FIXED");
    assertThat(resolved.creationDate()).isEqualTo(issue.creationDate());

    // user reopens the issue
    assertThat(adminIssueClient().transitions(resolved.key())).contains("reopen");
    adminIssueClient().doTransition(resolved.key(), "reopen");

    Issue reopened = searchIssueByKey(resolved.key());
    assertThat(reopened.status()).isEqualTo("REOPENED");
    assertThat(reopened.resolution()).isNull();
    assertThat(reopened.creationDate()).isEqualTo(resolved.creationDate());
    assertThat(reopened.updateDate().before(resolved.updateDate())).isFalse();
  }

  /**
   * SONAR-4286
   */
  @Test
  public void scan_should_not_reopen_or_close_false_positives() {
    // user marks issue as false-positive
    adminIssueClient().doTransition(issue.key(), "falsepositive");

    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // re-execute the same scan
    analysisWithIssues.run();

    // refresh
    Issue reloaded = searchIssueByKey(falsePositive.key());
    assertThat(reloaded.status()).isEqualTo("RESOLVED");
    assertThat(reloaded.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    // TODO check that update date has not been changed
  }

  /**
   * SONAR-4286
   */
  @Test
  public void scan_should_close_no_more_existing_false_positive() {
    // user marks as false-positive
    adminIssueClient().doTransition(issue.key(), "falsepositive");
    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // scan without any rules -> false-positive is closed
    analysisWithoutIssues.run();
    Issue closed = searchIssueByKey(issue.key());
    assertThat(closed.status()).isEqualTo("CLOSED");
    assertThat(closed.resolution()).isEqualTo("REMOVED");
    assertThat(closed.creationDate()).isEqualTo(issue.creationDate());
  }

  /**
   * SONAR-4286
   */
  @Test
  public void user_should_reopen_false_positive() {
    // user marks as false-positive
    adminIssueClient().doTransition(issue.key(), "falsepositive");

    Issue falsePositive = searchIssueByKey(issue.key());
    assertThat(falsePositive.status()).isEqualTo("RESOLVED");
    assertThat(falsePositive.resolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.creationDate()).isEqualTo(issue.creationDate());

    // user reopens the issue
    assertThat(adminIssueClient().transitions(falsePositive.key())).contains("reopen");
    adminIssueClient().doTransition(falsePositive.key(), "reopen");

    Issue reopened = searchIssueByKey(issue.key());
    assertThat(reopened.status()).isEqualTo("REOPENED");
    assertThat(reopened.resolution()).isNull();
    assertThat(reopened.creationDate()).isEqualTo(falsePositive.creationDate());
  }

  @Test
  public void user_should_not_reopen_closed_issue() {
    adminIssueClient().doTransition(issue.key(), "resolve");

    // re-execute scan without rules -> the issue is closed
    analysisWithoutIssues.run();

    // user try to reopen the issue
    assertThat(adminIssueClient().transitions(issue.key())).isEmpty();
  }

}
