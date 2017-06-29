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
import java.util.function.Predicate;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.tests.Session;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.qualityprofile.ChangeParentRequest;
import org.sonarqube.ws.client.qualityprofile.CopyRequest;
import org.sonarqube.ws.client.qualityprofile.QualityProfilesService;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import org.sonarqube.ws.client.qualityprofile.SetDefaultRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.expectBadRequestError;

public class BuiltInQualityProfilesTest {
  private static final String RULE_ONE_BUG_PER_LINE = "xoo:OneBugIssuePerLine";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void built_in_profiles_are_available_in_new_organization() {
    Organization org = tester.organizations().generate();
    SearchWsResponse result = tester.qProfiles().service().search(new SearchWsRequest().setOrganizationKey(org.getKey()));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("Basic", "xoo", true, true),
        tuple("Sonar way", "xoo", true, false),
        tuple("empty", "xoo", true, false),
        tuple("Basic", "xoo2", true, true),
        tuple("Sonar way", "xoo2", true, false));
  }

  @Test
  public void built_in_profiles_are_available_in_default_organization() {
    SearchWsResponse result = tester.qProfiles().service().search(new SearchWsRequest().setOrganizationKey("default-organization"));

    assertThat(result.getProfilesList())
      .extracting(QualityProfile::getOrganization, QualityProfile::getName, QualityProfile::getLanguage, QualityProfile::getIsBuiltIn, QualityProfile::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple("default-organization", "Basic", "xoo", true, true),
        tuple("default-organization", "Sonar way", "xoo", true, false),
        tuple("default-organization", "empty", "xoo", true, false),
        tuple("default-organization", "Basic", "xoo2", true, true),
        tuple("default-organization", "Sonar way", "xoo2", true, false));
  }

  @Test
  public void cannot_delete_built_in_profile_even_when_not_the_default_profile() {
    Organization org = tester.organizations().generate();
    QualityProfile builtInProfile = getProfile(org, p -> p.getIsBuiltIn() && p.getIsDefault() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));

    CreateWsResponse.QualityProfile profileInOrg = tester.qProfiles().createXooProfile(org);
    tester.qProfiles().service().setDefault(new SetDefaultRequest(profileInOrg.getKey()));

    expectBadRequestError(() -> tester.qProfiles().service().delete(builtInProfile.getKey()));
  }

  @Test
  public void built_in_profile_cannot_be_modified() {
    Organization org = tester.organizations().generate();
    QualityProfile builtInProfile = getProfile(org, p -> p.getIsBuiltIn() && p.getIsDefault() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));

    QualityProfilesService service = tester.qProfiles().service();
    expectBadRequestError(() -> tester.qProfiles().activateRule(builtInProfile.getKey(), RULE_ONE_BUG_PER_LINE));
    expectBadRequestError(() -> service.deactivateRule(builtInProfile.getKey(), RULE_ONE_BUG_PER_LINE));
    expectBadRequestError(() -> service.delete(builtInProfile.getKey()));
  }

  @Test
  public void copy_built_in_profile_to_a_custom_profile() {
    Organization org = tester.organizations().generate();
    User administrator = tester.users().generateAdministrator(org);
    QualityProfile builtInProfile = getProfile(org, p -> p.getIsBuiltIn() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));
    Session adminSession = tester.as(administrator.getLogin());

    QualityProfiles.CopyWsResponse copyResponse = adminSession.qProfiles().service().copy(new CopyRequest(builtInProfile.getKey(), "My copy"));

    assertThat(copyResponse.getIsDefault()).isFalse();
    assertThat(copyResponse.getKey()).isNotEmpty().isNotEqualTo(builtInProfile.getKey());
    assertThat(copyResponse.getLanguage()).isEqualTo(builtInProfile.getLanguage());
    assertThat(copyResponse.getName()).isEqualTo("My copy");
    assertThat(copyResponse.getIsInherited()).isFalse();

    QualityProfile copy = getProfile(org, p -> "My copy".equals(p.getName()) && "xoo".equals(p.getLanguage()));
    assertThat(copy.getIsBuiltIn()).isFalse();
    assertThat(copy.getIsDefault()).isFalse();
    assertThat(builtInProfile.getActiveRuleCount()).isGreaterThan(0);
    adminSession.qProfiles().assertThatNumberOfActiveRulesEqualsTo(copy.getKey(), (int) builtInProfile.getActiveRuleCount());
  }

  @Test
  public void can_inherit_and_disinherit_from_built_in_profile_to_a_custom_profile() {
    Organization org = tester.organizations().generate();
    User administrator = tester.users().generateAdministrator(org);
    QualityProfile builtInProfile = getProfile(org, p -> p.getIsBuiltIn() && "Basic".equals(p.getName()) && "xoo".equals(p.getLanguage()));
    Session adminSession = tester.as(administrator.getLogin());

    QualityProfiles.CopyWsResponse copyResponse = adminSession.qProfiles().service().copy(new CopyRequest(builtInProfile.getKey(), "My copy"));
    adminSession.qProfiles().service().changeParent(
      ChangeParentRequest.builder().setParentKey(builtInProfile.getKey()).setProfileKey(copyResponse.getKey()).build());

    QualityProfile inheritedQualityPropfile = getProfile(org, p -> p.getKey().equals(copyResponse.getKey()));

    assertThat(inheritedQualityPropfile.getParentKey()).isEqualTo(builtInProfile.getKey());
    assertThat(inheritedQualityPropfile.getParentName()).isEqualTo(builtInProfile.getName());

    // Remove inheritance
    adminSession.qProfiles().service().changeParent(
      new ChangeParentRequest(ChangeParentRequest.builder().setProfileKey(inheritedQualityPropfile.getKey())));

    inheritedQualityPropfile = getProfile(org, p -> p.getKey().equals(copyResponse.getKey()));

    assertThat(inheritedQualityPropfile.getParentKey()).isEmpty();
    assertThat(inheritedQualityPropfile.getParentName()).isEmpty();
  }

  private QualityProfile getProfile(Organization organization, Predicate<QualityProfile> filter) {
    return tester.qProfiles().service().search(new SearchWsRequest()
      .setOrganizationKey(organization.getKey())).getProfilesList()
      .stream()
      .filter(filter)
      .findAny().orElseThrow(IllegalStateException::new);
  }
}
