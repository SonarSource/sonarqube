/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class AddUserActionIT {

  private static final String XOO = "xoo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final WsActionTester ws = new WsActionTester(new AddUserAction(db.getDbClient(), uuidFactory, wsSupport, LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("add_user");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("qualityProfile", "language", "login");
  }

  @Test
  public void add_user() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isTrue();
  }

  @Test
  public void does_nothing_when_user_can_already_edit_profile() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isTrue();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isTrue();
  }

  @Test
  public void qp_administers_can_add_user() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isTrue();
  }

  @Test
  public void qp_editors_can_add_user() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isTrue();
  }

  @Test
  public void fail_when_user_does_not_exist() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("User with login 'unknown' is not found");
  }

  @Test
  public void fail_when_qprofile_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, "unknown")
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_LOGIN, user.getLogin())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile for language 'xoo' and name 'unknown' does not exist");
  }

  @Test
  public void fail_when_wrong_language() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("unknown"));
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_LOGIN, user.getLogin())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Quality Profile for language 'xoo' and name '%s' does not exist", profile.getName()));
  }

  @Test
  public void fail_when_qp_is_built_in() {
    UserDto user = db.users().insertUser();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO).setIsBuiltIn(true));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_LOGIN, user.getLogin())
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage(String.format("Operation forbidden for built-in Quality Profile '%s' with language 'xoo'", profile.getName()));
  }

  @Test
  public void fail_when_not_enough_permission() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    userSession.logIn(db.users().insertUser()).addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_LOGIN, user.getLogin())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }
}
