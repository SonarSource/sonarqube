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
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.issues.AssignRequest;
import org.sonarqube.ws.client.organizations.AddMemberRequest;
import org.sonarqube.ws.client.organizations.SearchRequest;
import org.sonarqube.ws.client.qualityprofiles.ChangelogRequest;
import org.sonarqube.ws.client.settings.SetRequest;
import org.sonarqube.ws.client.settings.ValuesRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
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

    // Create user using authentication
    authenticate(oldLogin, providerId);

    // Set user as member of the organization
    Organization organization = tester.organizations().generate();
    tester.organizations().service().addMember(new AddMemberRequest().setOrganization(organization.getKey()).setLogin(oldLogin));
    Projects.CreateWsResponse.Project project = tester.projects().provision(organization);
    Qualityprofiles.CreateWsResponse.QualityProfile profile = tester.qProfiles().createXooProfile(organization);
    tester.qProfiles().assignQProfileToProject(profile, project);
    tester.qProfiles().activateRule(profile.getKey(), "xoo:OneIssuePerLine");

    // Execute project and assignee an issue to the user
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", project.getKey(),
      "sonar.login", "admin",
      "sonar.password", "admin"));
    Issues.Issue issue = tester.wsClient().issues().search(new org.sonarqube.ws.client.issues.SearchRequest().setOrganization(organization.getKey())).getIssuesList().get(0);
    tester.wsClient().issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(oldLogin));

    // Update login during authentication, check issue is assigned to new login
    String newLogin = tester.users().generateLogin();
    authenticate(newLogin, providerId);
    tester.wsClient().issues().assign(new AssignRequest().setIssue(issue.getKey()).setAssignee(newLogin));
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

  private void authenticate(String login, String providerId) {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.user", login + "," + providerId + ",fake-" + login + ",John,john@email.com");
    tester.wsClient().wsConnector().call(
      new GetRequest("/sessions/init/fake-base-id-provider"))
      .failIfNotSuccessful();
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
