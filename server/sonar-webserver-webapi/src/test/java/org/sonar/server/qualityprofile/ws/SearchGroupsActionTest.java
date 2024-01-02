/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualityprofiles.SearchGroupsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchGroupsActionTest {
  private static final String XOO = "xoo";
  private static final String FOO = "foo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO, FOO);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);

  private final WsActionTester ws = new WsActionTester(new SearchGroupsAction(db.getDbClient(), wsSupport, LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("search_groups");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("qualityProfile", "language", "selected", "q", "p", "ps");
  }

  @Test
  public void test_example() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("users").setDescription("Users"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("administrators").setDescription("Administrators"));
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    String result = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setMediaType(JSON)
      .execute()
      .getInput();

    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void search_all_groups() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::getDescription, SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getName(), group1.getDescription(), true),
        tuple(group2.getName(), group2.getDescription(), false));
  }

  @Test
  public void search_selected_groups() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "selected")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::getDescription, SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getName(), group1.getDescription(), true));
  }

  @Test
  public void search_deselected_groups() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "deselected")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::getDescription, SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group2.getName(), group2.getDescription(), false));
  }

  @Test
  public void search_by_name() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup("sonar-users-project");
    GroupDto group2 = db.users().insertGroup("sonar-users-qprofile");
    GroupDto group3 = db.users().insertGroup("sonar-admin");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);
    db.qualityProfiles().addGroupPermission(profile, group3);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(TEXT_QUERY, "UsErS")
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName)
      .containsExactlyInAnyOrder(group1.getName(), group2.getName());
  }

  @Test
  public void group_without_description() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup(newGroupDto().setDescription(null));
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::hasDescription)
      .containsExactlyInAnyOrder(tuple(group.getName(), false));
  }

  @Test
  public void paging_search() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group3 = db.users().insertGroup("group3");
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    assertThat(ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchGroupsResponse.class).getGroupsList())
        .extracting(SearchGroupsResponse.Group::getName)
        .containsExactly(group1.getName());

    assertThat(ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "3")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchGroupsResponse.class).getGroupsList())
        .extracting(SearchGroupsResponse.Group::getName)
        .containsExactly(group3.getName());

    assertThat(ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "10")
      .executeProtobuf(SearchGroupsResponse.class).getGroupsList())
        .extracting(SearchGroupsResponse.Group::getName)
        .containsExactly(group1.getName(), group2.getName(), group3.getName());
  }

  @Test
  public void uses_global_permission() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName).containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void qp_administers_can_search_groups() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName).containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void qp_editors_can_search_groups() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup();
    db.qualityProfiles().addGroupPermission(profile, group);
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName).containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void fail_when_qprofile_does_not_exist() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, "unknown")
        .setParam(PARAM_LANGUAGE, XOO)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile for language 'xoo' and name 'unknown' does not exist");
  }

  @Test
  public void fail_when_wrong_language() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, FOO)
        .executeProtobuf(SearchGroupsResponse.class);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Quality Profile for language 'foo' and name '%s' does not exist", profile.getName()));
  }

  @Test
  public void fail_when_not_enough_permission() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(XOO));
    userSession.logIn(db.users().insertUser()).addPermission(GlobalPermission.ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_QUALITY_PROFILE, profile.getName())
        .setParam(PARAM_LANGUAGE, XOO)
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }
}
