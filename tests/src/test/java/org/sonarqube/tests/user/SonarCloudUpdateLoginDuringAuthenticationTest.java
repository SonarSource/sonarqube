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
package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.TesterSession;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.ce.ActivityRequest;
import org.sonarqube.ws.client.ce.TaskRequest;
import org.sonarqube.ws.client.custommeasures.CreateRequest;
import org.sonarqube.ws.client.issues.AddCommentRequest;
import org.sonarqube.ws.client.issues.AssignRequest;
import org.sonarqube.ws.client.organizations.AddMemberRequest;
import org.sonarqube.ws.client.organizations.SearchRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.qualityprofiles.ChangelogRequest;
import org.sonarqube.ws.client.rules.UpdateRequest;
import org.sonarqube.ws.client.settings.SetRequest;
import org.sonarqube.ws.client.settings.ValuesRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static util.ItUtils.extractCeTaskId;
import static util.ItUtils.projectDir;

public class SonarCloudUpdateLoginDuringAuthenticationTest {

  @ClassRule
  public static Orchestrator orchestrator = SonarCloudUserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    // enable the fake authentication plugin
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.enabled", "true");
  }

  @After
  public void tearDown() {
    tester.settings().resetSettings("sonar.auth.fake-base-id-provider.enabled", "sonar.auth.fake-base-id-provider.user");
  }

  @Test
  public void update_login_and_personal_organization_when_authenticating_existing_user() {
    tester.settings().setGlobalSettings("sonar.organizations.createPersonalOrg", "true");
    String oldLogin = tester.users().generateLogin();
    String providerId = tester.users().generateProviderId();
    authenticate(oldLogin, providerId);
    assertThat(tester.organizations().service().search(new SearchRequest()).getOrganizationsList())
      .extracting(Organization::getKey)
      .contains(oldLogin);

    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);

    assertThat(tester.users().service().search(new org.sonarqube.ws.client.users.SearchRequest()).getUsersList())
      .extracting(Users.SearchWsResponse.User::getLogin)
      .contains(newLogin)
      .doesNotContainSequence(oldLogin);
    assertThat(tester.organizations().service().search(new SearchRequest()).getOrganizationsList())
      .extracting(Organization::getKey)
      .contains(newLogin)
      .doesNotContain(oldLogin);
  }

  @Test
  public void update_login_and_personal_organization_during_auth_by_updating_only_one_letter_from_lower_case_to_upper_case() {
    String baseLogin = tester.users().generateLogin();
    String loginHavingLowerCase = baseLogin + "_letter";
    String providerId = tester.users().generateProviderId();
    authenticate(loginHavingLowerCase, providerId);
    assertThat(tester.organizations().service().search(new SearchRequest()).getOrganizationsList())
      .extracting(Organization::getKey)
      .contains(loginHavingLowerCase);

    String loginHavingUpperCase = baseLogin + "_Letter";
    authenticate(loginHavingUpperCase, providerId);

    assertThat(tester.users().service().search(new org.sonarqube.ws.client.users.SearchRequest()).getUsersList())
      .extracting(Users.SearchWsResponse.User::getLogin)
      .contains(loginHavingUpperCase)
      .doesNotContainSequence(loginHavingLowerCase);
    assertThat(tester.organizations().service().search(new SearchRequest()).getOrganizationsList())
      .extracting(Organization::getKey)
      // Organization key is still using key having lowercase
      .contains(loginHavingLowerCase)
      .doesNotContain(loginHavingUpperCase);
  }

  @Test
  public void issue_is_still_assigned_after_login_update() {
    String oldLogin = tester.users().generateLogin();
    String providerId = tester.users().generateProviderId();

    // Create user using authentication, and set user as member of the organization
    authenticate(oldLogin, providerId);
    Organization organization = tester.organizations().generate();
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));

    // Execute analysis and assign an issue to the user
    Projects.CreateWsResponse.Project project = tester.projects().provision(organization);
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles().assignQProfileToProject(profile, project);
    tester.qProfiles().activateRule(profile.getKey(), "xoo:OneIssuePerLine");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", project.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin"));
    Issue issue = tester.wsClient().issues().search(new org.sonarqube.ws.client.issues.SearchRequest().setOrganization(organization.getKey())).getIssuesList().get(0);
    tester.wsClient().issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(oldLogin));
    assertThat(getIssue(organization, issue.getKey()).getAssignee()).isEqualTo(oldLogin);

    // Update login during authentication, check issue is assigned to new login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    assertThat(getIssue(organization, issue.getKey()).getAssignee()).isEqualTo(newLogin);
  }

  @Test
  public void issue_changes_after_login_update() {
    String oldLogin = tester.users().generateLogin();
    String providerId = tester.users().generateProviderId();

    // Create user using authentication
    authenticate(oldLogin, providerId);
    Organization organization = tester.organizations().generate();
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));
    String userToken = tester.wsClient().userTokens().generate(new GenerateRequest().setLogin(oldLogin).setName("token")).getToken();
    WsClient userWsClient = tester.as(userToken, null).wsClient();

    // Execute analysis, then the user assign an issue to himself and add a comment
    Projects.CreateWsResponse.Project project = tester.projects().provision(organization);
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles().assignQProfileToProject(profile, project);
    tester.qProfiles().activateRule(profile.getKey(), "xoo:OneIssuePerLine");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", project.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin"));
    Issue issue = tester.wsClient().issues().search(new org.sonarqube.ws.client.issues.SearchRequest().setOrganization(organization.getKey())).getIssuesList().get(0);
    userWsClient.issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(oldLogin));
    userWsClient.issues().addComment(new AddCommentRequest().setIssue(issue.getKey()).setText("some comment"));

    // Comment and changelog contain old login
    assertThat(getIssue(organization, issue.getKey()).getComments().getComments(0).getLogin()).isEqualTo(oldLogin);
    assertThat(tester.wsClient().issues().changelog(new org.sonarqube.ws.client.issues.ChangelogRequest().setIssue(issue.getKey()))
      .getChangelog(0).getUser()).isEqualTo(oldLogin);

    // Update login during authentication, check comment and issue changelog contain new login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    assertThat(getIssue(organization, issue.getKey()).getComments().getComments(0).getLogin()).isEqualTo(newLogin);
    assertThat(tester.wsClient().issues().changelog(new org.sonarqube.ws.client.issues.ChangelogRequest().setIssue(issue.getKey()))
      .getChangelog(0).getUser()).isEqualTo(newLogin);
  }

  @Test
  public void default_assignee_login_is_updated_after_login_update() {
    String oldLogin = tester.users().generateLogin();
    String providerId = tester.users().generateProviderId();

    // Create user using authentication, and set user as member of the organization
    authenticate(oldLogin, providerId);
    Organization organization = tester.organizations().generate();
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));

    // Set default assignee on project, and execute analysis
    Projects.CreateWsResponse.Project project = tester.projects().provision(organization);
    tester.wsClient().settings().set(new SetRequest().setKey("sonar.issues.defaultAssigneeLogin").setComponent(project.getKey()).setValue(oldLogin));
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles().assignQProfileToProject(profile, project);
    tester.qProfiles().activateRule(profile.getKey(), "xoo:OneIssuePerLine");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", project.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin"));
    tester.wsClient().issues().search(new org.sonarqube.ws.client.issues.SearchRequest().setOrganization(organization.getKey())).getIssuesList()
      .forEach(i -> assertThat(getIssue(organization, i.getKey()).getAssignee()).isEqualTo(oldLogin));

    // Update login during authentication, check new login is the default assignee
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    assertThat(tester.wsClient().settings().values(new ValuesRequest().setKeys(singletonList("sonar.issues.defaultAssigneeLogin")).setComponent(project.getKey()))
      .getSettingsList())
        .extracting(Settings.Setting::getValue)
        .containsExactlyInAnyOrder(newLogin);
  }

  @Test
  public void qprofile_changes_after_login_update() throws JSONException {
    tester.settings().setGlobalSettings("sonar.organizations.anyoneCanCreate", "true");
    String providerId = tester.users().generateProviderId();
    String oldLogin = tester.users().generateLogin();
    authenticate(oldLogin, providerId);

    // Activate a rule on a new quality profile
    String userToken = tester.wsClient().userTokens().generate(new GenerateRequest().setLogin(oldLogin).setName("token")).getToken();
    TesterSession userSession = tester.as(userToken, null);
    Organization organization = userSession.organizations().generate();
    Qualityprofiles.CreateWsResponse.QualityProfile qProfile = userSession.qProfiles().createXooProfile(organization);
    userSession.qProfiles().activateRule(qProfile, "xoo:OneIssuePerLine");

    // Check changelog contain user login
    String changelog = tester.qProfiles().service().changelog(new ChangelogRequest()
      .setOrganization(organization.getKey())
      .setQualityProfile(qProfile.getName())
      .setLanguage(qProfile.getLanguage()));
    assertEquals(
      "{\n" +
        "  \"events\": [\n" +
        "    {\n" +
        "      \"ruleKey\": \"xoo:OneIssuePerLine\",\n" +
        "      \"authorLogin\": \"" + oldLogin + "\",\n" +
        "      \"action\": \"ACTIVATED\"\n" +
        "    }\n" +
        "  ]\n" +
        "}",
      changelog,
      false);

    // Update login during authentication, check changelog contains new user login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    String changelogReloaded = tester.qProfiles().service().changelog(new ChangelogRequest()
      .setOrganization(organization.getKey())
      .setQualityProfile(qProfile.getName())
      .setLanguage(qProfile.getLanguage()));
    assertEquals(
      "{\n" +
        "  \"events\": [\n" +
        "    {\n" +
        "      \"ruleKey\": \"xoo:OneIssuePerLine\",\n" +
        "      \"authorLogin\": \"" + newLogin + "\",\n" +
        "      \"action\": \"ACTIVATED\"\n" +
        "    }\n" +
        "  ]\n" +
        "}",
      changelogReloaded,
      false);
  }

  @Test
  public void user_tokens_after_login_update() {
    String providerId = tester.users().generateProviderId();
    String oldLogin = tester.users().generateLogin();

    // First authentication to create the user, then create a ws-client using a token
    authenticate(oldLogin, providerId);
    String userToken = tester.wsClient().userTokens().generate(new GenerateRequest().setLogin(oldLogin).setName("auth-token")).getToken();
    WsClient userWsClient = tester.as(userToken, null).wsClient();

    // Generate some user tokens
    userWsClient.userTokens().generate(new GenerateRequest().setName("token1"));
    userWsClient.userTokens().generate(new GenerateRequest().setName("token2"));
    assertThat(userWsClient.userTokens().search(new org.sonarqube.ws.client.usertokens.SearchRequest()).getUserTokensList())
      .extracting(UserTokens.SearchWsResponse.UserToken::getName)
      .contains("token1", "token2");

    // Update login during authentication, check user tokens are still there
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);

    assertThat(userWsClient.userTokens().search(new org.sonarqube.ws.client.usertokens.SearchRequest()).getUserTokensList())
      .extracting(UserTokens.SearchWsResponse.UserToken::getName)
      .contains("token1", "token2");
  }

  @Test
  public void manual_measure_after_login_update() throws JSONException {
    tester.settings().setGlobalSettings("sonar.organizations.anyoneCanCreate", "true");
    String providerId = tester.users().generateProviderId();
    String oldLogin = tester.users().generateLogin();

    // Create user using authentication
    authenticate(oldLogin, providerId);
    String userToken = tester.wsClient().userTokens().generate(new GenerateRequest().setLogin(oldLogin).setName("token")).getToken();
    WsClient userWsClient = tester.as(userToken, null).wsClient();

    // Grant user the admin permission on a project
    Organization organization = tester.organizations().generate();
    Project project = tester.projects().provision(organization);
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));
    String customMetricKey = randomAlphanumeric(50);
    tester.wsClient().metrics().create(new org.sonarqube.ws.client.metrics.CreateRequest().setKey(customMetricKey).setName("custom").setType("INT"));
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(oldLogin).setProjectKey(project.getKey()).setPermission("admin"));

    // Create a manual metric and a manual measure on it
    userWsClient.customMeasures().create(new CreateRequest().setMetricKey(customMetricKey).setProjectKey(project.getKey()).setValue("50"));
    String manualMeasures = tester.wsClient().customMeasures().search(new org.sonarqube.ws.client.custommeasures.SearchRequest().setProjectKey(project.getKey()));
    assertEquals(
      "{\n" +
        "  \"customMeasures\": [\n" +
        "    {\n" +
        "      \"projectKey\": \"" + project.getKey() + "\",\n" +
        "      \"user\": {\n" +
        "        \"login\": \"" + oldLogin + "\",\n" +
        "        \"name\": \"John\",\n" +
        "        \"email\": \"john@email.com\",\n" +
        "        \"active\": true\n" +
        "      }" +
        "    }" +
        "  ]\n" +
        "}",
      manualMeasures,
      false);

    // Update login during authentication, check manual measure contains new user login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    assertEquals(
      "{\n" +
        "  \"customMeasures\": [\n" +
        "    {\n" +
        "      \"projectKey\": \"" + project.getKey() + "\",\n" +
        "      \"user\": {\n" +
        "        \"login\": \"" + newLogin + "\",\n" +
        "        \"name\": \"John\",\n" +
        "        \"email\": \"john@email.com\",\n" +
        "        \"active\": true\n" +
        "      }" +
        "    }" +
        "  ]\n" +
        "}",
      tester.wsClient().customMeasures().search(new org.sonarqube.ws.client.custommeasures.SearchRequest().setProjectKey(project.getKey())),
      false);
  }

  @Test
  public void rule_note_login_after_login_update() {
    tester.settings().setGlobalSettings("sonar.organizations.anyoneCanCreate", "true");
    String providerId = tester.users().generateProviderId();
    String oldLogin = tester.users().generateLogin();

    // Create user using authentication
    authenticate(oldLogin, providerId);
    String userToken = tester.wsClient().userTokens().generate(new GenerateRequest().setLogin(oldLogin).setName("token")).getToken();
    WsClient userWsClient = tester.as(userToken, null).wsClient();

    // Grant user the qprofile admin permission on the organization
    Organization organization = tester.organizations().generate();
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(oldLogin).setOrganization(organization.getKey()).setPermission("profileadmin"));

    // Add a note on a rule
    userWsClient.rules().update(new UpdateRequest().setOrganization(organization.getKey()).setKey("xoo:OneIssuePerLine").setMarkdownNote("A user note"));
    assertThat(
      tester.wsClient().rules().search(new org.sonarqube.ws.client.rules.SearchRequest().setOrganization(organization.getKey()).setRuleKey("xoo:OneIssuePerLine")).getRulesList())
        .extracting(Rules.Rule::getKey, Rules.Rule::getNoteLogin, Rules.Rule::getMdNote)
        .containsExactlyInAnyOrder(tuple("xoo:OneIssuePerLine", oldLogin, "A user note"));

    // Update login during authentication, check rule note contains new user login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    assertThat(
      tester.wsClient().rules().search(new org.sonarqube.ws.client.rules.SearchRequest().setOrganization(organization.getKey()).setRuleKey("xoo:OneIssuePerLine")).getRulesList())
        .extracting(Rules.Rule::getKey, Rules.Rule::getNoteLogin, Rules.Rule::getMdNote)
        .containsExactlyInAnyOrder(tuple("xoo:OneIssuePerLine", newLogin, "A user note"));
  }

  @Test
  public void ce_task_and_activity_after_login_update() {
    String providerId = tester.users().generateProviderId();
    String oldLogin = tester.users().generateLogin();

    // Create user using authentication
    authenticate(oldLogin, providerId);
    String userToken = tester.wsClient().userTokens().generate(new GenerateRequest().setLogin(oldLogin).setName("token")).getToken();

    // Execute an analysis using user token
    Organization organization = tester.organizations().generate();
    Project project = tester.projects().provision(organization);
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(oldLogin).setOrganization(organization.getKey()).setPermission("scan"));
    String analysisTaskId = executeAnalysisOnSampleProject(organization, project, userToken);

    // Check login in ce task and activity
    assertThat(tester.wsClient().ce().task(new TaskRequest().setId(analysisTaskId)).getTask().getSubmitterLogin()).isEqualTo(oldLogin);
    assertThat(tester.wsClient().ce().activity(new ActivityRequest().setQ(analysisTaskId)).getTasksList())
      .extracting(Ce.Task::getComponentKey, Ce.Task::getSubmitterLogin)
      .containsExactlyInAnyOrder(tuple(project.getKey(), oldLogin));

    // Update login during authentication, check ce task and activity contains new login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    assertThat(tester.wsClient().ce().task(new TaskRequest().setId(analysisTaskId)).getTask().getSubmitterLogin()).isEqualTo(newLogin);
    assertThat(tester.wsClient().ce().activity(new ActivityRequest().setQ(analysisTaskId)).getTasksList())
      .extracting(Ce.Task::getComponentKey, Ce.Task::getSubmitterLogin)
      .containsExactlyInAnyOrder(tuple(project.getKey(), newLogin));
  }

  private void authenticate(String login, String providerId) {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.user", login + "," + providerId + ",fake-" + login + ",John,john@email.com");
    tester.wsClient().wsConnector().call(
      new GetRequest("/sessions/init/fake-base-id-provider"))
      .failIfNotSuccessful();
  }

  private static String executeAnalysisOnSampleProject(Organization organization, Project project, String userToken) {
    BuildResult buildResult = orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", project.getKey(),
      "sonar.login", userToken));
    return extractCeTaskId(buildResult);
  }

  private Issue getIssue(Organization organization, String issueKey) {
    List<Issue> issues = tester.wsClient().issues().search(new org.sonarqube.ws.client.issues.SearchRequest()
      .setIssues(singletonList(issueKey))
      .setOrganization(organization.getKey())
      .setAdditionalFields(singletonList("comments")))
      .getIssuesList();
    assertThat(issues).hasSize(1);
    return issues.get(0);
  }

}
