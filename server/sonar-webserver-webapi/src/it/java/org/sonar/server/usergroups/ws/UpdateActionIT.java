/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_CURRENT_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_DESCRIPTION;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private final WsActionTester ws = new WsActionTester(
    new UpdateAction(db.getDbClient(), userSession, new GroupService(db.getDbClient(), UuidFactoryImpl.INSTANCE), managedInstanceChecker));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).isNotEmpty();
  }

  @Test
  public void update_both_name_and_description() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group, user);
    loginAsAdmin();

    String result = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_NAME, "new-name")
      .setParam(PARAM_GROUP_DESCRIPTION, "New Description")
      .execute().getInput();

    assertJson(result).isSimilarTo("""
      {
        "group": {
          "name": "new-name",
          "description": "New Description",
          "membersCount": 1
        }
      }""");
  }

  @Test
  public void update_only_name() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_NAME, "new-name")
      .execute().getInput();

    assertJson(result).isSimilarTo(String.format("""
      {
        "group": {
          "name": "new-name",
          "description": "%s",
          "membersCount": 0
        }
      }""", group.getDescription()));
  }

  @Test
  public void update_only_description() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_DESCRIPTION, "New Description")
      .execute().getInput();

    assertJson(result).isSimilarTo(String.format("""
      {
        "group": {
          "name": "%s",
          "description": "New Description",
          "membersCount": 0
        }
      }""", group.getName()));
  }

  @Test
  public void return_default_field() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_NAME, "new-name")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"" + group.getDescription() + "\"," +
      "    \"membersCount\": 0," +
      "    \"default\": false" +
      "  }" +
      "}");
  }

  @Test
  public void require_admin_permission() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    userSession.logIn("not-admin");

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
        .setParam(PARAM_GROUP_NAME, "some-product-bu")
        .setParam(PARAM_GROUP_DESCRIPTION, "Business Unit for Some Awesome Product")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_name_is_too_short() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_NAME, "");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Group name cannot be empty");
  }

  @Test
  public void fail_if_no_currentname_are_provided() {
    insertDefaultGroup();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, "newname");

    loginAsAdmin();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'currentName' parameter is missing");
  }

  @Test
  public void fail_if_new_name_is_anyone() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_NAME, "AnYoNe");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Anyone group cannot be used");
  }

  @Test
  public void fail_to_update_if_name_already_exists() {
    insertDefaultGroup();
    GroupDto groupToBeRenamed = db.users().insertGroup("a name");
    String newName = "new-name";
    db.users().insertGroup(newName);
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, groupToBeRenamed.getName())
      .setParam(PARAM_GROUP_NAME, newName);

    assertThatThrownBy(request::execute)
      .isInstanceOf(ServerException.class)
      .hasMessage("Group 'new-name' already exists");
  }

  @Test
  public void fail_if_unknown_group_name() {
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, "42");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Could not find a user group with name '42'.");
  }

  @Test
  public void fail_to_update_default_group_name() {
    GroupDto group = db.users().insertDefaultGroup();
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_NAME, "new name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void fail_to_update_default_group_description() {
    GroupDto group = db.users().insertDefaultGroup();
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_CURRENT_NAME, group.getName())
      .setParam(PARAM_GROUP_DESCRIPTION, "new description");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void fail_if_instance_is_externally_managed() {
    loginAsAdmin();
    BadRequestException exception = BadRequestException.create("Not allowed");
    doThrow(exception).when(managedInstanceChecker).throwIfInstanceIsManaged();
    TestRequest testRequest = newRequest();
    assertThatThrownBy(testRequest::execute)
      .isEqualTo(exception);
  }

  private TestRequest newRequest() {
    return ws.newRequest();
  }

  private void loginAsAdmin() {
    userSession.logIn().addPermission(ADMINISTER);
  }

  private void insertDefaultGroup() {
    db.users().insertDefaultGroup();
  }

}
