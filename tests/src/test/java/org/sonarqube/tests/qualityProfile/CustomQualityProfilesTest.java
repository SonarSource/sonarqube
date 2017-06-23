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
package org.sonarqube.tests.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category6Suite;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.QProfileTester;
import org.sonarqube.tests.Session;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofile.ChangeParentRequest;
import org.sonarqube.ws.client.qualityprofile.CopyRequest;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import org.sonarqube.ws.client.qualityprofile.SetDefaultRequest;
import util.ItUtils;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectForbiddenError;
import static util.ItUtils.expectMissingError;
import static util.ItUtils.expectUnauthorizedError;
import static util.ItUtils.projectDir;

public class CustomQualityProfilesTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void activation_of_rules_is_isolated_among_organizations() {
    // create two profiles with same names in two organizations
    Organization org1 = tester.organizations().generate();
    Organization org2 = tester.organizations().generate();
    QualityProfile profileInOrg1 = tester.qProfiles().createXooProfile(org1, p -> p.setProfileName("foo"));
    QualityProfile profileInOrg2 = tester.qProfiles().createXooProfile(org2, p -> p.setProfileName("foo"));

    tester.qProfiles()
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 0)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    tester.qProfiles()
      .activateRule(profileInOrg1, "xoo:OneIssuePerLine")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    tester.qProfiles()
      .activateRule(profileInOrg1, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 2)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    tester.qProfiles()
      .deactivateRule(profileInOrg1, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    tester.qProfiles()
      .activateRule(profileInOrg2, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 1);

    delete(profileInOrg1);
    tester.qProfiles()
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 0)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 1);
  }

  @Test
  public void an_organization_administrator_can_manage_the_profiles_of_organization() {
    Organization org = tester.organizations().generate();
    User user = tester.users().generateAdministrator(org);

    QProfileTester adminSession = tester.as(user.getLogin()).qProfiles();
    QualityProfile profile = adminSession.createXooProfile(org);
    adminSession.assertThatNumberOfActiveRulesEqualsTo(profile, 0);

    adminSession
      .activateRule(profile, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profile, 1);

    adminSession.service().delete(profile.getKey());
    adminSession.assertThatNumberOfActiveRulesEqualsTo(profile, 0);
  }

  @Test
  public void deleting_an_organization_delete_all_profiles_on_this_organization() {
    Organization org = tester.organizations().generate();
    User user = tester.users().generateAdministrator(org);

    QProfileTester adminSession = tester.as(user.getLogin()).qProfiles();
    // Profile
    QualityProfile parentProfile = adminSession.createXooProfile(org);

    // Copied profile
    QualityProfiles.SearchWsResponse.QualityProfile builtInProfile = getProfile(org, p -> p.getIsBuiltIn() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));
    QualityProfiles.CopyWsResponse copyResponse = adminSession.service().copy(new CopyRequest(builtInProfile.getKey(), "My copy"));

    // Inherited profile from custom
    QualityProfile inheritedProfile1 = adminSession.service().create(
      CreateRequest.builder()
        .setLanguage(parentProfile.getLanguage())
        .setOrganizationKey(org.getKey())
        .setProfileName("inherited_profile")
        .build())
      .getProfile();

    adminSession.service().changeParent(
      ChangeParentRequest.builder().setParentKey(parentProfile.getKey()).setProfileKey(inheritedProfile1.getKey()).build());

    // Inherited profile from builtIn
    QualityProfile inheritedProfile2 = adminSession.service().create(
      CreateRequest.builder()
        .setLanguage(parentProfile.getLanguage())
        .setOrganizationKey(org.getKey())
        .setProfileName("inherited_profile2")
        .build())
      .getProfile();

    adminSession.service().changeParent(
      ChangeParentRequest.builder().setParentKey(builtInProfile.getKey()).setProfileKey(inheritedProfile2.getKey()).build());

    tester.organizations().service().delete(org.getKey());

    expectMissingError(() -> tester.qProfiles().service().search(new SearchWsRequest()
      .setOrganizationKey(org.getKey())));

    tester.qProfiles().service().search(new SearchWsRequest()).getProfilesList()
      .forEach(p -> {
        assertThat(p.getOrganization()).isNotEqualTo(org.getKey());
        assertThat(p.getKey()).isNotIn(parentProfile.getKey(), copyResponse.getKey(), inheritedProfile1.getKey(), inheritedProfile2.getKey());
      });
  }

  @Test
  public void an_organization_administrator_cannot_manage_the_profiles_of_other_organizations() {
    Organization org1 = tester.organizations().generate();
    Organization org2 = tester.organizations().generate();
    QualityProfile profileInOrg2 = tester.qProfiles().createXooProfile(org2);
    User adminOfOrg1 = tester.users().generateAdministrator(org1);

    QProfileTester adminSession = tester.as(adminOfOrg1.getLogin()).qProfiles();

    expectForbiddenError(() -> adminSession.createXooProfile(org2));
    expectForbiddenError(() -> adminSession.service().delete(profileInOrg2.getKey()));
    expectForbiddenError(() -> adminSession.activateRule(profileInOrg2, "xoo:OneIssuePerFile"));
    expectForbiddenError(() -> adminSession.deactivateRule(profileInOrg2, "xoo:OneIssuePerFile"));
  }

  private void delete(QualityProfile profile) {
    tester.qProfiles().service().delete(profile.getKey());
  }

  @Test
  public void anonymous_cannot_manage_the_profiles_of_an_organization() {
    Organization org = tester.organizations().generate();
    QualityProfile profile = tester.qProfiles().createXooProfile(org);

    Session anonymousSession = tester.asAnonymous();

    expectUnauthorizedError(() -> anonymousSession.qProfiles().createXooProfile(org));
    expectUnauthorizedError(() -> anonymousSession.qProfiles().service().delete(profile.getKey()));
    expectUnauthorizedError(() -> anonymousSession.qProfiles().activateRule(profile, "xoo:OneIssuePerFile"));
    expectUnauthorizedError(() -> anonymousSession.qProfiles().deactivateRule(profile, "xoo:OneIssuePerFile"));
  }

  @Test
  public void root_can_manage_the_profiles_of_any_organization() {
    Organization org = tester.organizations().generate();

    User orgAdmin = tester.users().generateAdministrator(org);
    Session adminSession = tester.as(orgAdmin.getLogin());
    QualityProfile profile = adminSession.qProfiles().createXooProfile(org);

    // root can activate rule and delete the profile
    tester.qProfiles()
      .activateRule(profile, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profile, 1);
    tester.qProfiles().service().delete(profile.getKey());
    tester.qProfiles().assertThatNumberOfActiveRulesEqualsTo(profile, 0);
  }

  @Test
  public void can_inherit_and_disinherit_and__from_another_custom_profile() {
    Organization org = tester.organizations().generate();
    User user = tester.users().generateAdministrator(org);

    Session adminSession = tester.as(user.getLogin());
    QualityProfile parentProfile = adminSession.qProfiles().createXooProfile(org);
    QualityProfile inheritedProfile = adminSession.qProfiles().service().create(
      CreateRequest.builder()
        .setLanguage(parentProfile.getLanguage())
        .setOrganizationKey(org.getKey())
        .setProfileName("inherited_profile")
        .build())
      .getProfile();

    adminSession.qProfiles().service().changeParent(
      ChangeParentRequest.builder().setParentKey(parentProfile.getKey()).setProfileKey(inheritedProfile.getKey()).build());

    QualityProfiles.SearchWsResponse.QualityProfile inheritedQualityPropfile = getProfile(org, p -> p.getKey().equals(inheritedProfile.getKey()));

    assertThat(inheritedQualityPropfile.getParentKey()).isEqualTo(parentProfile.getKey());
    assertThat(inheritedQualityPropfile.getParentName()).isEqualTo(parentProfile.getName());

    // Remove inheritance
    adminSession.qProfiles().service().changeParent(
      new ChangeParentRequest(ChangeParentRequest.builder().setProfileKey(inheritedQualityPropfile.getKey())));

    inheritedQualityPropfile = getProfile(org, p -> p.getKey().equals(inheritedProfile.getKey()));

    assertThat(inheritedQualityPropfile.getParentKey()).isEmpty();
    assertThat(inheritedQualityPropfile.getParentName()).isEmpty();
  }

  @Test
  public void analysis_must_use_default_profile() {
    Organization org = tester.organizations().generate();
    User admin = tester.users().generateAdministrator(org);

    Session adminSession = tester.as(admin.getLogin());

    String projectKey = randomAlphanumeric(10);
    String projectName = randomAlphanumeric(10);
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"),
        "sonar.login", admin.getLogin(),
        "sonar.password", admin.getLogin(),
        "sonar.organization", org.getKey())
        .setProjectKey(projectKey)
        .setProjectName(projectName));

    QualityProfiles.SearchWsResponse.QualityProfile defaultProfile = getProfile(org, p -> "xoo".equals(p.getLanguage()) &&
      p.getIsDefault());
    assertThatQualityProfileIsUsedFor(projectKey, defaultProfile.getKey());

    QualityProfile newXooProfile = adminSession.qProfiles().createXooProfile(org);
    adminSession.qProfiles().service().setDefault(new SetDefaultRequest(newXooProfile.getKey()));

    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"),
        "sonar.login", admin.getLogin(),
        "sonar.password", admin.getLogin(),
        "sonar.organization", org.getKey())
        .setProjectKey(projectKey)
        .setProjectName(projectName));

    assertThatQualityProfileIsUsedFor(projectKey, newXooProfile.getKey());
  }

  @Test
  public void analysis_must_use_associated_profile() {
    Organization org = tester.organizations().generate();
    User admin = tester.users().generateAdministrator(org);
    String projectKey = randomAlphanumeric(10);
    String projectName = randomAlphanumeric(10);
    Session adminSession = tester.as(admin.getLogin());
    QualityProfile newXooProfile = adminSession.qProfiles().createXooProfile(org);

    adminSession.wsClient().wsConnector().call(new PostRequest("api/projects/create")
      .setParam("project", projectKey)
      .setParam("name", projectName)
      .setParam("organization", org.getKey()));

    adminSession.qProfiles().service().addProject(AddProjectRequest.builder()
      .setProfileKey(newXooProfile.getKey())
      .setProjectKey(projectKey)
      .build());

    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"),
        "sonar.login", admin.getLogin(),
        "sonar.password", admin.getLogin(),
        "sonar.organization", org.getKey())
        .setProjectKey(projectKey)
        .setProjectName(projectName));

    assertThatQualityProfileIsUsedFor(projectKey, newXooProfile.getKey());
  }

  private void assertThatQualityProfileIsUsedFor(String projectKey, String qualityProfileKey) {
    GetRequest request = new GetRequest("api/navigation/component")
      .setParam("componentKey", projectKey);
    Map components = ItUtils.jsonToMap(tester.wsClient().wsConnector().call(request).content());

    assertThat(((Map) ((List) components.get("qualityProfiles")).get(0)).get("key")).isEqualTo(qualityProfileKey);
  }

  private QualityProfiles.SearchWsResponse.QualityProfile getProfile(Organization organization, Predicate<QualityProfiles.SearchWsResponse.QualityProfile> filter) {
    return tester.qProfiles().service().search(new SearchWsRequest()
      .setOrganizationKey(organization.getKey())).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}
