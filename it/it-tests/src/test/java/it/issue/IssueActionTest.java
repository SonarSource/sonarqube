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
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueComment;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.issue.NewActionPlan;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.toDate;
import static util.ItUtils.verifyHttpException;

public class IssueActionTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  Issue issue;
  ProjectAnalysis projectAnalysis;

  @Before
  public void setup() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");

    this.projectAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey).withQualityProfile(qualityProfileKey);
    this.projectAnalysis.run();
    this.issue = searchRandomIssue();
  }

  @Test
  public void no_comments_by_default() throws Exception {
    assertThat(issue.comments()).isEmpty();
  }

  @Test
  public void add_comment() throws Exception {
    IssueComment comment = adminIssueClient().addComment(issue.key(), "this is my *comment*");
    assertThat(comment.key()).isNotNull();
    assertThat(comment.htmlText()).isEqualTo("this is my <em>comment</em>");
    assertThat(comment.login()).isEqualTo("admin");
    assertThat(comment.createdAt()).isNotNull();

    // reload issue
    Issue reloaded = searchIssues(issue.key(), true).iterator().next();

    assertThat(reloaded.comments()).hasSize(1);
    assertThat(reloaded.comments().get(0).key()).isEqualTo(comment.key());
    assertThat(reloaded.comments().get(0).htmlText()).isEqualTo("this is my <em>comment</em>");
    assertThat(reloaded.updateDate().before(issue.creationDate())).isFalse();
  }

  /**
   * SONAR-4450
   */
  @Test
  public void should_reject_blank_comment() throws Exception {
    try {
      adminIssueClient().addComment(issue.key(), "  ");
      fail();
    } catch (HttpException ex) {
      assertThat(ex.status()).isEqualTo(400);
    }

    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.comments()).hasSize(0);
  }

  /**
   * SONAR-4352
   */
  @Test
  public void change_severity() {
    String componentKey = "sample";

    // there are no blocker issues
    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).isEmpty();

    // increase the severity of an issue
    adminIssueClient().setSeverity(issue.key(), "BLOCKER");

    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.severity()).isEqualTo("BLOCKER");
    assertThat(reloaded.status()).isEqualTo("OPEN");
    assertThat(reloaded.resolution()).isNull();
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reloaded.creationDate().before(reloaded.updateDate())).isTrue();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void assign() {
    assertThat(issue.assignee()).isNull();
    Issues issues = search(IssueQuery.create().issues(issue.key()));
    assertThat(issues.users()).isEmpty();

    adminIssueClient().assign(issue.key(), "admin");
    Assertions.assertThat(searchIssues(IssueQuery.create().assignees("admin"))).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.assignee()).isEqualTo("admin");
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());

    issues = search(IssueQuery.create().issues(issue.key()));
    assertThat(issues.user("admin")).isNotNull();
    assertThat(issues.user("admin").name()).isEqualTo("Administrator");

    // unassign
    adminIssueClient().assign(issue.key(), null);
    reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.assignee()).isNull();
    Assertions.assertThat(searchIssues(IssueQuery.create().assignees("admin"))).isEmpty();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void fail_assign_if_assignee_does_not_exist() {
    assertThat(issue.assignee()).isNull();
    try {
      adminIssueClient().assign(issue.key(), "unknown");
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  /**
   * SONAR-4290
   */
  @Test
  public void plan() {
    assertThat(issue.actionPlan()).isNull();

    // Set action plan to issue
    ActionPlan newActionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project("sample")
      .description("Short term issues").deadLine(toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();
    adminIssueClient().plan(issue.key(), newActionPlan.key());
    Assertions.assertThat(search(IssueQuery.create().actionPlans(newActionPlan.key())).list()).hasSize(1);

    projectAnalysis.run();
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.actionPlan()).isEqualTo(newActionPlan.key());
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    ActionPlan actionPlan = search(IssueQuery.create().actionPlans(newActionPlan.key())).actionPlans(reloaded);
    assertThat(actionPlan.name()).isEqualTo(newActionPlan.name());
    assertThat(actionPlan.deadLine()).isEqualTo(newActionPlan.deadLine());
  }

  @Test
  public void fail_plan_if_action_plan_does_not_exist() {
    assertThat(issue.actionPlan()).isNull();
    try {
      adminIssueClient().plan(issue.key(), "unknown");
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void unplan() {
    assertThat(issue.actionPlan()).isNull();

    // Set action plan to issue
    ActionPlan newActionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project("sample")
      .description("Short term issues").deadLine(toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();
    adminIssueClient().plan(issue.key(), newActionPlan.key());
    Assertions.assertThat(search(IssueQuery.create().actionPlans(newActionPlan.key())).list()).hasSize(1);

    // Unplan
    adminIssueClient().plan(issue.key(), null);
    Assertions.assertThat(search(IssueQuery.create().actionPlans(newActionPlan.key())).list()).hasSize(0);

    projectAnalysis.run();
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.actionPlan()).isNull();
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
  }

  /**
   * SONAR-4315
   */
  @Test
  public void apply_action_from_plugin() {
    // The condition on the action defined by the plugin is that the status must be resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    Assertions.assertThat(adminIssueClient().actions(issue.key())).contains("fake");

    adminIssueClient().doAction(issue.key(), "fake");

    // reload issue
    Issue reloaded = searchIssues(issue.key(), true).iterator().next();

    assertThat(reloaded.comments()).hasSize(1);
    assertThat(reloaded.comments().get(0).htmlText()).isEqualTo("New Comment from fake action");

    // The action is no more available when already executed (because an issue attribute is used to check if the action is available or not)
    Assertions.assertThat(adminIssueClient().actions(issue.key())).doesNotContain("fake");
  }

  /**
   * SONAR-4315
   */
  @Test
  public void issue_attribute_are_kept_on_new_analysis() {
    // The condition on the action defined by the plugin is that the status must be resolved
    adminIssueClient().doTransition(issue.key(), "resolve");
    adminIssueClient().doAction(issue.key(), "fake");
    Assertions.assertThat(adminIssueClient().actions(issue.key())).doesNotContain("fake");

    projectAnalysis.run();

    // Fake action is no more available if the issue attribute is still there
    Assertions.assertThat(adminIssueClient().actions(issue.key())).doesNotContain("fake");
  }

  private static List<Issue> searchIssuesBySeverities(String componentKey, String... severities) {
    return searchIssues(IssueQuery.create().componentRoots(componentKey).severities(severities));
  }

  private static ActionPlanClient adminActionPlanClient() {
    return ORCHESTRATOR.getServer().adminWsClient().actionPlanClient();
  }

}
