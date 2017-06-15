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
package it.qualityProfile;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.http.HttpMethod;
import it.Category6Suite;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse.QualityProfile;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.qualityprofile.AddProjectRequest;
import org.sonarqube.ws.client.qualityprofile.ChangeParentRequest;
import org.sonarqube.ws.client.qualityprofile.CopyRequest;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import org.sonarqube.ws.client.qualityprofile.SetDefaultRequest;
import util.ItUtils;
import util.OrganizationRule;
import util.QualityProfileRule;
import util.QualityProfileSupport;
import util.user.UserRule;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.expectForbiddenError;
import static util.ItUtils.expectMissingError;
import static util.ItUtils.expectUnauthorizedError;
import static util.ItUtils.projectDir;

public class CustomQualityProfilesTest {

  private static final String A_PASSWORD = "a_password";

  private static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static OrganizationRule organizations = new OrganizationRule(orchestrator);
  private static QualityProfileRule profiles = new QualityProfileRule(orchestrator);
  private static UserRule users = new UserRule(orchestrator);

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(orchestrator)
    .around(organizations)
    .around(profiles)
    .around(users);

  @Test
  public void activation_of_rules_is_isolated_among_organizations() {
    // create two profiles with same names in two organizations
    Organization org1 = organizations.create();
    Organization org2 = organizations.create();
    QualityProfile profileInOrg1 = profiles.createXooProfile(org1, p -> p.setProfileName("foo"));
    QualityProfile profileInOrg2 = profiles.createXooProfile(org2, p -> p.setProfileName("foo"));

    profiles
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 0)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    profiles
      .activateRule(profileInOrg1, "xoo:OneIssuePerLine")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    profiles
      .activateRule(profileInOrg1, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 2)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    profiles
      .deactivateRule(profileInOrg1, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 0);

    profiles
      .activateRule(profileInOrg2, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 1);

