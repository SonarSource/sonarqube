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

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.issues.IssuesPage;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.issues.AssignRequest;
import org.sonarqube.ws.client.issues.BulkChangeRequest;
import org.sonarqube.ws.client.issues.SearchRequest;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectHttpError;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.runProjectAnalysis;

public class OrganizationIssueAssignTest {

  private final static String SAMPLE_PROJECT_KEY = "sample";

  @ClassRule
  public static Orchestrator orchestrator = OrganizationIssueSuite.ORCHESTRATOR;
  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void auto_assign_issues_to_default_assignee_if_member_of_project_organization() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    provisionProjectAndAssociateItToQProfile(SAMPLE_PROJECT_KEY, organization);
    tester.settings().setProjectSetting("sample", "sonar.issues.defaultAssigneeLogin", user.getLogin());

    analyseProject(SAMPLE_PROJECT_KEY, organization);

    assertThat(getRandomIssue().getAssignee()).isEqualTo(user.getLogin());
  }

  @Test
  public void do_not_auto_assign_issues_to_default_assignee_if_not_member_of_project_organization() {
    Organization organization1 = tester.organizations().generate();
    Organization organization2 = tester.organizations().generate();
    User user = tester.users().generateMember(organization2);
    provisionProjectAndAssociateItToQProfile(SAMPLE_PROJECT_KEY, organization1);
    tester.settings().setProjectSetting("sample", "sonar.issues.defaultAssigneeLogin", user.getLogin());

    analyseProject(SAMPLE_PROJECT_KEY, organization1);

    assertThat(getRandomIssue().hasAssignee()).isFalse();
  }

  /**
   * SONAR-10302
   */
  @Test
  public void do_not_auto_assign_issues_to_user_if_assignee_is_not_member_of_project_organization() {
    Organization organization1 = tester.organizations().generate();
    Organization organization2 = tester.organizations().generate();
    User fabrice = tester.users().generateMember(organization1, u -> u.setScmAccount(singletonList("fabrice")));
    // Simon is not member of project's organization, no issue should be assigned to him
    User simon = tester.users().generateMember(organization2, u -> u.setScmAccount(singletonList("simon")));
    provisionProjectAndAssociateItToQProfile(SAMPLE_PROJECT_KEY, organization1);

    analyseProject(SAMPLE_PROJECT_KEY, organization1);

    Set<String> assignees = tester.wsClient().issues().search(new SearchRequest().setComponentKeys(singletonList(SAMPLE_PROJECT_KEY))).getIssuesList()
      .stream()
      .map(Issue::getAssignee)
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());
    assertThat(assignees)
      .containsOnly(fabrice.getLogin())
      .doesNotContain(simon.getLogin());
  }

  @Test
  public void assign_issue_to_user_being_member_of_same_organization_as_project_issue_organization() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, organization);
    Issue issue = getRandomIssue();

    assignIssueTo(issue, user);

    assertThat(getByKey(issue.getKey()).getAssignee()).isEqualTo(user.getLogin());
  }

  @Test
  public void fail_to_assign_issue_to_user_not_being_member_of_same_organization_as_project_issue_organization() {
    Organization organization1 = tester.organizations().generate();
    Organization organization2 = tester.organizations().generate();
    User user = tester.users().generateMember(organization2);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, organization1);
    Issue issue = getRandomIssue();

    expectHttpError(400,
      format("User '%s' is not member of organization '%s'", user.getLogin(), organization1.getKey()),
      () -> assignIssueTo(issue, user));
  }

  @Test
  public void bulk_assign_issues_to_user_being_only_member_of_same_organization_as_project_issue_organization() {
    Organization organization1 = tester.organizations().generate();
    Organization organization2 = tester.organizations().generate();
    // User is only member of org1, not of org2
    User user = tester.users().generateMember(organization1);

    restoreProfile(orchestrator, getClass().getResource("/organization/IssueAssignTest/one-issue-per-file-profile.xml"), organization2.getKey());

    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, organization1);
    provisionAndAnalyseProject("sample2", organization2);
    List<String> issues = tester.wsClient().issues().search(new SearchRequest()).getIssuesList().stream().map(Issue::getKey).collect(Collectors.toList());

    Issues.BulkChangeWsResponse response = tester.wsClient().issues()
      .bulkChange(new BulkChangeRequest().setIssues(issues).setAssign(singletonList(user.getLogin())));

    assertThat(response.getIgnored()).isGreaterThan(0);
    assertThat(tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList("sample"))).getIssuesList()).extracting(Issue::getAssignee)
      .containsOnly(user.getLogin());
    assertThat(tester.wsClient().issues().search(new SearchRequest().setProjects(singletonList("sample2"))).getIssuesList()).extracting(Issue::hasAssignee)
      .containsOnly(false);
  }

  @Test
  public void single_assign_search_show_only_members_in_global_issues() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    User otherUser = tester.users().generate();
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, organization);

    IssuesPage page = tester.openBrowser().logIn().submitCredentials(user.getLogin()).openIssues();
    page.getFirstIssue()
      .shouldAllowAssign()
      .assigneeSearchResultCount(otherUser.getLogin(), 0)
      .assigneeSearchResultCount(user.getLogin(), 1);
  }

  @Test
  public void bulk_assign_search_only_members_of_organization_in_project_issues() {
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    User otherUser = tester.users().generate();
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, organization);

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
    Organization organization = tester.organizations().generate();
    User user = tester.users().generateMember(organization);
    User otherUser = tester.users().generate();
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, organization);
    IssuesPage page = tester.openBrowser()
      .logIn().submitCredentials(user.getLogin())
      .openIssues();
    page
      .bulkChangeOpen()
      .bulkChangeAssigneeSearchCount(user.getLogin(), 1)
      .bulkChangeAssigneeSearchCount(otherUser.getLogin(), 1);
  }

  private void provisionAndAnalyseProject(String projectKey, Organization organization) {
    provisionProjectAndAssociateItToQProfile(projectKey, organization);
    analyseProject(projectKey, organization);
  }

  private void provisionProjectAndAssociateItToQProfile(String projectKey, Organization organization) {
    Project project = tester.projects().provision(organization, p -> p.setProject(projectKey));
    QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles()
      .activateRule(profile, "xoo:OneIssuePerLine")
      .assignQProfileToProject(profile, project);
  }

  private void analyseProject(String projectKey, Organization organization) {
    runProjectAnalysis(orchestrator, "issue/xoo-with-scm",
      "sonar.projectKey", projectKey,
      "sonar.organization", organization.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin",
      "sonar.scm.disabled", "false",
      "sonar.scm.provider", "xoo");
  }

  private Issues.Issue getByKey(String issueKey) {
    return tester.wsClient().issues().search(
      new SearchRequest()
        .setComponentKeys(singletonList(SAMPLE_PROJECT_KEY))
        .setIssues(singletonList(issueKey)))
      .getIssuesList()
      .get(0);
  }

  private Issues.Issue getRandomIssue() {
    return tester.wsClient().issues().search(
      new SearchRequest()
        .setComponentKeys(singletonList(SAMPLE_PROJECT_KEY)))
      .getIssuesList()
      .get(0);
  }

  private Issues.AssignResponse assignIssueTo(Issue issue, User u) {
    return tester.wsClient().issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(u.getLogin()));
  }
}
