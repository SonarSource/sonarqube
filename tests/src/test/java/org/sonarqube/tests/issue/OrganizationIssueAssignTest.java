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

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category6Suite;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.issue.AssignRequest;
import org.sonarqube.ws.client.issue.BulkChangeRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import org.sonarqube.pageobjects.issues.IssuesPage;
import util.issue.IssueRule;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectHttpError;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class OrganizationIssueAssignTest {

  private final static String SAMPLE_PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  @Rule
  public Tester tester = new Tester(orchestrator);

  @Rule
  public IssueRule issueRule = IssueRule.from(orchestrator);

  private Organizations.Organization org1;
  private Organizations.Organization org2;
  private User user;

  @Before
  public void setUp() throws Exception {
    org1 = tester.organizations().generate();
    org2 = tester.organizations().generate();
    user = tester.users().generate();
    restoreProfile(orchestrator, getClass().getResource("/organization/IssueAssignTest/one-issue-per-file-profile.xml"), org1.getKey());
  }

  @Test
  public void auto_assign_issues_to_user_if_default_assignee_is_member_of_project_organization() {
    tester.organizations().addMember(org1, user);

    provisionProject(SAMPLE_PROJECT_KEY, org1.getKey());
    setServerProperty(orchestrator, "sample", "sonar.issues.defaultAssigneeLogin", user.getLogin());

    analyseProject(SAMPLE_PROJECT_KEY, org1.getKey());

    assertThat(issueRule.getRandomIssue().getAssignee()).isEqualTo(user.getLogin());
  }

  @Test
  public void does_not_auto_assign_issues_to_user_if_default_assignee_is_not_member_of_project_organization() {
    tester.organizations().addMember(org2, user);
    provisionProject(SAMPLE_PROJECT_KEY, org1.getKey());
    setServerProperty(orchestrator, "sample", "sonar.issues.defaultAssigneeLogin", user.getLogin());

    analyseProject(SAMPLE_PROJECT_KEY, org1.getKey());

    assertThat(issueRule.getRandomIssue().hasAssignee()).isFalse();
  }

  @Test
  public void assign_issue_to_user_being_member_of_same_organization_as_project_issue_organization() {
    tester.organizations().addMember(org1, user);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, org1.getKey());
    Issue issue = issueRule.getRandomIssue();

    assignIssueTo(issue, user);

    assertThat(issueRule.getByKey(issue.getKey()).getAssignee()).isEqualTo(user.getLogin());
  }

  @Test
  public void fail_to_assign_issue_to_user_not_being_member_of_same_organization_as_project_issue_organization() {
    tester.organizations().addMember(org2, user);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, org1.getKey());
    Issue issue = issueRule.getRandomIssue();

    expectHttpError(400,
      format("User '%s' is not member of organization '%s'", user.getLogin(), org1.getKey()),
      () -> assignIssueTo(issue, user));
  }

  @Test
  public void bulk_assign_issues_to_user_being_only_member_of_same_organization_as_project_issue_organization() {
    restoreProfile(orchestrator, getClass().getResource("/organization/IssueAssignTest/one-issue-per-file-profile.xml"), org2.getKey());
    // User is only member of org1, not of org2
    tester.organizations().addMember(org1, user);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, org1.getKey());
    provisionAndAnalyseProject("sample2", org2.getKey());
    List<String> issues = issueRule.search(new org.sonarqube.ws.client.issue.SearchWsRequest()).getIssuesList().stream().map(Issue::getKey).collect(Collectors.toList());

    Issues.BulkChangeWsResponse response = tester.wsClient().issues()
      .bulkChange(BulkChangeRequest.builder().setIssues(issues).setAssign(user.getLogin()).build());

    assertThat(response.getIgnored()).isGreaterThan(0);
    assertThat(issueRule.search(new SearchWsRequest().setProjectKeys(singletonList("sample"))).getIssuesList()).extracting(Issue::getAssignee)
      .containsOnly(user.getLogin());
    assertThat(issueRule.search(new SearchWsRequest().setProjectKeys(singletonList("sample2"))).getIssuesList()).extracting(Issue::hasAssignee)
      .containsOnly(false);
  }

  @Test
  public void single_assign_search_show_only_members_in_global_issues() {
    tester.organizations().addMember(org1, user);
    User otherUser = tester.users().generate();
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, org1.getKey());
    IssuesPage page = tester.openBrowser().logIn().submitCredentials(user.getLogin()).openIssues();
    page.getFirstIssue()
      .shouldAllowAssign()
      .assigneeSearchResultCount(otherUser.getLogin(), 0)
      .assigneeSearchResultCount(user.getLogin(), 1);
  }

  @Test
  public void bulk_assign_search_only_members_of_organization_in_project_issues() {
    tester.organizations().addMember(org1, user);
    User otherUser = tester.users().generate();

    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, org1.getKey());
    IssuesPage page = tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openComponentIssues(SAMPLE_PROJECT_KEY);
    page
      .bulkChangeOpen()
      .bulkChangeAssigneeSearchCount(user.getLogin(), 1)
      .bulkChangeAssigneeSearchCount(otherUser.getLogin(), 0);
  }

  @Test
  public void bulk_assign_search_all_users_in_global_issues() {
    tester.organizations().addMember(org1, user);
    User otherUser = tester.users().generate();
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, org1.getKey());
    IssuesPage page = tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openIssues();
    page
      .bulkChangeOpen()
      .bulkChangeAssigneeSearchCount(user.getLogin(), 1)
      .bulkChangeAssigneeSearchCount(otherUser.getLogin(), 1);
  }

  private void provisionAndAnalyseProject(String projectKey, String organization) {
    provisionProject(projectKey, organization);
    analyseProject(projectKey, organization);
  }

  private void provisionProject(String projectKey, String organization) {
    tester.wsClient().projects().create(
      CreateRequest.builder()
        .setKey(projectKey)
        .setName(projectKey)
        .setOrganization(organization)
        .build());
  }

  private void analyseProject(String projectKey, String organization) {
    addQualityProfileToProject(organization, projectKey);
    runProjectAnalysis(orchestrator, "issue/xoo-with-scm",
      "sonar.projectKey", projectKey,
      "sonar.organization", organization,
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.scm.disabled", "false",
      "sonar.scm.provider", "xoo");
  }

  private void addQualityProfileToProject(String organization, String projectKey) {
    tester.wsClient().qualityProfiles().addProject(
      AddProjectRequest.builder()
        .setProjectKey(projectKey)
        .setOrganization(organization)
        .setLanguage("xoo")
        .setProfileName("one-issue-per-file-profile")
        .build());
  }

  private Issues.Operation assignIssueTo(Issue issue, User u) {
    return tester.wsClient().issues().assign(new AssignRequest(issue.getKey(), u.getLogin()));
  }
}
