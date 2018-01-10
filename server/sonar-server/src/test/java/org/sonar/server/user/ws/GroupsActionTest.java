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
package org.sonar.server.user.ws;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Users.GroupsWsResponse;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.SelectionMode.ALL;
import static org.sonar.api.server.ws.WebService.SelectionMode.DESELECTED;
import static org.sonar.api.server.ws.WebService.SelectionMode.SELECTED;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class GroupsActionTest {

  private static final String USER_LOGIN = "john";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setRoot();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private WsActionTester ws = new WsActionTester(new GroupsAction(db.getDbClient(), userSession, defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient())));

  @Test
  public void empty_groups() {
    insertUser();
    insertDefaultGroup("sonar-users", "Sonar Users");

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN));

    assertThat(response.getGroupsList()).isEmpty();
  }

  @Test
  public void return_selected_groups_selected_param_is_set_to_all() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(
        tuple(usersGroup.getId().longValue(), usersGroup.getName(), usersGroup.getDescription(), true),
        tuple(adminGroup.getId().longValue(), adminGroup.getName(), adminGroup.getDescription(), false));
  }

  @Test
  public void return_selected_groups_selected_param_is_set_to_selected() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
    insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, SELECTED.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(tuple(usersGroup.getId().longValue(), usersGroup.getName(), usersGroup.getDescription(), true));
  }

  @Test
  public void return_selected_groups_selected_param_is_not_set() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
    insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(tuple(usersGroup.getId().longValue(), usersGroup.getName(), usersGroup.getDescription(), true));
  }

  @Test
  public void return_not_selected_groups_selected_param_is_set_to_deselected() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, DESELECTED.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected)
      .containsOnly(tuple(adminGroup.getId().longValue(), adminGroup.getName(), adminGroup.getDescription(), false));
  }

  @Test
  public void return_group_not_having_description() {
    UserDto user = insertUser();
    GroupDto group = insertDefaultGroup("sonar-users", null);
    addUserToGroup(user, group);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", "john").setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList()).extracting(GroupsWsResponse.Group::hasDescription).containsOnly(false);
  }

  @Test
  public void search_with_pagination() {
    UserDto user = insertUser();
    insertDefaultGroup("sonar-users", "Sonar Users");
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
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
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
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
    GroupDto adminGroup = insertGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);

    GroupsWsResponse response = call(ws.newRequest().setParam("login", USER_LOGIN).setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDefault)
      .containsOnly(
        tuple(usersGroup.getId().longValue(), usersGroup.getName(), true),
        tuple(adminGroup.getId().longValue(), adminGroup.getName(), false));
  }

  @Test
  public void return_groups_from_given_organization() {
    UserDto user = insertUser();
    OrganizationDto organizationDto = db.organizations().insert();
    OrganizationDto otherOrganizationDto = db.organizations().insert();
    GroupDto group = db.users().insertDefaultGroup(newGroupDto().setName("group1").setOrganizationUuid(organizationDto.getUuid()));
    GroupDto otherGroup = db.users().insertDefaultGroup(newGroupDto().setName("group2").setOrganizationUuid(otherOrganizationDto.getUuid()));
    addUserToGroup(user, group);
    addUserToGroup(user, otherGroup);

    GroupsWsResponse response = call(ws.newRequest()
      .setParam("login", USER_LOGIN)
      .setParam("organization", organizationDto.getKey())
      .setParam(Param.SELECTED, ALL.value()));

    assertThat(response.getGroupsList())
      .extracting(GroupsWsResponse.Group::getId, GroupsWsResponse.Group::getName, GroupsWsResponse.Group::getDescription, GroupsWsResponse.Group::getSelected,
        GroupsWsResponse.Group::getDefault)
      .containsOnly(tuple(group.getId().longValue(), group.getName(), group.getDescription(), true, true));
  }

  @Test
  public void fail_when_organization_has_no_default_group() {
    UserDto user = insertUser();
    OrganizationDto organizationDto = db.organizations().insert(organization -> organization.setKey("OrgKey"));
    GroupDto group = db.users().insertGroup(organizationDto, "group1");
    addUserToGroup(user, group);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Default group cannot be found on organization '%s'", organizationDto.getUuid()));

    call(ws.newRequest().setParam("login", USER_LOGIN).setParam("organization", organizationDto.getKey()));
  }

  @Test
  public void fail_on_unknown_user() {
    insertDefaultGroup("sonar-users", "Sonar Users");

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Unknown user: john");

    call(ws.newRequest().setParam("login", USER_LOGIN));
  }

  @Test
  public void fail_on_disabled_user() {
    UserDto userDto = db.users().insertUser(user -> user.setLogin("disabled").setActive(false));
    insertDefaultGroup("sonar-users", "Sonar Users");

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Unknown user: disabled");

    call(ws.newRequest().setParam("login", userDto.getLogin()));
  }

  @Test
  public void fail_on_unknown_organization() {
    insertUser();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'unknown'");

    call(ws.newRequest().setParam("login", USER_LOGIN).setParam("organization", "unknown"));
  }

  @Test
  public void fail_when_page_size_is_greater_than_500() {
    UserDto user = insertUser();
    insertDefaultGroup("sonar-users", "Sonar Users");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'ps' parameter must be less than 500");

    call(ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam(Param.PAGE_SIZE, "501"));
  }

  @Test
  public void fail_on_missing_permission() {
    OrganizationDto organizationDto = db.organizations().insert();
    userSession.logIn().addPermission(ADMINISTER, organizationDto);

    expectedException.expect(ForbiddenException.class);

    call(ws.newRequest().setParam("login", USER_LOGIN));
  }

  @Test
  public void fail_when_no_permission() {
    userSession.logIn("not-admin");

    expectedException.expect(ForbiddenException.class);

    call(ws.newRequest().setParam("login", USER_LOGIN));
  }

  @Test
  public void test_json_example() {
    UserDto user = insertUser();
    GroupDto usersGroup = insertDefaultGroup("sonar-users", "Sonar Users");
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

    assertThat(action.params()).extracting(Param::key).containsOnly("p", "q", "ps", "login", "selected", "organization");

    WebService.Param qualifiers = action.param("login");
    assertThat(qualifiers.isRequired()).isTrue();

    WebService.Param organization = action.param("organization");
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.description()).isEqualTo("Organization key");
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.exampleValue()).isEqualTo("my-org");
    assertThat(organization.since()).isEqualTo("6.4");
  }

  private GroupDto insertGroup(String name, String description) {
    return db.users().insertGroup(newGroupDto().setName(name).setDescription(description).setOrganizationUuid(db.getDefaultOrganization().getUuid()));
  }

  private GroupDto insertDefaultGroup(String name, String description) {
    return db.users().insertDefaultGroup(newGroupDto().setName(name).setDescription(description).setOrganizationUuid(db.getDefaultOrganization().getUuid()));
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
