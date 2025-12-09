/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class DeleteActionIT {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);

  private final DeleteAction underTest = new DeleteAction(
    new Languages(LanguageTesting.newLanguage(A_LANGUAGE)),
    new QProfileFactoryImpl(dbClient, UuidFactoryFast.getInstance(), System2.INSTANCE, activeRuleIndexer), dbClient, userSession,
    new QProfileWsSupport(dbClient, userSession));
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void delete_profile_by_language_and_name() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
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
  public void as_qprofile_editor_and_global_admin() {
    QProfileDto profile = createProfile();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn(user).addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

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

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_LANGUAGE, profile1.getLanguage())
        .setParam(PARAM_QUALITY_PROFILE, profile1.getName())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_not_profile_administrator() {
    QProfileDto qprofile = createProfile();
    userSession.logIn(db.users().insertUser());

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_LANGUAGE, qprofile.getLanguage())
        .setParam(PARAM_QUALITY_PROFILE, qprofile.getName())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_not_logged_in() {
    QProfileDto profile = createProfile();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_KEY, profile.getKee())
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_missing_parameters() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'language' parameter is missing");
  }

  @Test
  public void fail_if_missing_language_parameter() {
    QProfileDto profile = createProfile();
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam("profileName", profile.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'language' parameter is missing");
  }

  @Test
  public void fail_if_missing_name_parameter() {
    QProfileDto profile = createProfile();
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_LANGUAGE, profile.getLanguage())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'qualityProfile' parameter is missing");
  }

  @Test
  public void fail_if_profile_does_not_exist() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_QUALITY_PROFILE, "does_not_exist")
        .setParam(PARAM_LANGUAGE, "xoo")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile for language 'xoo' and name 'does_not_exist' does not exist");
  }

  @Test
  public void fail_if_deleting_default_profile() {
    QProfileDto profile = createProfile();
    db.qualityProfiles().setAsDefault(profile);
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_LANGUAGE, profile.getLanguage())
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Profile '" + profile.getName() + "' cannot be deleted because it is marked as default");
  }

  @Test
  public void fail_if_a_descendant_is_marked_as_default() {
    QProfileDto parentProfile = createProfile();
    QProfileDto childProfile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE).setParentKee(parentProfile.getKee()));
    db.qualityProfiles().setAsDefault(childProfile);
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setMethod("POST")
        .setParam(PARAM_LANGUAGE, parentProfile.getLanguage())
        .setParam(PARAM_QUALITY_PROFILE, parentProfile.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Profile '" + parentProfile.getName() + "' cannot be deleted because its descendant named '" + childProfile.getName() +
        "' is marked as default");
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
