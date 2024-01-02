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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.test.JsonAssert.assertJson;

public class UsersActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final WsActionTester ws = new WsActionTester(
    new UsersAction(db.getDbClient(), userSession, new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient()))));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isFalse();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("9.8", "response fields 'total', 's', 'ps' have been deprecated, please use 'paging' object instead."),
      tuple("9.8", "The field 'paging' has been added to the response."),
      tuple("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));
  }

  @Test
  public void fail_if_unknown_group_uuid() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newUsersRequest()
        .setParam("id", "42")
        .setParam("login", "john").execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No group with id '42'");
  }

  @Test
  public void fail_if_not_admin() {
    GroupDto group = db.users().insertGroup();
    userSession.logIn("not-admin");

    assertThatThrownBy(() -> {
      newUsersRequest()
        .setParam("id", group.getUuid())
        .setParam("login", "john").execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void group_has_no_users() {
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newUsersRequest()
      .setParam("login", "john")
      .setParam("id", group.getUuid())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"p\": 1,\n" +
      "  \"total\": 0,\n" +
      "  \"paging\": {\n" +
      "    \"pageIndex\": 1,\n" +
      "    \"pageSize\": 25,\n" +
      "    \"total\": 0\n" +
      "  }," +
      "  \"users\": []\n" +
      "}");
  }

  @Test
  public void return_members_by_group_uuid() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, lovelace);
    loginAsAdmin();

    String result = newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void references_group_by_its_name() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, lovelace);
    loginAsAdmin();

    String result = newUsersRequest()
      .setParam("name", group.getName())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void filter_members_by_name() {
    GroupDto group = db.users().insertGroup("a group");
    UserDto adaLovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    UserDto graceHopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, adaLovelace);
    db.users().insertMember(group, graceHopper);
    loginAsAdmin();

    String response = newUsersRequest().setParam(PARAM_GROUP_ID, group.getUuid()).execute().getInput();

    assertThat(response).contains("Ada Lovelace", "Grace Hopper");
  }

  @Test
  public void selected_users() {
    GroupDto group = db.users().insertGroup("a group");
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, lovelace);
    loginAsAdmin();

    assertJson(newUsersRequest()
      .setParam("id", group.getUuid())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada\", \"name\": \"Ada Lovelace\", \"selected\": true}\n" +
        "  ]\n" +
        "}");

    assertJson(newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam(Param.SELECTED, SelectionMode.SELECTED.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada\", \"name\": \"Ada Lovelace\", \"selected\": true}\n" +
        "  ]\n" +
        "}");
  }

  @Test
  public void deselected_users() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, lovelace);
    loginAsAdmin();

    String result = newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam(Param.SELECTED, SelectionMode.DESELECTED.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"users\": [\n" +
      "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void paging() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada").setName("Ada Lovelace"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper"));
    db.users().insertMember(group, lovelace);
    loginAsAdmin();

    assertJson(newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam("ps", "1")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"p\": 1,\n" +
        "  \"ps\": 1,\n" +
        "  \"total\": 2,\n" +
        "  \"paging\": {\n" +
        "    \"pageIndex\": 1,\n" +
        "    \"pageSize\": 1,\n" +
        "    \"total\": 2\n" +
        "  }," +
        "  \"users\": [\n" +
        "    {\"login\": \"ada\", \"name\": \"Ada Lovelace\", \"selected\": true}\n" +
        "  ]\n" +
        "}");

    assertJson(newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam("ps", "1")
      .setParam("p", "2")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"p\": 2,\n" +
        "  \"ps\": 1,\n" +
        "  \"total\": 2,\n" +
        "  \"paging\": {\n" +
        "    \"pageIndex\": 2,\n" +
        "    \"pageSize\": 1,\n" +
        "    \"total\": 2\n" +
        "  }," +
        "  \"users\": [\n" +
        "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
        "  ]\n" +
        "}");
  }

  @Test
  public void filtering_by_name_email_and_login() {
    GroupDto group = db.users().insertGroup();
    UserDto lovelace = db.users().insertUser(newUserDto().setLogin("ada.login").setName("Ada Lovelace").setEmail("ada@email.com"));
    UserDto hopper = db.users().insertUser(newUserDto().setLogin("grace").setName("Grace Hopper").setEmail("grace@hopper.com"));
    db.users().insertMember(group, lovelace);
    loginAsAdmin();

    assertJson(newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam("q", "ace")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\"login\": \"ada.login\", \"name\": \"Ada Lovelace\", \"selected\": true},\n" +
        "    {\"login\": \"grace\", \"name\": \"Grace Hopper\", \"selected\": false}\n" +
        "  ]\n" +
        "}\n");

    assertJson(newUsersRequest().setParam("id", group.getUuid())
      .setParam("q", ".logi")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\n" +
        "      \"login\": \"ada.login\",\n" +
        "      \"name\": \"Ada Lovelace\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");

    assertJson(newUsersRequest().setParam("id", group.getUuid())
      .setParam("q", "OvE")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\n" +
        "      \"login\": \"ada.login\",\n" +
        "      \"name\": \"Ada Lovelace\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");

    assertJson(newUsersRequest().setParam("id", group.getUuid())
      .setParam("q", "mail")
      .execute()
      .getInput()).isSimilarTo("{\n" +
        "  \"users\": [\n" +
        "    {\n" +
        "      \"login\": \"ada.login\",\n" +
        "      \"name\": \"Ada Lovelace\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
  }

  @Test
  public void test_example() {
    GroupDto group = db.users().insertGroup();
    UserDto admin = db.users().insertUser(newUserDto().setLogin("admin").setName("Administrator"));
    db.users().insertMember(group, admin);
    UserDto george = db.users().insertUser(newUserDto().setLogin("george.orwell").setName("George Orwell"));
    db.users().insertMember(group, george);
    loginAsAdmin();

    String result = newUsersRequest()
      .setParam("id", group.getUuid())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  private TestRequest newUsersRequest() {
    return ws.newRequest();
  }

  private void loginAsAdmin() {
    userSession.logIn().addPermission(ADMINISTER);
  }

}
