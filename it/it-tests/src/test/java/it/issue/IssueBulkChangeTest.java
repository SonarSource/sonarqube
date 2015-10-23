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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.BulkChange;
import org.sonar.wsclient.issue.BulkChangeQuery;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.NewActionPlan;
import util.ItUtils;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SONAR-4421
 */
public class IssueBulkChangeTest extends AbstractIssueTest {

  private static final int BULK_EDITED_ISSUE_COUNT = 3;
  private static final String COMMENT_AS_MARKDOWN = "this is my *comment*";
  private static final String COMMENT_AS_HTML = "this is my <em>comment</em>";

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  private ProjectAnalysis xooSampleLittleIssuesAnalysis;

  @Before
  public void setUp() throws Exception {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueBulkChangeTest/one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");
    this.xooSampleLittleIssuesAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey)
      .withQualityProfile(qualityProfileKey);
  }

  @Test
  public void should_change_severity() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChange bulkChange = bulkChangeSeverityOfIssues(issueKeys, newSeverity);

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    assertIssueSeverity(issueKeys, newSeverity);
  }

  @Test
  public void should_do_transition() {
    xooSampleLittleIssuesAnalysis.run();
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChange bulkChange = bulkTransitionStatusOfIssues(issueKeys, "confirm");

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    assertIssueStatus(issueKeys, "CONFIRMED");
  }

  @Test
  public void should_assign() {
    xooSampleLittleIssuesAnalysis.run();

    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChange bulkChange = buldChangeAssigneeOfIssues(issueKeys, "admin");

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : searchIssues(issueKeys)) {
      assertThat(issue.assignee()).isEqualTo("admin");
    }
  }

  @Test
  public void should_plan() {
    xooSampleLittleIssuesAnalysis.run();

    // Create action plan
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project("sample").description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));

    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);
    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("plan")
        .actionParameter("plan", "plan", newActionPlan.key())
      );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : searchIssues(issueKeys)) {
      assertThat(issue.actionPlan()).isEqualTo(newActionPlan.key());
    }
  }

  @Test
  public void should_setSeverity_add_comment_in_single_WS_call() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("set_severity", "comment")
        .actionParameter("set_severity", "severity", newSeverity)
        .actionParameter("comment", "comment", COMMENT_AS_MARKDOWN)
      );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : searchIssues(issueKeys, true)) {
      assertThat(issue.comments()).hasSize(1);
      assertThat(issue.comments().get(0).htmlText()).isEqualTo(COMMENT_AS_HTML);
    }
  }

  @Test
  public void should_apply_bulk_change_on_many_actions() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("do_transition", "assign", "set_severity")
        .actionParameter("do_transition", "transition", "confirm")
        .actionParameter("assign", "assignee", "admin")
        .actionParameter("set_severity", "severity", newSeverity)
        .comment(COMMENT_AS_MARKDOWN)
      );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
    for (Issue issue : searchIssues(issueKeys, true)) {
      assertThat(issue.status()).isEqualTo("CONFIRMED");
      assertThat(issue.assignee()).isEqualTo("admin");
      assertThat(issue.severity()).isEqualTo(newSeverity);
      assertThat(issue.comments()).hasSize(1);
      assertThat(issue.comments().get(0).htmlText()).isEqualTo(COMMENT_AS_HTML);
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_not_logged() {
    xooSampleLittleIssuesAnalysis.run();

    String newSeverity = "BLOCKER";
    String[] issueKeys = searchIssueKeys(BULK_EDITED_ISSUE_COUNT);

    try {
      issueClient().bulkChange(createBulkChangeSeverityOfIssuesQuery(issueKeys, newSeverity));
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
    BulkChange bulkChange = bulkChangeSeverityOfIssues(issueKeys, newSeverity);
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);

    // Re apply the same bulk change -> no issue should be changed
    bulkChange = bulkChangeSeverityOfIssues(issueKeys, newSeverity);
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(0);
    assertThat(bulkChange.totalIssuesNotChanged()).isEqualTo(BULK_EDITED_ISSUE_COUNT);
  }

  @Test
  public void should_not_apply_bulk_change_if_no_issue_selected() {
    try {
      bulkChangeSeverityOfIssues(new String[] {}, "BLOCKER");
    } catch (Exception e) {
      assertHttpException(e, 400);
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_action_is_invalid() {
    xooSampleLittleIssuesAnalysis.run();

    int limit = BULK_EDITED_ISSUE_COUNT;
    String[] issueKeys = searchIssueKeys(limit);

    BulkChangeQuery query = (BulkChangeQuery.create().issues(issueKeys).actions("invalid"));
    try {
      adminIssueClient().bulkChange(query);
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
    BulkChangeQuery query = (BulkChangeQuery.create()
      .issues(issueKeys)
      .actions("do_transition")
      .actionParameter("do_transition", "transition", "unconfirm")
      .comment("this is my comment")
      );
    BulkChange bulkChange = adminIssueClient().bulkChange(query);
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(1);

    int nbIssuesWithComment = 0;
    for (Issue issue : searchIssues(issueKeys, true)) {
      if (!issue.comments().isEmpty()) {
        nbIssuesWithComment++;
      }
    }
    // Only one issue should have the comment
    assertThat(nbIssuesWithComment).isEqualTo(1);
  }

  private static void assertIssueSeverity(String[] issueKeys, String expectedSeverity) {
    for (Issue issue : searchIssues(issueKeys)) {
      assertThat(issue.severity()).isEqualTo(expectedSeverity);
    }
  }

  private static void assertIssueStatus(String[] issueKeys, String expectedStatus) {
    for (Issue issue : searchIssues(issueKeys)) {
      assertThat(issue.status()).isEqualTo(expectedStatus);
    }
  }

  private static void assertHttpException(Exception e, int expectedCode) {
    assertThat(e).isInstanceOf(HttpException.class);
    assertThat(((HttpException) e).status()).isEqualTo(expectedCode);
  }

  private static BulkChange bulkChangeSeverityOfIssues(String[] issueKeys, String newSeverity) {
    BulkChangeQuery bulkChangeQuery = createBulkChangeSeverityOfIssuesQuery(issueKeys, newSeverity);

    return adminIssueClient().bulkChange(bulkChangeQuery);
  }

  private static BulkChangeQuery createBulkChangeSeverityOfIssuesQuery(String[] issueKeys, String newSeverity) {
    BulkChangeQuery bulkChangeQuery = BulkChangeQuery.create()
      .actions("set_severity")
      .actionParameter("set_severity", "severity", newSeverity);
    if (issueKeys != null && issueKeys.length > 0) {
      bulkChangeQuery.issues(issueKeys);
    }
    return bulkChangeQuery;
  }

  private static BulkChange bulkTransitionStatusOfIssues(String[] issueKeys, String newSeverity) {
    return adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("do_transition")
        .actionParameter("do_transition", "transition", newSeverity)
      );
  }

  private static BulkChange buldChangeAssigneeOfIssues(String[] issueKeys, String newAssignee) {
    return adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("assign")
        .actionParameter("assign", "assignee", newAssignee)
      );
  }

  private static String[] getIssueKeys(List<Issue> issues, int nbIssues) {
    return FluentIterable.from(issues)
      .limit(nbIssues)
      .transform(IssueToKey.INSTANCE)
      .toArray(String.class);
  }

  private static String[] searchIssueKeys(int limit) {
    return getIssueKeys(searchIssues(), limit);
  }

  private static ActionPlanClient adminActionPlanClient() {
    return ORCHESTRATOR.getServer().adminWsClient().actionPlanClient();
  }

  private enum IssueToKey implements Function<Issue, String> {
    INSTANCE;

    public String apply(Issue issue) {
      return issue.key();
    }
  }
}
