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
package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.BulkChange;
import org.sonar.wsclient.issue.BulkChangeQuery;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class IssuePermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  public WsClient adminWsClient = newAdminWsClient(orchestrator);

  @Before
  public void init() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/authorisation/one-issue-per-line-profile.xml"));

    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(sampleProject);

    orchestrator.getServer().provisionProject("sample2", "Sample2");
    orchestrator.getServer().associateProjectToQualityProfile("sample2", "xoo", "one-issue-per-line");
    SonarScanner sampleProject2 = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "sample2")
      .setProperty("sonar.projectName", "Sample2");
    orchestrator.executeBuild(sampleProject2);
  }

  @Test
  public void need_user_permission_on_project_to_see_issue() {
    SonarClient client = orchestrator.getServer().adminWsClient();

    String withBrowsePermission = "with-browse-permission";
    String withoutBrowsePermission = "without-browse-permission";

    try {
      client.userClient().create(UserParameters.create().login(withBrowsePermission).name(withBrowsePermission)
        .password("password").passwordConfirmation("password"));
      addUserPermission(withBrowsePermission, "sample", "user");

      client.userClient().create(UserParameters.create().login(withoutBrowsePermission).name(withoutBrowsePermission)
        .password("password").passwordConfirmation("password"));
      // By default, it's the group anyone that have the permission user, it would be better to remove all groups on this permission
      removeGroupPermission("anyone", "sample", "user");

      // Without user permission, a user cannot see issues on the project
      assertThat(orchestrator.getServer().wsClient(withoutBrowsePermission, "password").issueClient().find(
        IssueQuery.create().componentRoots("sample")).list()).isEmpty();

      // With user permission, a user can see issues on the project
      assertThat(orchestrator.getServer().wsClient(withBrowsePermission, "password").issueClient().find(
        IssueQuery.create().componentRoots("sample")).list()).isNotEmpty();

    } finally {
      client.userClient().deactivate(withBrowsePermission);
      client.userClient().deactivate(withoutBrowsePermission);
    }
  }

  /**
   * SONAR-4839
   */
  @Test
  public void need_user_permission_on_project_to_see_issue_changelog() {
    SonarClient client = orchestrator.getServer().adminWsClient();
    Issue issue = client.issueClient().find(IssueQuery.create().componentRoots("sample")).list().get(0);
    client.issueClient().assign(issue.key(), "admin");

    String withBrowsePermission = "with-browse-permission";
    String withoutBrowsePermission = "without-browse-permission";

    try {
      client.userClient().create(UserParameters.create().login(withBrowsePermission).name(withBrowsePermission)
        .password("password").passwordConfirmation("password"));
      addUserPermission(withBrowsePermission, "sample", "user");

      client.userClient().create(UserParameters.create().login(withoutBrowsePermission).name(withoutBrowsePermission)
        .password("password").passwordConfirmation("password"));
      // By default, it's the group anyone that have the permission user, it would be better to remove all groups on this permission
      removeGroupPermission("anyone", "sample", "user");

      // Without user permission, a user cannot see issue changelog on the project
      try {
        orchestrator.getServer().wsClient(withoutBrowsePermission, "password").issueClient().changes(issue.key());
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(HttpException.class).describedAs("404");
      }

      // Without user permission, a user cannot see issues on the project
      assertThat(orchestrator.getServer().wsClient(withBrowsePermission, "password").issueClient().changes(issue.key())).isNotEmpty();

    } finally {
      client.userClient().deactivate(withBrowsePermission);
      client.userClient().deactivate(withoutBrowsePermission);
    }
  }

  /**
   * SONAR-2447
   */
  @Test
  public void need_administer_issue_permission_on_project_to_set_severity() {
    SonarClient client = orchestrator.getServer().adminWsClient();
    Issue issueOnSample = client.issueClient().find(IssueQuery.create().componentRoots("sample")).list().get(0);
    Issue issueOnSample2 = client.issueClient().find(IssueQuery.create().componentRoots("sample2")).list().get(0);

    String user = "user";

    try {
      client.userClient().create(UserParameters.create().login(user).name(user).password("password").passwordConfirmation("password"));
      addUserPermission(user, "sample", "issueadmin");

      // Without issue admin permission, a user cannot set severity on the issue
      try {
        orchestrator.getServer().wsClient(user, "password").issueClient().setSeverity(issueOnSample2.key(), "BLOCKER");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(HttpException.class).describedAs("404");
      }

      // With issue admin permission, a user can set severity on the issue
      assertThat(orchestrator.getServer().wsClient(user, "password").issueClient().setSeverity(issueOnSample.key(), "BLOCKER").severity()).isEqualTo("BLOCKER");

    } finally {
      client.userClient().deactivate(user);
    }
  }

  /**
   * SONAR-2447
   */
  @Test
  public void need_administer_issue_permission_on_project_to_flag_as_false_positive() {
    SonarClient client = orchestrator.getServer().adminWsClient();
    Issue issueOnSample = client.issueClient().find(IssueQuery.create().componentRoots("sample")).list().get(0);
    Issue issueOnSample2 = client.issueClient().find(IssueQuery.create().componentRoots("sample2")).list().get(0);

    String user = "user";

    try {
      client.userClient().create(UserParameters.create().login(user).name(user).password("password").passwordConfirmation("password"));
      addUserPermission(user, "sample", "issueadmin");

      // Without issue admin permission, a user cannot flag an issue as false positive
      try {
        orchestrator.getServer().wsClient(user, "password").issueClient().doTransition(issueOnSample2.key(), "falsepositive");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(HttpException.class).describedAs("404");
      }

      // With issue admin permission, a user can flag an issue as false positive
      assertThat(orchestrator.getServer().wsClient(user, "password").issueClient().doTransition(issueOnSample.key(), "falsepositive").status()).isEqualTo("RESOLVED");

    } finally {
      client.userClient().deactivate(user);
    }
  }

  /**
   * SONAR-2447
   */
  @Test
  public void need_administer_issue_permission_on_project_to_bulk_change_severity_and_false_positive() {
    SonarClient client = orchestrator.getServer().adminWsClient();
    Issue issueOnSample = client.issueClient().find(IssueQuery.create().componentRoots("sample")).list().get(0);
    Issue issueOnSample2 = client.issueClient().find(IssueQuery.create().componentRoots("sample2")).list().get(0);

    String user = "user";

    try {
      client.userClient().create(UserParameters.create().login(user).name(user).password("password").passwordConfirmation("password"));
      addUserPermission(user, "sample", "issueadmin");

      BulkChange bulkChange = orchestrator.getServer().wsClient(user, "password").issueClient().bulkChange(
        BulkChangeQuery.create().issues(issueOnSample.key(), issueOnSample2.key())
          .actions("set_severity", "do_transition")
          .actionParameter("do_transition", "transition", "falsepositive")
          .actionParameter("set_severity", "severity", "BLOCKER"));

      assertThat(bulkChange.totalIssuesChanged()).isEqualTo(1);
      assertThat(bulkChange.totalIssuesNotChanged()).isEqualTo(1);

    } finally {
      client.userClient().deactivate(user);
    }
  }

  private void addUserPermission(String login, String projectKey, String permission) {
    adminWsClient.permissions().addUser(
      new AddUserWsRequest()
        .setLogin(login)
        .setProjectKey(projectKey)
        .setPermission(permission));
  }

  private void removeGroupPermission(String groupName, String projectKey, String permission) {
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest()
      .setGroupName(groupName)
      .setProjectKey(projectKey)
      .setPermission(permission));
  }
}
