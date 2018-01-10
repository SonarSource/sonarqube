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
package org.sonar.server.qualityprofile.ws;

import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;

public class SetDefaultActionTest {

  private static final String XOO_1_KEY = "xoo1";
  private static final String XOO_2_KEY = "xoo2";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DefaultOrganizationProvider defaultOrganizationProvider;
  private DbClient dbClient;
  private QProfileWsSupport wsSupport;

  private SetDefaultAction underTest;
  private WsActionTester ws;

  private OrganizationDto organization;
  /** Single, default quality profile for language xoo1 */
  private QProfileDto xoo1Profile;
  /** Parent quality profile for language xoo2 (not a default) */
  private QProfileDto xoo2Profile;
  /** Child quality profile for language xoo2, set as default */
  private QProfileDto xoo2Profile2;

  @Before
  public void setUp() {
    defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
    dbClient = db.getDbClient();
    wsSupport = new QProfileWsSupport(dbClient, userSessionRule, defaultOrganizationProvider);
    organization = OrganizationTesting.newOrganizationDto();
    db.organizations().insert(organization);
    underTest = new SetDefaultAction(LanguageTesting.newLanguages(XOO_1_KEY, XOO_2_KEY), dbClient, userSessionRule, wsSupport);

    String organizationUuid = organization.getUuid();
    xoo1Profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationUuid)
      .setLanguage(XOO_1_KEY);
    xoo2Profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationUuid)
      .setLanguage(XOO_2_KEY);
    xoo2Profile2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationUuid)
      .setLanguage(XOO_2_KEY)
      .setParentKee(xoo2Profile.getKee());
    dbClient.qualityProfileDao().insert(db.getSession(), xoo1Profile, xoo2Profile, xoo2Profile2);
    db.commit();
    db.qualityProfiles().setAsDefault(xoo1Profile, xoo2Profile2);

    ws = new WsActionTester(underTest);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("key", "qualityProfile", "language", "organization");
    assertThat(definition.param("organization").since()).isEqualTo("6.4");
    Param profile = definition.param("key");
    assertThat(profile.deprecatedKey()).isEqualTo("profileKey");
    assertThat(definition.param("qualityProfile").deprecatedSince()).isNullOrEmpty();
    assertThat(definition.param("language").deprecatedSince()).isNullOrEmpty();
  }

  @Test
  public void set_default_profile_using_key() {
    logInAsQProfileAdministrator();

    checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile2.getKee());

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, xoo2Profile.getKee()).execute();

    assertThat(response.getInput()).isEmpty();

    checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile.getKee());

    // One more time!
    TestResponse response2 = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, xoo2Profile.getKee()).execute();

    assertThat(response2.getInput()).isEmpty();
    checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile.getKee());
  }

  @Test
  public void set_default_profile_using_language_and_name() {
    logInAsQProfileAdministrator();

    checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile2.getKee());

    TestResponse response = ws.newRequest().setMethod("POST")
      .setParam("language", xoo2Profile.getLanguage())
      .setParam("qualityProfile", xoo2Profile.getName())
      .setParam("organization", organization.getKey())
      .execute();

    assertThat(response.getInput()).isEmpty();

    checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile.getKee());
  }

  @Test
  public void should_not_change_other_organizations() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();

    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization1.getUuid());

    QProfileDto profileOrg1Old = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization1.getUuid())
      .setLanguage(XOO_1_KEY);
    QProfileDto profileOrg1New = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization1.getUuid())
      .setLanguage(XOO_1_KEY);
    QProfileDto profileOrg2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization2.getUuid())
      .setLanguage(XOO_1_KEY);
    db.qualityProfiles().insert(profileOrg1Old, profileOrg1New, profileOrg2);
    db.qualityProfiles().setAsDefault(profileOrg1Old, profileOrg2);

    checkDefaultProfile(organization1, XOO_1_KEY, profileOrg1Old.getKee());
    checkDefaultProfile(organization2, XOO_1_KEY, profileOrg2.getKee());

    TestResponse response = ws.newRequest().setMethod("POST")
      .setParam("language", profileOrg1New.getLanguage())
      .setParam("qualityProfile", profileOrg1New.getName())
      .setParam("organization", organization1.getKey())
      .execute();

    assertThat(response.getInput()).isEmpty();
    assertThat(response.getStatus()).isEqualTo(204);

    checkDefaultProfile(organization1, XOO_1_KEY, profileOrg1New.getKee());
    checkDefaultProfile(organization2, XOO_1_KEY, profileOrg2.getKee());
  }

  @Test
  public void fail_to_set_default_profile_using_invalid_key() {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'unknown-profile-666' does not exist");

    ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, "unknown-profile-666")
      .execute();

    checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile2.getKee());
  }

  @Test
  public void fail_to_set_default_profile_using_language_and_invalid_name() {
    logInAsQProfileAdministrator();

    try {
      TestResponse response = ws.newRequest().setMethod("POST")
        .setParam("language", XOO_2_KEY)
        .setParam("qualityProfile", "Unknown")
        .execute();
      Fail.failBecauseExceptionWasNotThrown(NotFoundException.class);
    } catch (NotFoundException nfe) {
      assertThat(nfe).hasMessage("Quality Profile for language 'xoo2' and name 'Unknown' does not exist");
      checkDefaultProfile(organization, XOO_1_KEY, xoo1Profile.getKee());
      checkDefaultProfile(organization, XOO_2_KEY, xoo2Profile2.getKee());
    }
  }

  @Test
  public void fail_if_parameter_profile_key_is_combined_with_parameter_organization() {
    userSessionRule.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("When providing a quality profile key, neither of organization/language/name must be set");

    ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, xoo2Profile.getKee())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, xoo2Profile.getKee())
      .execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, xoo2Profile.getKee())
      .execute();
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization.getUuid());
  }

  private void checkDefaultProfile(OrganizationDto organization, String language, String key) {
    assertThat(dbClient.qualityProfileDao().selectDefaultProfile(db.getSession(), organization, language).getKee()).isEqualTo(key);
  }
}
