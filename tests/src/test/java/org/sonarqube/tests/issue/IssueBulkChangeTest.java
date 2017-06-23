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

import com.google.common.collect.FluentIterable;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.base.HttpException;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.BulkChangeWsResponse;
import org.sonarqube.ws.client.issue.BulkChangeRequest;
import org.sonarqube.ws.client.issue.IssuesService;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;
import util.issue.IssueRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.Common.Severity.BLOCKER;
import static org.sonarqube.ws.Issues.Issue;
import static util.ItUtils.newAdminWsClient;

/**
 * SONAR-4421
 */
public class IssueBulkChangeTest extends AbstractIssueTest {

  private static final int BULK_EDITED_ISSUE_COUNT = 3;
  private static final String COMMENT_AS_MARKDOWN = "this is my *comment*";
  private static final String COMMENT_AS_HTML = "this is my <strong>comment</strong>";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @ClassRule
  public static IssueRule issueRule = IssueRule.from(ORCHESTRATOR);

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  private IssuesService issuesService;
  private ProjectAnalysis xooSampleLittleIssuesAnalysis;

  @Before
  public void setUp() throws Exception {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueBulkChangeTest/one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");
    this.xooSampleLittleIssuesAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey)
      .withQualityProfile(qualityProfileKey);
    this.issuesService = newAdminWsClient(ORCHESTRATOR).issues();
  }

  @Test
  public void should_change_severity() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChangeWsResponse bulkChange = bulkChangeSeverityOfIssues(issueKeys, newSeverity);

    assertThat(bulkChange.getSuccess()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    assertIssueSeverity(issueKeys, newSeverity);
  }

  @Test
  public void should_do_transition() {
    xooSampleLittleIssuesAnalysis.run();
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChangeWsResponse bulkChangeResponse = bulkTransitionStatusOfIssues(issueKeys, "confirm");

    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    assertIssueStatus(issueKeys, "CONFIRMED");
  }

  @Test
  public void should_assign() {
    xooSampleLittleIssuesAnalysis.run();

    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChangeWsResponse bulkChangeResponse = buldChangeAssigneeOfIssues(issueKeys, "admin");

    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : issueRule.getByKeys(issueKeys)) {
      assertThat(issue.getAssignee()).isEqualTo("admin");
    }
  }

  @Test
  public void should_setSeverity_add_comment_in_single_WS_call() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    BulkChangeWsResponse bulkChangeResponse = issuesService.bulkChange(BulkChangeRequest.builder()
      .setIssues(asList(issueKeys))
      .setSetSeverity(newSeverity)
      .setComment(COMMENT_AS_MARKDOWN)
      .build());

    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : issueRule.getByKeys(issueKeys)) {
      assertThat(issue.getComments().getCommentsList()).hasSize(1);
      assertThat(issue.getComments().getComments(0).getHtmlText()).isEqualTo(COMMENT_AS_HTML);
    }
  }

  @Test
  public void should_apply_bulk_change_on_many_actions() {
    xooSampleLittleIssuesAnalysis.run();
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    BulkChangeWsResponse bulkChangeResponse = issuesService.bulkChange(BulkChangeRequest.builder()
      .setIssues(asList(issueKeys))
      .setDoTransition("confirm")
      .setAssign("admin")
      .setSetSeverity("BLOCKER")
      .setComment(COMMENT_AS_MARKDOWN)
      .build());

    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : issueRule.getByKeys(issueKeys)) {
      assertThat(issue.getStatus()).isEqualTo("CONFIRMED");
      assertThat(issue.getAssignee()).isEqualTo("admin");
      assertThat(issue.getSeverity()).isEqualTo(BLOCKER);
      assertThat(issue.getComments().getCommentsList()).hasSize(1);
      assertThat(issue.getComments().getComments(0).getHtmlText()).isEqualTo(COMMENT_AS_HTML);
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_not_logged() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    try {
      issuesService.bulkChange(createBulkChangeSeverityOfIssuesQuery(issueKeys, newSeverity));
    } catch (Exception e) {
      assertHttpException(e, 401);
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_no_change_to_do() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    // Apply the bulk change a first time
    BulkChangeWsResponse bulkChangeResponse = bulkChangeSeverityOfIssues(issueKeys, newSeverity);
    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(BULK_EDITED_ISSUE_COUNT);

    // Re apply the same bulk change -> no issue should be changed
    bulkChangeResponse = bulkChangeSeverityOfIssues(issueKeys, newSeverity);
    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(0);
    assertThat(bulkChangeResponse.getIgnored()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
  }

  @Test
  public void should_not_apply_bulk_change_if_no_action() {
    xooSampleLittleIssuesAnalysis.run();

    try {
      int limit = BULK_EDITED_ISSUE_COUNT;
      String[] issueKeys = searchIssueKeys(limit);
      issuesService.bulkChange(BulkChangeRequest.builder().setIssues(asList(issueKeys)).build());
    } catch (Exception e) {
      assertHttpException(e, 400);
    }
  }

  @Test
  public void should_add_comment_only_on_issues_that_will_be_changed() {
    xooSampleLittleIssuesAnalysis.run();
    int nbIssues = BULK_EDITED_ISSUE_COUNT;
    String[] issueKeys = searchIssueKeys(nbIssues);

    // Confirm an issue
    adminIssueClient().doTransition(searchIssues().iterator().next().key(), "confirm");

    // Apply a bulk change on unconfirm transition
    BulkChangeWsResponse bulkChangeResponse = issuesService.bulkChange(BulkChangeRequest.builder().setIssues(asList(issueKeys))
      .setDoTransition("unconfirm")
      .setComment("this is my comment")
      .build());
    assertThat(bulkChangeResponse.getSuccess()).isEqualTo(1);

    int nbIssuesWithComment = 0;
    for (Issues.Issue issue : issueRule.getByKeys(issueKeys)) {
      if (!issue.getComments().getCommentsList().isEmpty()) {
        nbIssuesWithComment++;
      }
    }
    // Only one issue should have the comment
    assertThat(nbIssuesWithComment).isEqualTo(1);
  }

  private static void assertIssueSeverity(String[] issueKeys, String expectedSeverity) {
    for (Issues.Issue issue : issueRule.getByKeys(issueKeys)) {
      assertThat(issue.getSeverity().name()).isEqualTo(expectedSeverity);
    }
  }

  private static void assertIssueStatus(String[] issueKeys, String expectedStatus) {
    for (Issues.Issue issue : issueRule.getByKeys(issueKeys)) {
      assertThat(issue.getStatus()).isEqualTo(expectedStatus);
    }
  }

  private static void assertHttpException(Exception e, int expectedCode) {
    assertThat(e).isInstanceOf(HttpException.class);
    assertThat(((HttpException) e).status()).isEqualTo(expectedCode);
  }

  private BulkChangeWsResponse bulkChangeSeverityOfIssues(String[] issueKeys, String newSeverity) {
    BulkChangeRequest bulkChangeQuery = createBulkChangeSeverityOfIssuesQuery(issueKeys, newSeverity);
    return issuesService.bulkChange(bulkChangeQuery);
  }

  private static BulkChangeRequest createBulkChangeSeverityOfIssuesQuery(String[] issueKeys, String newSeverity) {
    BulkChangeRequest.Builder request = BulkChangeRequest.builder().setSetSeverity(newSeverity);
    if (issueKeys != null && issueKeys.length > 0) {
      request.setIssues(asList(issueKeys));
    }
    return request.build();
  }

  private BulkChangeWsResponse bulkTransitionStatusOfIssues(String[] issueKeys, String newSeverity) {
    return issuesService.bulkChange(BulkChangeRequest.builder().setIssues(asList(issueKeys)).setDoTransition(newSeverity).build());
  }

  private BulkChangeWsResponse buldChangeAssigneeOfIssues(String[] issueKeys, String newAssignee) {
    return issuesService.bulkChange(BulkChangeRequest.builder().setIssues(asList(issueKeys)).setAssign(newAssignee).build());
  }

  private static String[] searchIssueKeys(int limit) {
    return getIssueKeys(issueRule.search(new SearchWsRequest()).getIssuesList(), limit);
  }

  private static String[] getIssueKeys(List<Issues.Issue> issues, int nbIssues) {
    return FluentIterable.from(issues)
      .limit(nbIssues)
      .transform(Issue::getKey)
      .toArray(String.class);
  }

}
