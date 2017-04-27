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

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class DeleteActionTest {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession session = dbTester.getSession();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private DeleteAction underTest = new DeleteAction(
    new Languages(LanguageTesting.newLanguage(A_LANGUAGE)),
    new QProfileFactory(dbClient, UuidFactoryFast.getInstance(), System2.INSTANCE, activeRuleIndexer),
    dbClient, userSessionRule,
    new QProfileWsSupport(dbClient, userSessionRule, TestDefaultOrganizationProvider.from(dbTester)));
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void delete_profile_by_key() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    QualityProfileDto profile1 = createProfile(organization);
    QualityProfileDto profile2 = createProfile(organization);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project, profile1);

    logInAsQProfileAdministrator(organization);

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", profile1.getKey())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1, organization);
    verifyProfileExists(profile2);
  }

  @Test
  public void delete_profile_by_language_and_name_in_default_organization() throws Exception {
    OrganizationDto organization = dbTester.getDefaultOrganization();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    QualityProfileDto profile1 = createProfile(organization);
    QualityProfileDto profile2 = createProfile(organization);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project, profile1);

    logInAsQProfileAdministrator(organization);

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("language", profile1.getLanguage())
      .setParam("profileName", profile1.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1, organization);
    verifyProfileExists(profile2);
  }

  @Test
  public void delete_profile_by_language_and_name_in_specified_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    QualityProfileDto profile1 = createProfile(organization);
    QualityProfileDto profile2 = createProfile(organization);
    dbTester.qualityProfiles().associateProjectWithQualityProfile(project, profile1);
    logInAsQProfileAdministrator(organization);

    TestResponse response = tester.newRequest()
      .setMethod("POST")
      .setParam("organization", organization.getKey())
      .setParam("language", profile1.getLanguage())
      .setParam("profileName", profile1.getName())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1, organization);
    verifyProfileExists(profile2);
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();

    QualityProfileDto profileInOrg1 = createProfile(organization1);
    QualityProfileDto profileInOrg2 = createProfile(organization2);

    logInAsQProfileAdministrator(organization1);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", profileInOrg2.getKey())
      .execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    QualityProfileDto profile = createProfile(dbTester.getDefaultOrganization());

    expectedException.expect(UnauthorizedException.class);

    tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", profile.getKey())
      .execute();
  }

  @Test
  public void throw_IAE_if_missing_parameters() {
    userSessionRule.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    tester.newRequest()
      .setMethod("POST")
      .execute();
  }

  @Test
  public void throw_IAE_if_missing_language_parameter() {
    OrganizationDto organization = dbTester.organizations().insert();
    QualityProfileDto profile = createProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    tester.newRequest()
      .setMethod("POST")
      .setParam("organization", organization.getKey())
      .setParam("profileName", profile.getName())
      .execute();
  }

  @Test
  public void throw_IAE_if_missing_name_parameter() throws Exception {
    OrganizationDto organization = dbTester.organizations().insert();
    QualityProfileDto profile = createProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    tester.newRequest()
      .setMethod("POST")
      .setParam("organization", organization.getKey())
      .setParam("language", profile.getLanguage())
      .execute();
  }

  @Test
  public void throw_IAE_if_too_many_parameters_to_reference_profile() {
    OrganizationDto organization = dbTester.organizations().insert();
    QualityProfileDto profile = createProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("When providing a quality profile key, neither of organization/language/name must be set");

    tester.newRequest()
      .setMethod("POST")
      .setParam("organization", organization.getKey())
      .setParam("language", profile.getLanguage())
      .setParam("profileName", profile.getName())
      .setParam("profileKey", profile.getKey())
      .execute();
  }

  @Test
  public void throw_NotFoundException_if_profile_does_not_exist() {
    userSessionRule.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'does_not_exist' does not exist");

    tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", "does_not_exist")
      .execute();
  }

  @Test
  public void throw_ISE_if_deleting_default_profile() {
    OrganizationDto organization = dbTester.organizations().insert();
    QualityProfileDto profile = createDefaultProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile '" + profile.getName() + "' cannot be deleted because it is marked as default");

    tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", profile.getKey())
      .execute();
  }

  @Test
  public void throw_ISE_if_a_descendant_is_marked_as_default() {
    OrganizationDto organization = dbTester.organizations().insert();
    QualityProfileDto parentProfile = createProfile(organization);
    QualityProfileDto childProfile = dbTester.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE), p -> p.setDefault(true),
      p -> p.setParentKee(parentProfile.getKey()));
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile '" + parentProfile.getName() + "' cannot be deleted because its descendant named '" + childProfile.getName() +
      "' is marked as default");

    tester.newRequest()
      .setMethod("POST")
      .setParam("profileKey", parentProfile.getKey())
      .execute();
  }

  private void logInAsQProfileAdministrator(OrganizationDto organization) {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }

  private void verifyProfileDoesNotExist(QualityProfileDto profile, OrganizationDto organization) {
    assertThat(dbClient.qualityProfileDao().selectByKey(session, profile.getKey())).isNull();
    assertThat(dbClient.qualityProfileDao().selectSelectedProjects(organization, profile.getKey(), null, session)).isEmpty();
  }

  private void verifyProfileExists(QualityProfileDto profile) {
    assertThat(dbClient.qualityProfileDao().selectByKey(session, profile.getKey())).isNotNull();
  }

  private QualityProfileDto createProfile(OrganizationDto organization) {
    return dbTester.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE), p -> p.setDefault(false));
  }

  private QualityProfileDto createDefaultProfile(OrganizationDto organization) {
    return dbTester.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE), p -> p.setDefault(true));
  }
}
