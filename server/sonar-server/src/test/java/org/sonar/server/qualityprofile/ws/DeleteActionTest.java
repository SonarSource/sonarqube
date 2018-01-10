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

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class DeleteActionTest {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);

  private DeleteAction underTest = new DeleteAction(
    new Languages(LanguageTesting.newLanguage(A_LANGUAGE)),
    new QProfileFactoryImpl(dbClient, UuidFactoryFast.getInstance(), System2.INSTANCE, activeRuleIndexer), dbClient, userSession,
    new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(db)));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void delete_profile_by_key() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    QProfileDto profile1 = createProfile(organization);
    QProfileDto profile2 = createProfile(organization);
    db.qualityProfiles().associateWithProject(project, profile1);

    logInAsQProfileAdministrator(organization);

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, profile1.getKee())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1, organization);
    verifyProfileExists(profile2);
  }

  @Test
  public void delete_profile_by_language_and_name_in_default_organization() {
    OrganizationDto organization = db.getDefaultOrganization();
    ComponentDto project = db.components().insertPrivateProject(organization);
    QProfileDto profile1 = createProfile(organization);
    QProfileDto profile2 = createProfile(organization);
    db.qualityProfiles().associateWithProject(project, profile1);

    logInAsQProfileAdministrator(organization);

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile1.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile1.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1, organization);
    verifyProfileExists(profile2);
  }

  @Test
  public void delete_profile_by_language_and_name_in_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    QProfileDto profile1 = createProfile(organization);
    QProfileDto profile2 = createProfile(organization);
    db.qualityProfiles().associateWithProject(project, profile1);
    logInAsQProfileAdministrator(organization);

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_LANGUAGE, profile1.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile1.getName())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1, organization);
    verifyProfileExists(profile2);
  }

  @Test
  public void as_qprofile_editor() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = createProfile(organization);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn(user);

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile, organization);
  }

  @Test
  public void fail_if_built_in_profile() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile1 = db.qualityProfiles().insert(organization, p -> p.setIsBuiltIn(true));
    logInAsQProfileAdministrator(organization);

    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, profile1.getKee())
      .execute();
  }

  @Test
  public void fail_if_not_profile_administrator() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto qprofile = createProfile(organization);
    userSession.logIn(db.users().insertUser());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, qprofile.getKee())
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    QProfileDto profile = createProfile(db.getDefaultOrganization());

    expectedException.expect(UnauthorizedException.class);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, profile.getKee())
      .execute();
  }

  @Test
  public void fail_if_missing_parameters() {
    userSession.logIn();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    ws.newRequest()
      .setMethod("POST")
      .execute();
  }

  @Test
  public void fail_if_missing_language_parameter() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = createProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam("profileName", profile.getName())
      .execute();
  }

  @Test
  public void fail_if_missing_name_parameter() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = createProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .execute();
  }

  @Test
  public void fail_if_too_many_parameters_to_reference_profile() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = createProfile(organization);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("When providing a quality profile key, neither of organization/language/name must be set");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam("profileName", profile.getName())
      .setParam(PARAM_KEY, profile.getKee())
      .execute();
  }

  @Test
  public void fail_if_profile_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'does_not_exist' does not exist");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, "does_not_exist")
      .execute();
  }

  @Test
  public void fail_if_deleting_default_profile() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = createProfile(organization);
    db.qualityProfiles().setAsDefault(profile);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile '" + profile.getName() + "' cannot be deleted because it is marked as default");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, profile.getKee())
      .execute();
  }

  @Test
  public void fail_if_a_descendant_is_marked_as_default() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto parentProfile = createProfile(organization);
    QProfileDto childProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE).setParentKee(parentProfile.getKee()));
    db.qualityProfiles().setAsDefault(childProfile);
    logInAsQProfileAdministrator(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile '" + parentProfile.getName() + "' cannot be deleted because its descendant named '" + childProfile.getName() +
      "' is marked as default");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, parentProfile.getKee())
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("language", "organization", "key", "qualityProfile");
    Param key = definition.param("key");
    assertThat(key.deprecatedKey()).isEqualTo("profileKey");
    assertThat(key.deprecatedSince()).isEqualTo("6.6");
    Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNullOrEmpty();
    Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  private void logInAsQProfileAdministrator(OrganizationDto organization) {
    userSession
      .logIn(db.users().insertUser())
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }

  private void verifyProfileDoesNotExist(QProfileDto profile, OrganizationDto organization) {
    assertThat(dbClient.qualityProfileDao().selectByUuid(dbSession, profile.getKee())).isNull();
    assertThat(dbClient.qualityProfileDao().selectSelectedProjects(dbSession, organization, profile, null)).isEmpty();
  }

  private void verifyProfileExists(QProfileDto profile) {
    assertThat(dbClient.qualityProfileDao().selectByUuid(dbSession, profile.getKee())).isNotNull();
  }

  private QProfileDto createProfile(OrganizationDto organization) {
    return db.qualityProfiles().insert(organization, p -> p.setLanguage(A_LANGUAGE));
  }
}
