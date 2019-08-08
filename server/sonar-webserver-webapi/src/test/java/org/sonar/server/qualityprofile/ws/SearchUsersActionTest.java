/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.AvatarResolver;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.issue.FakeAvatarResolver;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualityprofiles.SearchUsersResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchUsersActionTest {

  private static final String XOO = "xoo";
  private static final String FOO = "foo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO, FOO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db));
  private AvatarResolver avatarResolver = new FakeAvatarResolver();

  private WsActionTester ws = new WsActionTester(new SearchUsersAction(db.getDbClient(), wsSupport, LANGUAGES, avatarResolver));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("search_users");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("organization", "qualityProfile", "language", "selected", "q", "p", "ps");
  }

  @Test
  public void test_example() {
    avatarResolver = new AvatarResolverImpl();
    ws = new WsActionTester(new SearchUsersAction(db.getDbClient(), wsSupport, LANGUAGES, avatarResolver));
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser(u -> u.setLogin("admin").setName("Administrator").setEmail("admin@email.com"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("george.orwell").setName("George Orwell").setEmail("george@orwell.com"));
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    String result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setMediaType(JSON)
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void search_all_users() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser(u -> u.setEmail("user1@email.com"));
    UserDto user2 = db.users().insertUser(u -> u.setEmail("user2@email.com"));
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getAvatar, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName(), "user1@email.com_avatar", true),
        tuple(user2.getLogin(), user2.getName(), "user2@email.com_avatar", false));
  }

  @Test
  public void search_selected_users() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "selected")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName(), true));
  }

  @Test
  public void search_deselected_users() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "deselected")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user2.getLogin(), user2.getName(), false));
  }

  @Test
  public void search_by_login() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(TEXT_QUERY, user1.getLogin())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin)
      .containsExactlyInAnyOrder(user1.getLogin());
  }

  @Test
  public void search_by_name() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser(u -> u.setName("John Doe"));
    UserDto user2 = db.users().insertUser(u -> u.setName("Jane Doe"));
    UserDto user3 = db.users().insertUser(u -> u.setName("John Smith"));
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.organizations().addMember(organization, user3);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(TEXT_QUERY, "ohn")
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin)
      .containsExactlyInAnyOrder(user1.getLogin(), user3.getLogin());
  }

  @Test
  public void user_without_email() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser(u -> u.setEmail(null));
    db.organizations().addMember(organization, user);
    db.qualityProfiles().addUserPermission(profile, user);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::hasAvatar)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void paging_search() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user2 = db.users().insertUser(u -> u.setName("user2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("user3"));
    UserDto user1 = db.users().insertUser(u -> u.setName("user1"));
    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);
    db.organizations().addMember(organization, user3);
    db.qualityProfiles().addUserPermission(profile, user1);
    db.qualityProfiles().addUserPermission(profile, user2);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    assertThat(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchUsersResponse.class).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactly(user1.getLogin());

    assertThat(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "3")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchUsersResponse.class).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactly(user3.getLogin());

    assertThat(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "10")
      .executeProtobuf(SearchUsersResponse.class).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactly(user1.getLogin(), user2.getLogin(), user3.getLogin());
  }

  @Test
  public void uses_default_organization_when_no_organization() {
    OrganizationDto organization = db.getDefaultOrganization();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin).containsExactlyInAnyOrder(user1.getLogin());
  }

  @Test
  public void qp_administers_can_search_users() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin).containsExactlyInAnyOrder(user.getLogin());
  }

  @Test
  public void qp_editors_can_search_users() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin).containsExactlyInAnyOrder(user.getLogin());
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
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_wrong_language() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    UserDto user1 = db.users().insertUser();
    db.organizations().addMember(organization, user1);
    db.qualityProfiles().addUserPermission(profile, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'foo' and name '%s' does not exist in organization '%s'", profile.getName(), organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, FOO)
      .executeProtobuf(SearchUsersResponse.class);
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
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }
}
