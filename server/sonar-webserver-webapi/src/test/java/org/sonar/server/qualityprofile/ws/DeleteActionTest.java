/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.db.project.ProjectDto;
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
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
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
  public void delete_profile_by_language_and_name_in_default_organization() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile1 = createProfile();
    QProfileDto profile2 = createProfile();
    db.qualityProfiles().associateWithProject(project, profile1);

    logInAsQProfileAdministrator();

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile1.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile1.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1);
    verifyProfileExists(profile2);
  }

  @Test
  public void delete_profile_by_language_and_name_in_specified_organization() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    QProfileDto profile1 = createProfile();
    QProfileDto profile2 = createProfile();
    db.qualityProfiles().associateWithProject(project, profile1);
    logInAsQProfileAdministrator();

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile1.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile1.getName())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile1);
    verifyProfileExists(profile2);
  }

  @Test
  public void as_qprofile_editor() {
    QProfileDto profile = createProfile();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn(user);

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);

    verifyProfileDoesNotExist(profile);
  }

  @Test
  public void fail_if_built_in_profile() {
    QProfileDto profile1 = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true).setLanguage(A_LANGUAGE));
    logInAsQProfileAdministrator();

    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile1.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile1.getName())
      .execute();
  }

  @Test
  public void fail_if_not_profile_administrator() {
    QProfileDto qprofile = createProfile();
    userSession.logIn(db.users().insertUser());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, qprofile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, qprofile.getName())
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    QProfileDto profile = createProfile();

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
    expectedException.expectMessage("The 'language' parameter is missing");

    ws.newRequest()
      .setMethod("POST")
      .execute();
  }

  @Test
  public void fail_if_missing_language_parameter() {
    QProfileDto profile = createProfile();
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'language' parameter is missing");

    ws.newRequest()
      .setMethod("POST")
      .setParam("profileName", profile.getName())
      .execute();
  }

  @Test
  public void fail_if_missing_name_parameter() {
    QProfileDto profile = createProfile();
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'qualityProfile' parameter is missing");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .execute();
  }

  @Test
  public void fail_if_profile_does_not_exist() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile for language 'xoo' and name 'does_not_exist' does not exist");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_QUALITY_PROFILE, "does_not_exist")
      .setParam(PARAM_LANGUAGE, "xoo")
      .execute();
  }

  @Test
  public void fail_if_deleting_default_profile() {
    QProfileDto profile = createProfile();
    db.qualityProfiles().setAsDefault(profile);
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile '" + profile.getName() + "' cannot be deleted because it is marked as default");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, profile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .execute();
  }

  @Test
  public void fail_if_a_descendant_is_marked_as_default() {
    QProfileDto parentProfile = createProfile();
    QProfileDto childProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE).setParentKee(parentProfile.getKee()));
    db.qualityProfiles().setAsDefault(childProfile);
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Profile '" + parentProfile.getName() + "' cannot be deleted because its descendant named '" + childProfile.getName() +
      "' is marked as default");

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, parentProfile.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, parentProfile.getName())
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("language", "qualityProfile");
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn(db.users().insertUser())
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private void verifyProfileDoesNotExist(QProfileDto profile) {
    assertThat(dbClient.qualityProfileDao().selectByUuid(dbSession, profile.getKee())).isNull();
    assertThat(dbClient.qualityProfileDao().selectSelectedProjects(dbSession, profile, null)).isEmpty();
  }

  private void verifyProfileExists(QProfileDto profile) {
    assertThat(dbClient.qualityProfileDao().selectByUuid(dbSession, profile.getKee())).isNotNull();
  }

  private QProfileDto createProfile() {
    return db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));
  }
}
