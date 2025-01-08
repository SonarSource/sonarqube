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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Users.GroupsWsResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.SELECTED;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class GroupsActionIT {

  private static final String USER_LOGIN = "john";

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().addPermission(GlobalPermission.ADMINISTER);

  private WsActionTester ws = new WsActionTester(new GroupsAction(db.getDbClient(), userSession,
    new DefaultGroupFinder(db.getDbClient())));

  @Test
  public void empty_groups() {
    insertUser();
    insertDefaultGroup();

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN));

    assertThat(response.getGroupsList()).isEmpty();
  }

  @Test
  public void return_selected_groups_selected_param_is_set_to_all() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(
        tuple(usersGroup.getUuid(), usersGroup.getName(), usersGroup.getDescription(), true),
        tuple(adminGroup.getUuid(), adminGroup.getName(), adminGroup.getDescription(), false));
  }

  @Test
  public void return_selected_groups_selected_param_is_set_to_selected() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, SELECTED.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(tuple(usersGroup.getUuid(), usersGroup.getName(), usersGroup.getDescription(), true));
  }

  @Test
  public void return_selected_groups_selected_param_is_not_set() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(tuple(usersGroup.getUuid(), usersGroup.getName(), usersGroup.getDescription(), true));
  }

  @Test
  public void return_not_selected_groups_selected_param_is_set_to_deselected() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, DESELECTED.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(tuple(adminGroup.getUuid(), adminGroup.getName(), adminGroup.getDescription(), false));
  }

  @Test
  public void search_with_pagination() {
    UserDto user = insertUser();
    insertDefaultGroup();
    for (int i = 1; i <= 9; i++) {
      GroupDto groupDto = insertGroup("group-" + i, "group-" + i);
      addUserToGroup(user, groupDto);
    }

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN)
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.PAGE, "2")
      .setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList()).extracting(GroupsWsResponse.Group::getName).containsOnly("group-4", "group-5", "group-6");
    assertThat(response.getPaging().getPageSize()).isEqualTo(3);
    assertThat(response.getPaging().getPageIndex()).isEqualTo(2);
    assertThat(response.getPaging().getTotal()).isEqualTo(10);
  }

  @Test
  public void search_by_text_query() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    assertThat(call(ws.newRequest().setParam("login", USER_LOGIN).setParam("q", "admin").setParam(Param.SELECTED, ALL.value())).getGroupsList())
      .extracting(GroupsWsResponse.Group::getName).containsOnly(adminGroup.getName());
    assertThat(call(ws.newRequest().setParam("login", USER_LOGIN).setParam("q", "users").setParam(Param.SELECTED, ALL.value())).getGroupsList())
      .extracting(GroupsWsResponse.Group::getName).containsOnly(usersGroup.getName());
  }

  @Test
  public void return_default_group_information() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDefault)
      .containsOnly(
        tuple(usersGroup.getUuid(), usersGroup.getName(), true),
        tuple(adminGroup.getUuid(), adminGroup.getName(), false));
  }

  @Test
  public void return_groups() {
    UserDto user = insertUser();
    GroupDto group = db.users().insertDefaultGroup();
    addUserToGroup(user, group);

    GroupsWsResponse response = call(ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected,
        GroupsWsResponse.Group::getDefault)
      .containsOnly(tuple(group.getUuid(), group.getName(), group.getDescription(), true, true));
  }

  @Test
  public void fail_when_no_default_group() {
    UserDto user = insertUser();
    GroupDto group = db.users().insertGroup("group1");
    addUserToGroup(user, group);

    assertThatThrownBy(() -> {
      call(ws.newRequest().setParam("login", USER_LOGIN));
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default group cannot be found");
  }

  @Test
  public void fail_on_unknown_user() {
    insertDefaultGroup();

    assertThatThrownBy(() -> {
      call(ws.newRequest().setParam("login", USER_LOGIN));
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unknown user: john");
  }

  @Test
  public void fail_on_disabled_user() {
    UserDto userDto = db.users().insertUser(user -> user.setLogin("disabled").setActive(false));
    insertDefaultGroup();

    assertThatThrownBy(() -> {
      call(ws.newRequest().setParam("login", userDto.getLogin()));
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unknown user: disabled");
  }

  @Test
  public void fail_when_page_size_is_greater_than_500() {
    UserDto user = insertUser();
    insertDefaultGroup();

    assertThatThrownBy(() -> {
      call(ws.newRequest()
        .setParam("login", user.getLogin())
        .setParam(Param.PAGE_SIZE, "501"));
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'ps' parameter must be less than 500");
  }

  @Test
  public void fail_on_missing_permission() {
    userSession.logIn().addPermission(SCAN);

    assertThatThrownBy(() -> {
      call(ws.newRequest().setParam("login", USER_LOGIN));
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_on_missing_permission_and_non_existent_user() {
    userSession.logIn().addPermission(SCAN);

    assertThatThrownBy(() -> {
      call(ws.newRequest().setParam("login", "unknown"));
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_no_permission() {
    userSession.logIn("not-admin");

    TestRequest request = ws.newRequest().setParam("login", USER_LOGIN);
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void test_json_example() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup();
    insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam("login", USER_LOGIN)
      .setParam(Param.SELECTED, ALL.value())
      .setParam(Param.PAGE_SIZE, "25")
      .setParam(Param.PAGE, "1")
      .execute().getInput();
    assertJson(response).ignoreFields("id").isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.2");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();

    assertThat(action.params()).extracting(Param::key).containsOnly("p", "q", "ps", "login", "selected");

    WebService.Param qualifiers = action.param("login");
    assertThat(qualifiers.isRequired()).isTrue();
  }

  private GroupDto insertGroup(String name, String description) {
    return db.users().insertGroup(newGroupDto().setName(name).setDescription(description));
  }

  private GroupDto insertDefaultGroup() {
    return db.users().insertDefaultGroup();
  }

  private void addUserToGroup(UserDto user, GroupDto usersGroup) {
    db.users().insertMember(usersGroup, user);
  }

  private UserDto insertUser() {
    return db.users().insertUser(newUserDto()
      .setActive(true)
      .setEmail("john@email.com")
      .setLogin(USER_LOGIN)
      .setName("John")
      .setScmAccounts(singletonList("jn")));
  }

  private GroupsWsResponse call(TestRequest request) {
    return request.executeProtobuf(GroupsWsResponse.class);
  }

}
