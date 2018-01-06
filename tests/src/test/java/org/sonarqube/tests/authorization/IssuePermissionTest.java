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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.issues.BulkChangeRequest;
import org.sonarqube.ws.client.issues.ChangelogRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.projects.UpdateVisibilityRequest;
import util.ItUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.projectDir;

public class IssuePermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @Before
  public void init() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/authorisation/one-issue-per-line-profile.xml"));

    orchestrator.getServer().provisionProject("privateProject", "PrivateProject");
    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject("privateProject").setVisibility("private"));
    orchestrator.getServer().associateProjectToQualityProfile("privateProject", "xoo", "one-issue-per-line");
    SonarScanner privateProject = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "privateProject")
      .setProperty("sonar.projectName", "PrivateProject");
    orchestrator.executeBuild(privateProject);

    orchestrator.getServer().provisionProject("publicProject", "PublicProject");
    orchestrator.getServer().associateProjectToQualityProfile("publicProject", "xoo", "one-issue-per-line");
    SonarScanner publicProject = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "publicProject")
      .setProperty("sonar.projectName", "PublicProject");


    orchestrator.executeBuild(publicProject);
  }

  @Test
  public void need_user_permission_on_project_to_see_issue() {
    SonarClient client = orchestrator.getServer().adminWsClient();

    String withBrowsePermission = "with-browse-permission";
    String withoutBrowsePermission = "without-browse-permission";

    try {
      client.userClient().create(UserParameters.create().login(withBrowsePermission).name(withBrowsePermission)
        .password("password").passwordConfirmation("password"));
      addUserPermission(withBrowsePermission, "privateProject", "user");

      client.userClient().create(UserParameters.create().login(withoutBrowsePermission).name(withoutBrowsePermission)
        .password("password").passwordConfirmation("password"));

      // Without user permission, a user cannot see issues on the project
      assertThat(orchestrator.getServer().wsClient(withoutBrowsePermission, "password").issueClient().find(
        IssueQuery.create().componentRoots("privateProject")).list()).isEmpty();

      // With user permission, a user can see issues on the project
      assertThat(orchestrator.getServer().wsClient(withBrowsePermission, "password").issueClient().find(
        IssueQuery.create().componentRoots("privateProject")).list()).isNotEmpty();

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
    Issue issue = client.issueClient().find(IssueQuery.create().componentRoots("privateProject")).list().get(0);
    client.issueClient().assign(issue.key(), "admin");

    String withBrowsePermission = "with-browse-permission";
    String withoutBrowsePermission = "without-browse-permission";

    try {
      client.userClient().create(UserParameters.create().login(withBrowsePermission).name(withBrowsePermission)
        .password("password").passwordConfirmation("password"));
      addUserPermission(withBrowsePermission, "privateProject", "user");

      client.userClient().create(UserParameters.create().login(withoutBrowsePermission).name(withoutBrowsePermission)
        .password("password").passwordConfirmation("password"));

      // Without user permission, a user cannot see issue changelog on the project
      try {
        changelog(issue.key(), withoutBrowsePermission, "password");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(org.sonarqube.ws.client.HttpException.class).describedAs("403");
      }

      // Without user permission, a user cannot see issues on the project
      assertThat(changelog(issue.key(), withBrowsePermission, "password").getChangelogList()).isNotEmpty();

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
    Issue issueOnPrivateProject = client.issueClient().find(IssueQuery.create().componentRoots("privateProject")).list().get(0);
    Issue issueOnPublicProject = client.issueClient().find(IssueQuery.create().componentRoots("publicProject")).list().get(0);

    String user = "user";

    try {
      client.userClient().create(UserParameters.create().login(user).name(user).password("password").passwordConfirmation("password"));
      addUserPermission(user, "publicProject", "issueadmin");

      // Without issue admin permission, a user cannot set severity on the issue
      try {
        orchestrator.getServer().wsClient(user, "password").issueClient().setSeverity(issueOnPrivateProject.key(), "BLOCKER");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(HttpException.class).describedAs("404");
      }

      // With issue admin permission, a user can set severity on the issue
      assertThat(orchestrator.getServer().wsClient(user, "password").issueClient().setSeverity(issueOnPublicProject.key(), "BLOCKER").severity()).isEqualTo("BLOCKER");

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
    Issue issueOnPrivateProject = client.issueClient().find(IssueQuery.create().componentRoots("privateProject")).list().get(0);
    Issue issueOnPublicProject = client.issueClient().find(IssueQuery.create().componentRoots("publicProject")).list().get(0);

    String user = "user";

    try {
      client.userClient().create(UserParameters.create().login(user).name(user).password("password").passwordConfirmation("password"));
      addUserPermission(user, "publicProject", "issueadmin");

      // Without issue admin permission, a user cannot flag an issue as false positive
      try {
        orchestrator.getServer().wsClient(user, "password").issueClient().doTransition(issueOnPrivateProject.key(), "falsepositive");
        fail();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(HttpException.class).describedAs("404");
      }

      // With issue admin permission, a user can flag an issue as false positive
      assertThat(orchestrator.getServer().wsClient(user, "password").issueClient().doTransition(issueOnPublicProject.key(), "falsepositive").status()).isEqualTo("RESOLVED");

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
    Issue issueOnPrivateProject = client.issueClient().find(IssueQuery.create().componentRoots("privateProject")).list().get(0);
    Issue issueOnPublicProject = client.issueClient().find(IssueQuery.create().componentRoots("publicProject")).list().get(0);

    String user = "user";

    try {
      client.userClient().create(UserParameters.create().login(user).name(user).password("password").passwordConfirmation("password"));
      addUserPermission(user, "privateProject", "issueadmin");

      Issues.BulkChangeWsResponse response = makeBlockerAndFalsePositive(user, issueOnPrivateProject, issueOnPublicProject);

      // public project but no issueadmin permission on publicProject => issue visible but not updated
      // no user permission on privateproject => issue invisible and not updated
      assertThat(response.getTotal()).isEqualTo(1);
      assertThat(response.getSuccess()).isEqualTo(0);
      assertThat(response.getIgnored()).isEqualTo(1);

      addUserPermission(user, "privateProject", "user");
      response = makeBlockerAndFalsePositive(user, issueOnPrivateProject, issueOnPublicProject);

      // public project but no issueadmin permission on publicProject => unsuccessful on issueOnPublicProject
      // user and issueadmin permission on privateproject => successful and 1 more issue visible
      assertThat(response.getTotal()).isEqualTo(2);
      assertThat(response.getSuccess()).isEqualTo(1);
      assertThat(response.getIgnored()).isEqualTo(1);

      addUserPermission(user, "publicProject", "issueadmin");
      response = makeBlockerAndFalsePositive(user, issueOnPrivateProject, issueOnPublicProject);

      // public and issueadmin permission on publicProject => successful on issueOnPublicProject
      // issueOnPrivateProject already in specified state => unsuccessful
      assertThat(response.getTotal()).isEqualTo(2);
      assertThat(response.getSuccess()).isEqualTo(1);
      assertThat(response.getIgnored()).isEqualTo(1);

      response = makeBlockerAndFalsePositive(user, issueOnPrivateProject, issueOnPublicProject);

      // issueOnPublicProject and issueOnPrivateProject already in specified state => unsuccessful
      assertThat(response.getTotal()).isEqualTo(2);
      assertThat(response.getSuccess()).isEqualTo(0);
      assertThat(response.getIgnored()).isEqualTo(2);
    } finally {
      client.userClient().deactivate(user);
    }
  }

  private Issues.BulkChangeWsResponse makeBlockerAndFalsePositive(String user, Issue issueOnPrivateProject, Issue issueOnPublicProject) {
    return newUserWsClient(orchestrator, user, "password").issues()
      .bulkChange(new BulkChangeRequest().setIssues(asList(issueOnPrivateProject.key(), issueOnPublicProject.key()))
        .setSetSeverity(singletonList("BLOCKER"))
        .setDoTransition("falsepositive"));
  }

  private void addUserPermission(String login, String projectKey, String permission) {
    tester.wsClient().permissions().addUser(
      new AddUserRequest()
        .setLogin(login)
        .setProjectKey(projectKey)
        .setPermission(permission));
  }

  private static Issues.ChangelogWsResponse changelog(String issueKey, String login, String password) {
    return newUserWsClient(orchestrator, login, password).issues().changelog(new ChangelogRequest().setIssue(issueKey));
  }
}
