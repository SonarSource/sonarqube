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

package it.organization;

import com.sonar.orchestrator.Orchestrator;
import it.Category6Suite;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issue.AssignRequest;
import org.sonarqube.ws.client.issue.BulkChangeRequest;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import pageobjects.Navigation;
import pageobjects.issues.IssuesPage;
import util.issue.IssueRule;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.deleteOrganizationsIfExists;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newOrganizationKey;
import static util.ItUtils.restoreProfile;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class IssueAssignTest {

  private final static String ORGANIZATION_KEY = newOrganizationKey();
  private final static String OTHER_ORGANIZATION_KEY = newOrganizationKey();
  private final static String SAMPLE_PROJECT_KEY = "sample";
  private static final String ASSIGNEE_LOGIN = "bob";
  private static final String OTHER_LOGIN = "neo";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @ClassRule
  public static IssueRule issueRule = IssueRule.from(orchestrator);

  private WsClient adminClient = newAdminWsClient(orchestrator);

  @BeforeClass
  public static void enableOrganizations() throws Exception {
    enableOrganizationsSupport();
  }

  @Before
  public void setUp() throws Exception {
    userRule.deactivateUsers(ASSIGNEE_LOGIN, OTHER_LOGIN);
    createOrganization(ORGANIZATION_KEY);
    restoreProfile(orchestrator, getClass().getResource("/organization/IssueAssignTest/one-issue-per-file-profile.xml"), ORGANIZATION_KEY);
  }

  @After
  public void tearDown() throws Exception {
    userRule.deactivateUsers(ASSIGNEE_LOGIN, OTHER_LOGIN);
    deleteOrganizationsIfExists(orchestrator, ORGANIZATION_KEY, OTHER_ORGANIZATION_KEY);
  }

  @Test
  public void auto_assign_issues_to_user_if_default_assignee_is_member_of_project_organization() throws Exception {
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    provisionProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    setServerProperty(orchestrator, "sample", "sonar.issues.defaultAssigneeLogin", ASSIGNEE_LOGIN);

    analyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);

    assertThat(issueRule.getRandomIssue().getAssignee()).isEqualTo(ASSIGNEE_LOGIN);
  }

  @Test
  public void does_not_auto_assign_issues_to_user_if_default_assignee_is_not_member_of_project_organization() throws Exception {
    createOrganization(OTHER_ORGANIZATION_KEY);
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(OTHER_ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    provisionProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    setServerProperty(orchestrator, "sample", "sonar.issues.defaultAssigneeLogin", ASSIGNEE_LOGIN);

    analyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);

    assertThat(issueRule.getRandomIssue().hasAssignee()).isFalse();
  }

  @Test
  public void assign_issue_to_user_being_member_of_same_organization_as_project_issue_organization() throws Exception {
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    Issue issue = issueRule.getRandomIssue();

    adminClient.issues().assign(new AssignRequest(issue.getKey(), ASSIGNEE_LOGIN));

    assertThat(issueRule.getByKey(issue.getKey()).getAssignee()).isEqualTo(ASSIGNEE_LOGIN);
  }

  @Test
  public void fail_to_assign_issue_to_user_not_being_member_of_same_organization_as_project_issue_organization() throws Exception {
    createOrganization(OTHER_ORGANIZATION_KEY);
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(OTHER_ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    Issue issue = issueRule.getRandomIssue();

    expectedException.expect(HttpException.class);
    expectedException.expectMessage(format("User 'bob' is not member of organization '%s'", ORGANIZATION_KEY));

    adminClient.issues().assign(new AssignRequest(issue.getKey(), ASSIGNEE_LOGIN));
  }

  @Test
  public void bulk_assign_issues_to_user_being_only_member_of_same_organization_as_project_issue_organization() throws Exception {
    createOrganization(OTHER_ORGANIZATION_KEY);
    restoreProfile(orchestrator, getClass().getResource("/organization/IssueAssignTest/one-issue-per-file-profile.xml"), OTHER_ORGANIZATION_KEY);
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    // User is only member of "organization-key", not of "other-organization-key"
    adminClient.organizations().addMember(ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    provisionAndAnalyseProject("sample2", OTHER_ORGANIZATION_KEY);
    List<String> issues = issueRule.search(new org.sonarqube.ws.client.issue.SearchWsRequest()).getIssuesList().stream().map(Issue::getKey).collect(Collectors.toList());

    Issues.BulkChangeWsResponse response = adminClient.issues().bulkChange(BulkChangeRequest.builder().setIssues(issues).setAssign(ASSIGNEE_LOGIN).build());

    assertThat(response.getIgnored()).isGreaterThan(0);
    assertThat(issueRule.search(new SearchWsRequest().setProjectKeys(singletonList("sample"))).getIssuesList()).extracting(Issue::getAssignee).containsOnly(ASSIGNEE_LOGIN);
    assertThat(issueRule.search(new SearchWsRequest().setProjectKeys(singletonList("sample2"))).getIssuesList()).extracting(Issue::hasAssignee).containsOnly(false);
  }

  @Test
  public void single_assign_search_show_only_members_in_global_issues() {
    createOrganization(OTHER_ORGANIZATION_KEY);
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    userRule.createUser(OTHER_LOGIN, "pwd");
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    IssuesPage page = nav.logIn().asAdmin().openIssues();
    page.getFirstIssue()
      .shouldAllowAssign()
      .assigneeSearchResultCount(OTHER_LOGIN, 0)
      .assigneeSearchResultCount(ASSIGNEE_LOGIN, 1);
  }

  @Test
  public void bulk_assign_search_only_members_of_organization_in_project_issues() {
    createOrganization(OTHER_ORGANIZATION_KEY);
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    userRule.createUser(OTHER_LOGIN, "pwd");
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    IssuesPage page = nav.logIn().asAdmin().openComponentIssues(SAMPLE_PROJECT_KEY);
    page
      .bulkChangeOpen()
      .bulkChangeAssigneeSearchCount(ASSIGNEE_LOGIN, 1)
      .bulkChangeAssigneeSearchCount(OTHER_LOGIN, 0);
  }

  @Test
  public void bulk_assign_search_all_users_in_global_issues() {
    createOrganization(OTHER_ORGANIZATION_KEY);
    userRule.createUser(ASSIGNEE_LOGIN, ASSIGNEE_LOGIN);
    adminClient.organizations().addMember(ORGANIZATION_KEY, ASSIGNEE_LOGIN);
    userRule.createUser(OTHER_LOGIN, "pwd");
    provisionAndAnalyseProject(SAMPLE_PROJECT_KEY, ORGANIZATION_KEY);
    IssuesPage page = nav.logIn().asAdmin().openIssues();
    page
      .bulkChangeOpen()
      .bulkChangeAssigneeSearchCount(ASSIGNEE_LOGIN, 1)
      .bulkChangeAssigneeSearchCount(OTHER_LOGIN, 1);
  }

  private void createOrganization(String organizationKey) {
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(organizationKey).setName(organizationKey).build()).getOrganization();
  }

  private void provisionAndAnalyseProject(String projectKey, String organization) {
    provisionProject(projectKey, organization);
    analyseProject(projectKey, organization);
  }

  private void provisionProject(String projectKey, String organization) {
    adminClient.projects().create(
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
    adminClient.qualityProfiles().addProject(
      AddProjectRequest.builder()
        .setProjectKey(projectKey)
        .setOrganization(organization)
        .setLanguage("xoo")
        .setProfileName("one-issue-per-file-profile")
        .build());
  }
}
