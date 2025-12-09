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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
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
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class RemoveGroupActionIT {

  private static final String XOO = "xoo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);

  private final WsActionTester ws = new WsActionTester(new RemoveGroupAction(db.getDbClient(), wsSupport, LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("remove_group");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("qualityProfile", "language", "group");
  }

  @Test
  public void remove_group() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isFalse();
  }

  @Test
  public void does_nothing_when_group_cannot_edit_profile() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isFalse();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isFalse();
  }

  @Test
  public void qp_administers_can_remove_group() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isFalse();
  }

  @Test
  public void qp_editors_can_remove_group() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isFalse();
  }

  @Test
  public void fail_when_group_does_not_exist() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_GROUP, "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No group with name 'unknown'");
  }

  @Test
  public void fail_when_qprofile_does_not_exist() {
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, "unknown")
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_GROUP, group.getName())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile for language 'xoo' and name 'unknown' does not exist");
  }

  @Test
  public void fail_when_wrong_language() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("unknown"));
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_GROUP, group.getName())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Quality Profile for language 'xoo' and name '%s' does not exist", profile.getName()));
  }

  @Test
  public void fail_when_qp_is_built_in() {
    GroupDto group = db.users().insertGroup();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO).setIsBuiltIn(true));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_GROUP, group.getName())
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage(String.format("Operation forbidden for built-in Quality Profile '%s' with language 'xoo'", profile.getName()));
  }

  @Test
  public void fail_when_not_enough_permission() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    userSession.logIn(db.users().insertUser()).addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .setParam(PARAM_GROUP, group.getName())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }
}
