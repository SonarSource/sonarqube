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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.users.CreateRequest;
import org.sonarqube.ws.client.users.SearchRequest;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

public class AutoAssignTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  ProjectAnalysis projectAnalysis;

  @Before
  public void setup() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("issue/AutoAssignTest");
    projectAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey)
      .withQualityProfile(qualityProfileKey)
      .withProperties("sonar.scm.disabled", "false", "sonar.scm.provider", "xoo");
  }

  @After
  public void resetData() {
    newAdminWsClient(ORCHESTRATOR).wsConnector().call(new PostRequest("api/projects/delete").setParam("project", "AutoAssignTest"));
    deleteAllUsers();

    // Reset default assignee
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", null);
  }

  @Test
  public void auto_assign_issues_to_user() {
    // verify that login matches, case-sensitive
    createUser("user1", "User 1", "user1@email.com");
    createUser("USER2", "User 2", "user2@email.com");
    // verify that name is not used to match, whatever the case
    createUser("user3", "User 3", "user3@email.com");
    createUser("user4", "USER 4", "user4@email.com");
    // verify that email matches, case-insensitive
    createUser("user5", "User 5", "user5@email.com");
    createUser("user6", "User 6", "USER6@email.COM");
    // verify that SCM account matches, case-insensitive
    createUser("user7", "User 7", "user7@email.com", "user7ScmAccount");
    createUser("user8", "User 8", "user8@email.com", "user8SCMaccOUNT");
    // SCM accounts longer than 255
    createUser("user9", "User 9", "user9@email.com", IntStream.range(0, 256).mapToObj(i -> "s").collect(Collectors.joining()));

    projectAnalysis.run();

    List<Issue> issues = search(IssueQuery.create().components("AutoAssignTest:src/sample.xoo").sort("FILE_LINE")).list();
    // login match, case-sensitive
    verifyIssueAssignee(issues, 1, "user1");
    verifyIssueAssignee(issues, 2, null);
    // user name is not used to match
    verifyIssueAssignee(issues, 3, null);
    verifyIssueAssignee(issues, 4, null);
    // email match, case-insensitive
    verifyIssueAssignee(issues, 5, "user5");
    verifyIssueAssignee(issues, 6, "user6");
    // SCM account match, case-insensitive
    verifyIssueAssignee(issues, 7, "user7");
    verifyIssueAssignee(issues, 8, "user8");
    // SCM accounts longer than 255 chars
    verifyIssueAssignee(issues, 10, "user9");
  }

  private static void verifyIssueAssignee(List<Issue> issues, int line, @Nullable String expectedAssignee) {
    assertThat(issues.get(line - 1).assignee()).isEqualTo(expectedAssignee);
  }

  @Test
  public void auto_assign_issues_to_default_assignee() {
    createUser("user1", "User 1", "user1@email.com");
    createUser("user2", "User 2", "user2@email.com");
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", "user2");
    projectAnalysis.run();

    // user1 is assigned to his issues. All other issues are assigned to the default assignee.
    assertThat(search(IssueQuery.create().assignees("user1")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assignees("user2")).list()).hasSize(9);
    // No unassigned issues
    assertThat(search(IssueQuery.create().assigned(false)).list()).isEmpty();
  }

  /**
   * SONAR-7098
   *
   * Given two versions of same project:
   * v1: issue, but no SCM data
   * v2: old issue and SCM data
   * Expected: all issues should be associated with authors
   */
  @Test
  public void update_author_and_assignee_when_scm_is_activated() {
    createUser("user1", "User 1", "user1@email.com");

    // Run a first analysis without SCM
    projectAnalysis.withProperties("sonar.scm.disabled", "true").run();
    List<Issue> issues = searchIssues();
    assertThat(issues).isNotEmpty();

    // No author and assignee are set
    assertThat(issues)
      .extracting(Issue::line, Issue::author)
      .containsExactlyInAnyOrder(
        tuple(1, ""),
        tuple(2, ""),
        tuple(3, ""),
        tuple(4, ""),
        tuple(5, ""),
        tuple(6, ""),
        tuple(7, ""),
        tuple(8, ""),
        tuple(9, ""),
        tuple(10, ""));
    assertThat(search(IssueQuery.create().assigned(true)).list()).isEmpty();

    // Run a second analysis with SCM
    projectAnalysis.run();
    issues = searchIssues();
    assertThat(issues).isNotEmpty();

    // Authors and assignees are set
    assertThat(issues)
      .extracting(Issue::line, Issue::author)
      .containsExactlyInAnyOrder(
        tuple(1, "user1"),
        tuple(2, "user2"),
        tuple(3, "user3name"),
        tuple(4, "user4name"),
        tuple(5, "user5@email.com"),
        tuple(6, "user6@email.com"),
        tuple(7, "user7scmaccount"),
        tuple(8, "user8scmaccount"),
        tuple(9, "user8scmaccount"),
        // SONAR-8727
        tuple(10, ""));
    assertThat(search(IssueQuery.create().assignees("user1")).list()).hasSize(1);
  }

  private static void createUser(String login, String name, String email, String... scmAccounts) {
    newAdminWsClient(ORCHESTRATOR).users().create(
      new CreateRequest()
        .setLogin(login)
        .setName(name)
        .setEmail(email)
        .setPassword("xxxxxxx")
        .setScmAccounts(asList(scmAccounts)));
  }

  private static void deleteAllUsers() {
    WsClient wsClient = newAdminWsClient(ORCHESTRATOR);
    Users.SearchWsResponse searchResponse = wsClient.users().search(new SearchRequest());
    searchResponse.getUsersList().forEach(user -> {
      wsClient.wsConnector().call(new PostRequest("api/users/deactivate").setParam("login", user.getLogin()));
    });
  }
}
