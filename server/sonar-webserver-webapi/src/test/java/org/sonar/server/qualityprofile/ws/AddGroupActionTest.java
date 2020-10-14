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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class AddGroupActionTest {

  private static final String XOO = "xoo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db));
  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private WsActionTester ws = new WsActionTester(new AddGroupAction(db.getDbClient(), uuidFactory, wsSupport, LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("add_group");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("qualityProfile", "language", "group");
  }

  @Test
  public void add_group() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
  }

  @Test
  public void does_nothing_when_group_can_already_edit_profile() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
  }

  @Test
  public void qp_administers_can_add_group() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
  }

  @Test
  public void can_add_group_with_user_edit_permission() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
  }

  @Test
  public void can_add_group_with_group_edit_permission() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn(userAllowedToEditProfile).setGroups(group);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
  }

  @Test
  public void uses_default_organization_when_no_organization() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();

    assertThat(db.getDbClient().qProfileEditGroupsDao().exists(db.getSession(), profile, group)).isTrue();
  }

  @Test
  public void fail_when_group_does_not_exist() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with name 'unknown'");

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, "unknown")
      .execute();
  }

  @Test
  public void fail_when_qprofile_does_not_exist() {
    GroupDto group = db.users().insertGroup();
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile for language 'xoo' and name 'unknown' does not exist");

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, "unknown")
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, group.getName())
      .execute();
  }

  @Test
  public void fail_when_wrong_language() {
    OrganizationDto organization = db.organizations().getDefaultOrganization();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("unknown"));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'xoo' and name '%s' does not exist", profile.getName()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, user.getLogin())
      .execute();
  }

  @Test
  public void fail_when_qp_is_built_in() {
    OrganizationDto organization = db.getDefaultOrganization();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO).setIsBuiltIn(true));
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(String.format("Operation forbidden for built-in Quality Profile '%s' with language 'xoo'", profile.getName()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, user.getLogin())
      .execute();
  }

  @Test
  public void fail_when_not_enough_permission() {
    OrganizationDto organization = db.getDefaultOrganization();
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn(db.users().insertUser()).addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_GROUP, user.getLogin())
      .execute();
  }
}
