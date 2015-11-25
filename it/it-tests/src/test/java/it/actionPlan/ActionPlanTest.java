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
package it.actionPlan;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.issue.NewActionPlan;
import org.sonar.wsclient.issue.UpdateActionPlan;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddUserWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.toDate;
import static util.ItUtils.verifyHttpException;

public class ActionPlanTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;
  private static final String PROJECT_KEY = "sample";

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/actionPlan/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line-profile");
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    adminWsClient = newAdminWsClient(orchestrator);
  }

  protected static ActionPlanClient adminActionPlanClient() {
    return orchestrator.getServer().adminWsClient().actionPlanClient();
  }

  protected static ActionPlanClient actionPlanClient() {
    return orchestrator.getServer().wsClient().actionPlanClient();
  }

  protected static ActionPlan firstActionPlan(String projectKey) {
    List<ActionPlan> actionPlans = actionPlanClient().find(projectKey);
    assertThat(actionPlans).hasSize(1);
    return actionPlans.get(0);
  }

  protected static IssueClient adminIssueClient() {
    return orchestrator.getServer().adminWsClient().issueClient();
  }

  protected static Issue searchRandomIssue() {
    List<Issue> issues = search(IssueQuery.create()).list();
    assertThat(issues).isNotEmpty();
    return issues.get(0);
  }

  protected static Issues search(IssueQuery issueQuery) {
    return issueClient().find(issueQuery);
  }

  protected static IssueClient issueClient() {
    return orchestrator.getServer().wsClient().issueClient();
  }

  @Before
  public void resetData() {
    // TODO should be done by a WS
    orchestrator.getDatabase().truncate("action_plans");
    assertThat(adminActionPlanClient().find(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void create_action_plan() {
    assertThat(adminActionPlanClient().find(PROJECT_KEY)).isEmpty();

    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.name()).isEqualTo("Short term");
    assertThat(actionPlan.description()).isEqualTo("Short term issues");
    assertThat(actionPlan.status()).isEqualTo("OPEN");
    assertThat(actionPlan.project()).isEqualTo(PROJECT_KEY);
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.createdAt()).isNotNull();
    assertThat(actionPlan.updatedAt()).isNotNull();
    assertThat(actionPlan.totalIssues()).isEqualTo(0);
    assertThat(actionPlan.unresolvedIssues()).isEqualTo(0);
  }

  /**
   * SONAR-5179
   */
  @Test
  public void need_project_administrator_permission_to_create_action_plan() {
    String projectAdminUser = "with-admin-permission-on-project";
    String projectUser = "with-user-permission-on-project";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      // Create a user having admin permission on the project
      adminClient.userClient().create(UserParameters.create().login(projectAdminUser).name(projectAdminUser).password("password").passwordConfirmation("password"));
      adminWsClient.permissions().addUser(
        new AddUserWsRequest()
          .setLogin(projectAdminUser)
          .setProjectKey(PROJECT_KEY)
          .setPermission("admin"));

      // Create a user having browse permission on the project
      adminClient.userClient().create(UserParameters.create().login(projectUser).name(projectUser).password("password").passwordConfirmation("password"));
      adminWsClient.permissions().addUser(
        new AddUserWsRequest()
          .setLogin(projectUser)
          .setProjectKey(PROJECT_KEY)
          .setPermission("user"));

      // Without project admin permission, a user cannot set action plan
      try {
        orchestrator.getServer().wsClient(projectUser, "password").actionPlanClient().create(
          NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues"));
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(HttpException.class).describedAs("404");
      }

      // With project admin permission, a user can set action plan
      orchestrator.getServer().wsClient(projectAdminUser, "password").actionPlanClient().create(
        NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues"));
      assertThat(actionPlanClient().find(PROJECT_KEY)).hasSize(1);

    } finally {
      adminClient.userClient().deactivate(projectAdminUser);
      adminClient.userClient().deactivate(projectUser);
    }
  }

  @Test
  public void fail_create_action_plan_if_missing_project() {
    try {
      adminActionPlanClient().create(NewActionPlan.create().name("Short term")
        .description("Short term issues").deadLine(toDate("2113-01-31")));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void fail_create_action_plan_if_missing_name() {
    try {
      adminActionPlanClient().create(NewActionPlan.create().project(PROJECT_KEY)
        .description("Short term issues").deadLine(toDate("2113-01-31")));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void update_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));

    ActionPlan updatedActionPlan = adminActionPlanClient().update(
      UpdateActionPlan.create().key(newActionPlan.key()).name("Long term").description("Long term issues").deadLine(toDate("2114-12-01")));
    assertThat(updatedActionPlan).isNotNull();

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.description()).isEqualTo("Long term issues");
    assertThat(actionPlan.project()).isEqualTo(PROJECT_KEY);
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.deadLine()).isNotEqualTo(newActionPlan.deadLine());
    assertThat(actionPlan.updatedAt()).isNotNull();
  }

  @Test
  public void fail_update_action_plan_if_missing_name() {
    try {
      adminActionPlanClient().create(
        NewActionPlan.create().project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void delete_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));

    adminActionPlanClient().delete(newActionPlan.key());

    List<ActionPlan> results = adminActionPlanClient().find(PROJECT_KEY);
    assertThat(results).isEmpty();
  }

  /**
   * SONAR-4449
   */
  @Test
  public void delete_action_plan_also_unplan_linked_issues() {
    // Create action plan
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));

    Issue issue = searchRandomIssue();
    // Link an issue to the action plan
    adminIssueClient().plan(issue.key(), newActionPlan.key());
    // Delete action plan
    adminActionPlanClient().delete(newActionPlan.key());

    // Reload the issue
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.actionPlan()).isNull();
  }

  @Test
  public void close_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));
    assertThat(firstActionPlan(PROJECT_KEY).status()).isEqualTo("OPEN");

    adminActionPlanClient().close(newActionPlan.key());

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.status()).isEqualTo("CLOSED");
  }

  @Test
  public void open_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));

    adminActionPlanClient().close(newActionPlan.key());
    adminActionPlanClient().open(newActionPlan.key());

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.status()).isEqualTo("OPEN");
  }

  @Test
  public void find_action_plans() {
    assertThat(actionPlanClient().find(PROJECT_KEY)).isEmpty();

    adminActionPlanClient().create(NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));
    adminActionPlanClient().create(NewActionPlan.create().name("Long term").project(PROJECT_KEY).description("Long term issues"));

    assertThat(actionPlanClient().find(PROJECT_KEY)).hasSize(2);
  }

  protected Issue searchIssueByKey(String issueKey) {
    IssueQuery query = IssueQuery.create().issues(issueKey);
    query.urlParams().put("additionalFields", "_all");
    List<Issue> issues = search(query).list();
    assertThat(issues).hasSize(1);
    return issues.get(0);
  }

}
