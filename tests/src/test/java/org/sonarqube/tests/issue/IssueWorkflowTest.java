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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.IssuesService;
import org.sonarqube.ws.client.issues.DoTransitionRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;
import util.issue.IssueRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.toDatetime;

public class IssueWorkflowTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR);
  
  @ClassRule
  public static final IssueRule issueRule = IssueRule.from(ORCHESTRATOR);

  private ProjectAnalysis analysisWithIssues;
  private ProjectAnalysis analysisWithoutIssues;
  private IssuesService issuesService;

  private Issue issue;

  @Before
  public void before() {
    issuesService = tester.wsClient().issues();
    String oneIssuePerFileProfileKey = projectAnalysisRule.registerProfile("/issue/IssueWorkflowTest/xoo-one-issue-per-line-profile.xml");
    String analyzedProjectKey = projectAnalysisRule.registerProject("issue/workflow");
    analysisWithIssues = projectAnalysisRule.newProjectAnalysis(analyzedProjectKey).withQualityProfile(oneIssuePerFileProfileKey);
    analysisWithoutIssues = analysisWithIssues.withXooEmptyProfile();
    analysisWithIssues.run();

    issue = issueRule.getRandomIssue();
  }

  /**
   * Issue on a disabled rule (uninstalled plugin or rule deactivated from quality profile) must 
   * be CLOSED with resolution REMOVED
   */
  @Test
  public void issue_is_closed_as_removed_when_rule_is_disabled() {
    SearchRequest ruleSearchRequest = new SearchRequest().setRules(singletonList("xoo:OneIssuePerLine"));
    List<Issue> issues = issueRule.search(ruleSearchRequest).getIssuesList();
    assertThat(issues).isNotEmpty();

    // re-analyze with profile "empty". The rule is disabled so the issues must be closed
    analysisWithoutIssues.run();
    issues = issueRule.search(ruleSearchRequest).getIssuesList();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.getStatus()).isEqualTo("CLOSED");
      assertThat(issue.getResolution()).isEqualTo("REMOVED");
    }
  }

  /**
   * SONAR-4329
   */
  @Test
  public void user_should_confirm_issue() {
    // mark as confirmed
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("confirm"));

    Issue confirmed = issueRule.getByKey(issue.getKey());
    assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
    assertThat(confirmed.hasResolution()).isFalse();
    assertThat(confirmed.getCreationDate()).isEqualTo(issue.getCreationDate());

    // user unconfirm the issue
    assertThat(transitions(confirmed.getKey())).contains("unconfirm");
    issuesService.doTransition(new DoTransitionRequest().setIssue(confirmed.getKey()).setTransition("unconfirm"));

    Issue unconfirmed = issueRule.getByKey(issue.getKey());
    assertThat(unconfirmed.getStatus()).isEqualTo("REOPENED");
    assertThat(unconfirmed.hasResolution()).isFalse();
    assertThat(unconfirmed.getCreationDate()).isEqualTo(confirmed.getCreationDate());
  }

  /**
   * SONAR-4329
   */
  @Test
  public void user_should_mark_as_false_positive_confirmed_issue() {
    // mark as confirmed
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("confirm"));

    Issue confirmed = issueRule.getByKey(issue.getKey());
    assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
    assertThat(confirmed.hasResolution()).isFalse();
    assertThat(confirmed.getCreationDate()).isEqualTo(issue.getCreationDate());

    // user mark the issue as false-positive
    assertThat(transitions(confirmed.getKey())).contains("falsepositive");
    issuesService.doTransition(new DoTransitionRequest().setIssue(confirmed.getKey()).setTransition("falsepositive"));

    Issue falsePositive = issueRule.getByKey(issue.getKey());
    assertThat(falsePositive.getStatus()).isEqualTo("RESOLVED");
    assertThat(falsePositive.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.getCreationDate()).isEqualTo(confirmed.getCreationDate());
  }

  /**
   * SONAR-4329
   */
  @Test
  public void scan_should_close_no_more_existing_confirmed() {
    // mark as confirmed
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("confirm"));
    Issue falsePositive = issueRule.getByKey(issue.getKey());
    assertThat(falsePositive.getStatus()).isEqualTo("CONFIRMED");
    assertThat(falsePositive.hasResolution()).isFalse();
    assertThat(falsePositive.getCreationDate()).isEqualTo(issue.getCreationDate());

    // scan without any rules -> confirmed is closed
    analysisWithoutIssues.run();
    Issue closed = issueRule.getByKey(issue.getKey());
    assertThat(closed.getStatus()).isEqualTo("CLOSED");
    assertThat(closed.getResolution()).isEqualTo("REMOVED");
    assertThat(closed.getCreationDate()).isEqualTo(issue.getCreationDate());
  }

  /**
   * SONAR-4288
   */
  @Test
  public void scan_should_reopen_unresolved_issue_but_marked_as_resolved() {
    // mark as resolved
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("resolve"));
    Issue resolvedIssue = issueRule.getByKey(issue.getKey());
    assertThat(resolvedIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(resolvedIssue.getResolution()).isEqualTo("FIXED");
    assertThat(resolvedIssue.getCreationDate()).isEqualTo(issue.getCreationDate());

    // re-execute scan, with the same Q profile -> the issue has not been fixed
    analysisWithIssues.run();

    // reload issue
    Issue reopenedIssue = issueRule.getByKey(issue.getKey());

    // the issue has been reopened
    assertThat(reopenedIssue.getStatus()).isEqualTo("REOPENED");
    assertThat(reopenedIssue.hasResolution()).isFalse();
    assertThat(reopenedIssue.getCreationDate()).isEqualTo(issue.getCreationDate());
    assertThat(toDatetime(reopenedIssue.getUpdateDate())).isAfter(toDatetime(issue.getUpdateDate()));
  }

  /**
   * SONAR-4288
   */
  @Test
  public void scan_should_close_resolved_issue() {
    // mark as resolved
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("resolve"));
    Issue resolvedIssue = issueRule.getByKey(issue.getKey());
    assertThat(resolvedIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(resolvedIssue.getResolution()).isEqualTo("FIXED");
    assertThat(resolvedIssue.getCreationDate()).isEqualTo(issue.getCreationDate());
    assertThat(resolvedIssue.hasCloseDate()).isFalse();

    // re-execute scan without rules -> the issue is removed with resolution "REMOVED"
    analysisWithoutIssues.run();

    // reload issue
    Issue closedIssue = issueRule.getByKey(issue.getKey());
    assertThat(closedIssue.getStatus()).isEqualTo("CLOSED");
    assertThat(closedIssue.getResolution()).isEqualTo("REMOVED");
    assertThat(closedIssue.getCreationDate()).isEqualTo(issue.getCreationDate());
    assertThat(toDatetime(closedIssue.getUpdateDate())).isAfter(toDatetime(resolvedIssue.getUpdateDate()));
    assertThat(closedIssue.hasCloseDate()).isTrue();
    assertThat(toDatetime(closedIssue.getCloseDate())).isAfter(toDatetime(closedIssue.getCreationDate()));
  }

  /**
   * SONAR-4288
   */
  @Test
  public void user_should_reopen_issue_marked_as_resolved() {
    // user marks issue as resolved
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("resolve"));
    Issue resolved = issueRule.getByKey(issue.getKey());
    assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
    assertThat(resolved.getResolution()).isEqualTo("FIXED");
    assertThat(resolved.getCreationDate()).isEqualTo(issue.getCreationDate());

    // user reopens the issue
    assertThat(transitions(resolved.getKey())).contains("reopen");
    adminIssueClient().doTransition(resolved.getKey(), "reopen");

    Issue reopened = issueRule.getByKey(resolved.getKey());
    assertThat(reopened.getStatus()).isEqualTo("REOPENED");
    assertThat(reopened.hasResolution()).isFalse();
    assertThat(reopened.getCreationDate()).isEqualTo(resolved.getCreationDate());
  }

  /**
   * SONAR-4286
   */
  @Test
  public void scan_should_not_reopen_or_close_false_positives() {
    // user marks issue as false-positive
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("falsepositive"));

    Issue falsePositive = issueRule.getByKey(issue.getKey());
    assertThat(falsePositive.getStatus()).isEqualTo("RESOLVED");
    assertThat(falsePositive.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.getCreationDate()).isEqualTo(issue.getCreationDate());

    // re-execute the same scan
    analysisWithIssues.run();

    // refresh
    Issue reloaded = issueRule.getByKey(falsePositive.getKey());
    assertThat(reloaded.getStatus()).isEqualTo("RESOLVED");
    assertThat(reloaded.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(reloaded.getCreationDate()).isEqualTo(issue.getCreationDate());
    assertThat(toDatetime(reloaded.getUpdateDate())).isEqualTo(toDatetime(falsePositive.getUpdateDate()));
  }

  /**
   * SONAR-4286
   */
  @Test
  public void scan_should_close_no_more_existing_false_positive() {
    // user marks as false-positive
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("falsepositive"));
    Issue falsePositive = issueRule.getByKey(issue.getKey());
    assertThat(falsePositive.getStatus()).isEqualTo("RESOLVED");
    assertThat(falsePositive.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.getCreationDate()).isEqualTo(issue.getCreationDate());

    // scan without any rules -> false-positive is closed
    analysisWithoutIssues.run();
    Issue closed = issueRule.getByKey(issue.getKey());
    assertThat(closed.getStatus()).isEqualTo("CLOSED");
    assertThat(closed.getResolution()).isEqualTo("REMOVED");
    assertThat(closed.getCreationDate()).isEqualTo(issue.getCreationDate());
  }

  /**
   * SONAR-4286
   */
  @Test
  public void user_should_reopen_false_positive() {
    // user marks as false-positive
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("falsepositive"));

    Issue falsePositive = issueRule.getByKey(issue.getKey());
    assertThat(falsePositive.getStatus()).isEqualTo("RESOLVED");
    assertThat(falsePositive.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(falsePositive.getCreationDate()).isEqualTo(issue.getCreationDate());

    // user reopens the issue
    assertThat(transitions(falsePositive.getKey())).contains("reopen");
    adminIssueClient().doTransition(falsePositive.getKey(), "reopen");

    Issue reopened = issueRule.getByKey(issue.getKey());
    assertThat(reopened.getStatus()).isEqualTo("REOPENED");
    assertThat(reopened.hasResolution()).isFalse();
    assertThat(reopened.getCreationDate()).isEqualTo(falsePositive.getCreationDate());
  }

  @Test
  public void user_should_not_reopen_closed_issue() {
    issuesService.doTransition(new DoTransitionRequest().setIssue(issue.getKey()).setTransition("resolve"));

    // re-execute scan without rules -> the issue is closed
    analysisWithoutIssues.run();

    // user try to reopen the issue
    assertThat(transitions(issue.getKey())).isEmpty();
  }

  private List<String> transitions(String issueKey) {
    Issues.SearchWsResponse response = searchIssues(new SearchRequest().setIssues(singletonList(issueKey)).setAdditionalFields(singletonList("transitions")));
    assertThat(response.getTotal()).isEqualTo(1);
    return response.getIssues(0).getTransitions().getTransitionsList();
  }

  private Issues.SearchWsResponse searchIssues(SearchRequest request) {
    return tester.wsClient().issues().search(request);
  }

}
