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
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final WsActionTester ws = new WsActionTester(
    new UpdateAction(db.getDbClient(), userSession, new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient()))));

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
      .setParam("id", group.getUuid())
      .setParam("name", "new-name")
      .setParam("description", "New Description")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"New Description\"," +
      "    \"membersCount\": 1" +
      "  }" +
      "}");
  }

  @Test
  public void update_only_name() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam("id", group.getUuid())
      .setParam("name", "new-name")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"" + group.getDescription() + "\"," +
      "    \"membersCount\": 0" +
      "  }" +
      "}");
  }

  @Test
  public void update_only_name_by_name() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam("currentName", group.getName())
      .setParam("name", "new-name")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"new-name\"," +
      "    \"description\": \"" + group.getDescription() + "\"," +
      "    \"membersCount\": 0" +
      "  }" +
      "}");
  }

  @Test
  public void update_only_description() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam("id", group.getUuid())
      .setParam("description", "New Description")
      .execute().getInput();

    assertJson(result).isSimilarTo("{" +
      "  \"group\": {" +
      "    \"name\": \"" + group.getName() + "\"," +
      "    \"description\": \"New Description\"," +
      "    \"membersCount\": 0" +
      "  }" +
      "}");
  }

  @Test
  public void return_default_field() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    String result = newRequest()
      .setParam("id", group.getUuid())
      .setParam("name", "new-name")
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
        .setParam("id", group.getUuid())
        .setParam("name", "some-product-bu")
        .setParam("description", "Business Unit for Some Awesome Product")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_name_is_too_short() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", group.getUuid())
        .setParam("name", "")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Group name cannot be empty");
  }

  @Test
  public void fail_if_no_id_and_no_currentname_are_provided() {
    insertDefaultGroup();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("name", "newname")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Need to specify one and only one of 'id' or 'currentName'");
  }

  @Test
  public void fail_if_both_id_and_currentname_are_provided() {
    insertDefaultGroup();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", "id")
        .setParam("currentName", "name")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Need to specify one and only one of 'id' or 'currentName'");
  }

  @Test
  public void fail_if_new_name_is_anyone() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", group.getUuid())
        .setParam("name", "AnYoNe")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Anyone group cannot be used");
  }

  @Test
  public void fail_to_update_if_name_already_exists() {
    insertDefaultGroup();
    GroupDto groupToBeRenamed = db.users().insertGroup("a name");
    String newName = "new-name";
    db.users().insertGroup(newName);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", groupToBeRenamed.getUuid())
        .setParam("name", newName)
        .execute();
    })
      .isInstanceOf(ServerException.class)
      .hasMessage("Group 'new-name' already exists");
  }

  @Test
  public void fail_if_unknown_group_id() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", "42")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Could not find a user group with id '42'.");
  }

  @Test
  public void fail_if_unknown_group_name() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("currentName", "42")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Could not find a user group with name '42'.");
  }

  @Test
  public void fail_to_update_default_group_name() {
    GroupDto group = db.users().insertDefaultGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", group.getUuid())
        .setParam("name", "new name")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void fail_to_update_default_group_description() {
    GroupDto group = db.users().insertDefaultGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", group.getUuid())
        .setParam("description", "new description")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
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
