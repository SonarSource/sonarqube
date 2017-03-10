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
package org.sonar.server.qualityprofile.ws;

import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QualityProfileDto;
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

public class SetDefaultActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";
  private WsActionTester tester;
  private DefaultOrganizationProvider defaultOrganizationProvider;
  private DbClient dbClient;
  private QProfileWsSupport wsSupport;
  private OrganizationDto organization;
  private SetDefaultAction underTest;

  /** Single, default quality profile for language xoo1 */
  private QualityProfileDto xoo1Profile;
  /** Parent quality profile for language xoo2 (not a default) */
  private QualityProfileDto xoo2Profile;
  /** Child quality profile for language xoo2, set as default */
  private QualityProfileDto xoo2Profile2;

  @Before
  public void setUp() {
    defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
    dbClient = db.getDbClient();
    wsSupport = new QProfileWsSupport(dbClient, userSessionRule, defaultOrganizationProvider);
    organization = OrganizationTesting.newOrganizationDto();
    db.organizations().insert(organization);
    underTest = new SetDefaultAction(LanguageTesting.newLanguages(xoo1Key, xoo2Key), dbClient, userSessionRule, wsSupport);

    String organizationUuid = organization.getUuid();
    xoo1Profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationUuid)
      .setLanguage(xoo1Key)
      .setDefault(true);
    xoo2Profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationUuid)
      .setLanguage(xoo2Key);
    xoo2Profile2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationUuid)
      .setLanguage(xoo2Key)
      .setParentKee(xoo2Profile.getKee())
      .setDefault(true);
    dbClient.qualityProfileDao().insert(db.getSession(), xoo1Profile, xoo2Profile, xoo2Profile2);
    db.commit();

    tester = new WsActionTester(underTest);
  }

  @Test
  public void set_default_profile_using_key() throws Exception {
    logInAsQProfileAdministrator();

    checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
    checkDefaultProfile(organization, xoo2Key, xoo2Profile2.getKey());

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", xoo2Profile.getKee()).execute();

    assertThat(response.getInput()).isEmpty();

    checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
    checkDefaultProfile(organization, xoo2Key, xoo2Profile.getKee());

    // One more time!
    TestResponse response2 = tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", xoo2Profile.getKee()).execute();

    assertThat(response2.getInput()).isEmpty();
    checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
    checkDefaultProfile(organization, xoo2Key, xoo2Profile.getKee());
  }

  @Test
  public void set_default_profile_using_language_and_name() throws Exception {
    logInAsQProfileAdministrator();

    checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
    checkDefaultProfile(organization, xoo2Key, xoo2Profile2.getKey());

    TestResponse response = tester.newRequest().setMethod("POST")
      .setParam("language", xoo2Profile.getLanguage())
      .setParam("profileName", xoo2Profile.getName())
      .setParam("organization", organization.getKey())
      .execute();

    assertThat(response.getInput()).isEmpty();

    checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
    checkDefaultProfile(organization, xoo2Key, xoo2Profile.getKee());
  }

  @Test
  public void should_not_change_other_organizations() throws Exception {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();

    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization1.getUuid());

    QualityProfileDto profileOrg1Old = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization1.getUuid())
      .setLanguage(xoo1Key)
      .setDefault(true);
    QualityProfileDto profileOrg1New = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization1.getUuid())
      .setLanguage(xoo1Key)
      .setDefault(false);
    QualityProfileDto profileOrg2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization2.getUuid())
      .setLanguage(xoo1Key)
      .setDefault(true);
    db.qualityProfiles().insertQualityProfiles(profileOrg1Old, profileOrg1New, profileOrg2);

    checkDefaultProfile(organization1, xoo1Key, profileOrg1Old.getKey());
    checkDefaultProfile(organization2, xoo1Key, profileOrg2.getKey());

    TestResponse response = tester.newRequest().setMethod("POST")
      .setParam("language", profileOrg1New.getLanguage())
      .setParam("profileName", profileOrg1New.getName())
      .setParam("organization", organization1.getKey())
      .execute();

    assertThat(response.getInput()).isEmpty();
    assertThat(response.getStatus()).isEqualTo(204);

    checkDefaultProfile(organization1, xoo1Key, profileOrg1New.getKey());
    checkDefaultProfile(organization2, xoo1Key, profileOrg2.getKee());
  }

  @Test
  public void fail_to_set_default_profile_using_invalid_key() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'unknown-profile-666' does not exist");

    tester.newRequest().setMethod("POST")
      .setParam("profileKey", "unknown-profile-666")
      .execute();

    checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
    checkDefaultProfile(organization, xoo2Key, xoo2Profile2.getKey());
  }

  @Test
  public void fail_to_set_default_profile_using_language_and_invalid_name() throws Exception {
    logInAsQProfileAdministrator();

    try {
      TestResponse response = tester.newRequest().setMethod("POST")
        .setParam("language", xoo2Key)
        .setParam("profileName", "Unknown")
        .execute();
      Fail.failBecauseExceptionWasNotThrown(NotFoundException.class);
    } catch (NotFoundException nfe) {
      assertThat(nfe).hasMessage("Quality Profile for language 'xoo2' and name 'Unknown' does not exist");
      checkDefaultProfile(organization, xoo1Key, xoo1Profile.getKey());
      checkDefaultProfile(organization, xoo2Key, xoo2Profile2.getKey());
    }
  }

  @Test
  public void fail_if_parameter_profile_key_is_combined_with_parameter_organization() throws Exception {
    userSessionRule.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("When providing a quality profile key, neither of organization/language/name must be set");

    tester.newRequest().setMethod("POST")
      .setParam("profileKey", xoo2Profile.getKee())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest().setMethod("POST")
      .setParam("profileKey", xoo2Profile.getKee())
      .execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest().setMethod("POST")
      .setParam("profileKey", xoo2Profile.getKee())
      .execute();
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization.getUuid());
  }

  private void checkDefaultProfile(OrganizationDto organization, String language, String key) {
    assertThat(dbClient.qualityProfileDao().selectDefaultProfile(db.getSession(), organization, language).getKey()).isEqualTo(key);
  }
}