    profiles
      .delete(profileInOrg1)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg1, 0)
      .assertThatNumberOfActiveRulesEqualsTo(profileInOrg2, 1);
  }

  @Test
  public void an_organization_administrator_can_manage_the_profiles_of_organization() {
    Organization org = organizations.create();
    User user = users.createAdministrator(org, A_PASSWORD);

    QualityProfileSupport adminProfiles = profiles.as(user.getLogin(), A_PASSWORD);
    QualityProfile profile = adminProfiles.createXooProfile(org);
    adminProfiles.assertThatNumberOfActiveRulesEqualsTo(profile, 0);

    adminProfiles
      .activateRule(profile, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profile, 1);

    adminProfiles
      .delete(profile)
      .assertThatNumberOfActiveRulesEqualsTo(profile, 0);
  }

  @Test
  public void deleting_an_organization_delete_all_profiles_on_this_organization() {
    Organization org = organizations.create();
    User user = users.createAdministrator(org, A_PASSWORD);

    QualityProfileSupport adminProfiles = profiles.as(user.getLogin(), A_PASSWORD);
    // Profile
    QualityProfile parentProfile = adminProfiles.createXooProfile(org);

    // Copied profile
    QualityProfiles.SearchWsResponse.QualityProfile builtInProfile = getProfile(org, p -> p.getIsBuiltIn() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));
    QualityProfiles.CopyWsResponse copyResponse = adminProfiles.getWsService().copy(new CopyRequest(builtInProfile.getKey(), "My copy"));

    // Inherited profile from custom
    QualityProfile inheritedProfile1 = adminProfiles.getWsService().create(
      CreateRequest.builder()
        .setLanguage(parentProfile.getLanguage())
        .setOrganizationKey(org.getKey())
        .setProfileName("inherited_profile")
        .build()).getProfile();

    adminProfiles.getWsService().changeParent(
      ChangeParentRequest.builder().setParentKey(parentProfile.getKey()).setProfileKey(inheritedProfile1.getKey()).build());

    // Inherited profile from builtIn
    QualityProfile inheritedProfile2 = adminProfiles.getWsService().create(
      CreateRequest.builder()
        .setLanguage(parentProfile.getLanguage())
        .setOrganizationKey(org.getKey())
        .setProfileName("inherited_profile2")
        .build()).getProfile();

    adminProfiles.getWsService().changeParent(
      ChangeParentRequest.builder().setParentKey(builtInProfile.getKey()).setProfileKey(inheritedProfile2.getKey()).build());

    organizations.delete(org);

    expectMissingError(() -> profiles.getWsService().search(new SearchWsRequest()
      .setOrganizationKey(org.getKey())).getProfilesList());

    profiles.getWsService().search(new SearchWsRequest()).getProfilesList().stream()
      .forEach(p -> {
        assertThat(p.getOrganization()).isNotEqualTo(org.getKey());
        assertThat(p.getKey()).isNotIn(parentProfile.getKey(), copyResponse.getKey(), inheritedProfile1.getKey(), inheritedProfile2.getKey());
      });
  }

  @Test
  public void an_organization_administrator_cannot_manage_the_profiles_of_other_organizations() {
    Organization org1 = organizations.create();
    Organization org2 = organizations.create();
    QualityProfile profileInOrg2 = profiles.createXooProfile(org2);
    User adminOfOrg1 = users.createAdministrator(org1, A_PASSWORD);

    QualityProfileSupport adminProfiles = profiles.as(adminOfOrg1.getLogin(), A_PASSWORD);

    expectForbiddenError(() -> adminProfiles.createXooProfile(org2));
    expectForbiddenError(() -> adminProfiles.delete(profileInOrg2));
    expectForbiddenError(() -> adminProfiles.activateRule(profileInOrg2, "xoo:OneIssuePerFile"));
    expectForbiddenError(() -> adminProfiles.deactivateRule(profileInOrg2, "xoo:OneIssuePerFile"));
  }

  @Test
  public void anonymous_cannot_manage_the_profiles_of_an_organization() {
    Organization org = organizations.create();
    QualityProfile profile = profiles.createXooProfile(org);

    QualityProfileSupport anonymousProfiles = profiles.asAnonymous();

    expectUnauthorizedError(() -> anonymousProfiles.createXooProfile(org));
    expectUnauthorizedError(() -> anonymousProfiles.delete(profile));
    expectUnauthorizedError(() -> anonymousProfiles.activateRule(profile, "xoo:OneIssuePerFile"));
    expectUnauthorizedError(() -> anonymousProfiles.deactivateRule(profile, "xoo:OneIssuePerFile"));
  }

  @Test
  public void root_can_manage_the_profiles_of_any_organization() {
    Organization org = organizations.create();

    User orgAdmin = users.createAdministrator(org, A_PASSWORD);
    QualityProfileSupport adminProfiles = profiles.as(orgAdmin.getLogin(), A_PASSWORD);
    QualityProfile profile = adminProfiles.createXooProfile(org);

    // root can activate rule and delete the profile
    profiles
      .activateRule(profile, "xoo:OneIssuePerFile")
      .assertThatNumberOfActiveRulesEqualsTo(profile, 1);
    profiles
      .delete(profile)
      .assertThatNumberOfActiveRulesEqualsTo(profile, 0);
  }

  @Test
  public void can_inherit_and_disinherit_and__from_another_custom_profile() {
    Organization org = organizations.create();
    User user = users.createAdministrator(org, A_PASSWORD);

    QualityProfileSupport adminProfiles = profiles.as(user.getLogin(), A_PASSWORD);
    QualityProfile parentProfile = adminProfiles.createXooProfile(org);
    QualityProfile inheritedProfile = adminProfiles.getWsService().create(
      CreateRequest.builder()
        .setLanguage(parentProfile.getLanguage())
        .setOrganizationKey(org.getKey())
        .setProfileName("inherited_profile")
        .build()).getProfile();

    adminProfiles.getWsService().changeParent(
      ChangeParentRequest.builder().setParentKey(parentProfile.getKey()).setProfileKey(inheritedProfile.getKey()).build());

    QualityProfiles.SearchWsResponse.QualityProfile inheritedQualityPropfile = getProfile(org, p -> p.getKey().equals(inheritedProfile.getKey()));

    assertThat(inheritedQualityPropfile.getParentKey()).isEqualTo(parentProfile.getKey());
    assertThat(inheritedQualityPropfile.getParentName()).isEqualTo(parentProfile.getName());

    // Remove inheritance
    adminProfiles.getWsService().changeParent(
      new ChangeParentRequest(ChangeParentRequest.builder().setProfileKey(inheritedQualityPropfile.getKey())));

    inheritedQualityPropfile = getProfile(org, p -> p.getKey().equals(inheritedProfile.getKey()));

    assertThat(inheritedQualityPropfile.getParentKey()).isEmpty();
    assertThat(inheritedQualityPropfile.getParentName()).isEmpty();
  }

  @Test
  public void analysis_must_use_default_profile() {
    Organization org = organizations.create();
    User user = users.createAdministrator(org, A_PASSWORD);

    QualityProfileSupport adminProfiles = profiles.as(user.getLogin(), A_PASSWORD);

    String projectKey = randomAlphanumeric(10);
    String projectName = randomAlphanumeric(10);
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"),
        "sonar.login", user.getLogin(),
        "sonar.password", A_PASSWORD,
        "sonar.organization", org.getKey())
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );

    QualityProfiles.SearchWsResponse.QualityProfile defaultProfile = getProfile(org, p -> "xoo".equals(p.getLanguage()) && p.getIsDefault());
    assertThatQualityProfileIsUsedFor(projectKey, defaultProfile.getKey());

    QualityProfile newXooProfile = adminProfiles.createXooProfile(org);
    profiles.getWsService().setDefault(new SetDefaultRequest(newXooProfile.getKey()));

    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"),
        "sonar.login", user.getLogin(),
        "sonar.password", A_PASSWORD,
        "sonar.organization", org.getKey())
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );

    assertThatQualityProfileIsUsedFor(projectKey, newXooProfile.getKey());
  }

  @Test
  public void analysis_must_use_associated_profile() {
    Organization org = organizations.create();
    User user = users.createAdministrator(org, A_PASSWORD);
    String projectKey = randomAlphanumeric(10);
    String projectName = randomAlphanumeric(10);
    QualityProfileSupport adminProfiles = profiles.as(user.getLogin(), A_PASSWORD);
    QualityProfile newXooProfile = adminProfiles.createXooProfile(org);

    orchestrator.getServer().newHttpCall("api/projects/create")
      .setCredentials(user.getLogin(), A_PASSWORD)
      .setParam("project", projectKey)
      .setParam("name", projectName)
      .setParam("organization", org.getKey())
      .setMethod(HttpMethod.POST)
      .execute();

    adminProfiles.getWsService().addProject(AddProjectRequest.builder()
      .setProfileKey(newXooProfile.getKey())
      .setProjectKey(projectKey)
      .build());

    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"),
        "sonar.login", user.getLogin(),
        "sonar.password", A_PASSWORD,
        "sonar.organization", org.getKey())
        .setProjectKey(projectKey)
        .setProjectName(projectName)
    );

    assertThatQualityProfileIsUsedFor(projectKey, newXooProfile.getKey());
  }

  private static void assertThatQualityProfileIsUsedFor(String projectKey, String qualityProfileKey) {
    Map components = ItUtils.jsonToMap(orchestrator.getServer().newHttpCall("api/navigation/component")
      .setParam("componentKey", projectKey)
      .execute().getBodyAsString());

    assertThat(((Map) ((List) components.get("qualityProfiles")).get(0)).get("key")).isEqualTo(qualityProfileKey);
  }

  private QualityProfiles.SearchWsResponse.QualityProfile getProfile(Organization organization, Predicate<QualityProfiles.SearchWsResponse.QualityProfile> filter) {
    return profiles.getWsService().search(new SearchWsRequest()
      .setOrganizationKey(organization.getKey())).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}
