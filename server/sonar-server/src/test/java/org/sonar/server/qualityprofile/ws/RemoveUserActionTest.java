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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
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
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class RemoveUserActionTest {

  private static final String XOO = "xoo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db));

  private WsActionTester ws = new WsActionTester(new RemoveUserAction(db.getDbClient(), wsSupport, LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("remove_user");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("organization", "qualityProfile", "language", "login");
  }

  @Test
  public void remove_user() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
  }

  @Test
  public void does_nothing_when_user_cannot_edit_profile() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
  }

  @Test
  public void qp_administers_can_remove_user() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
  }

  @Test
  public void qp_editors_can_remove_user() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.qualityProfiles().addUserPermission(profile, user);
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
  }

  @Test
  public void uses_default_organization_when_no_organization() {
    OrganizationDto organization = db.getDefaultOrganization();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin() )
      .execute();

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(db.getSession(), profile, user)).isFalse();
  }

  @Test
  public void fail_when_user_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown' is not found");

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, "unknown")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_qprofile_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'xoo' and name 'unknown' does not exist in organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, "unknown")
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_qprofile_does_not_belong_to_organization() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    OrganizationDto anotherOrganization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(anotherOrganization, p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'xoo' and name '%s' does not exist in organization '%s'", profile.getName(), organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_wrong_language() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage("unknown"));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'xoo' and name '%s' does not exist in organization '%s'", profile.getName(), organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_user_is_not_member_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(anotherOrganization, user);
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("User '%s' is not member of organization '%s'", user.getLogin(), organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_qp_is_built_in() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO).setIsBuiltIn(true));
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(String.format("Operation forbidden for built-in Quality Profile '%s' with language 'xoo'", profile.getName()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_not_enough_permission() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_LOGIN, user.getLogin())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }
}
