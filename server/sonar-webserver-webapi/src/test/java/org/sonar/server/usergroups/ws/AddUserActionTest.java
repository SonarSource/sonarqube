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
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;

public class AddUserActionTest {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final WsActionTester ws = new WsActionTester(new AddUserAction(db.getDbClient(), userSession, newGroupWsSupport()));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));
  }

  @Test
  public void add_user_to_group_referenced_by_its_id() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    loginAsAdmin();

    newRequest()
      .setParam("id", group.getUuid())
      .setParam("login", user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).containsOnly(group.getUuid());
  }

  @Test
  public void add_user_to_group_referenced_by_its_name() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).containsOnly(group.getUuid());
  }

  @Test
  public void add_user_to_another_group() {
    insertDefaultGroup();
    GroupDto admins = db.users().insertGroup("admins");
    GroupDto users = db.users().insertGroup("users");
    UserDto user = db.users().insertUser("my-admin");
    db.users().insertMember(users, user);
    loginAsAdmin();

    newRequest()
      .setParam("id", admins.getUuid())
      .setParam("login", user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).containsOnly(admins.getUuid(), users.getUuid());
  }

  @Test
  public void do_not_fail_if_user_is_already_member_of_group() {
    insertDefaultGroup();
    GroupDto users = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(users, user);
    loginAsAdmin();

    newRequest()
      .setParam("id", users.getUuid())
      .setParam("login", user.getLogin())
      .execute();

    // do not insert duplicated row
    assertThat(db.users().selectGroupUuidsOfUser(user)).hasSize(1).containsOnly(users.getUuid());
  }

  @Test
  public void group_has_multiple_members() {
    insertDefaultGroup();
    GroupDto users = db.users().insertGroup();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertMember(users, user1);
    loginAsAdmin();

    newRequest()
      .setParam("id", users.getUuid())
      .setParam("login", user2.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user1)).containsOnly(users.getUuid());
    assertThat(db.users().selectGroupUuidsOfUser(user2)).containsOnly(users.getUuid());
  }

  @Test
  public void response_status_is_no_content() {
    db.users().insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    loginAsAdmin();

    TestResponse response = newRequest()
      .setParam("id", group.getUuid())
      .setParam("login", user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_group_does_not_exist() {
    UserDto user = db.users().insertUser();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", "42")
        .setParam("login", user.getLogin())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No group with id '42'");
  }

  @Test
  public void fail_if_user_does_not_exist() {
    GroupDto group = db.users().insertGroup("admins");
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", group.getUuid())
        .setParam("login", "my-admin")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Could not find a user with login 'my-admin'");
  }

  @Test
  public void fail_if_not_administrator() {
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();

    assertThatThrownBy(() -> {
      executeRequest(group, user);
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_to_add_user_to_default_group() {
    UserDto user = db.users().insertUser();
    GroupDto defaultGroup = db.users().insertDefaultGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", defaultGroup.getUuid())
        .setParam(PARAM_LOGIN, user.getLogin())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void fail_when_no_default_group() {
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_LOGIN, user.getLogin())
        .setParam(PARAM_GROUP_NAME, group.getName())
        .execute();
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default group cannot be found");
  }

  private void executeRequest(GroupDto groupDto, UserDto userDto) {
    newRequest()
      .setParam("id", groupDto.getUuid())
      .setParam("login", userDto.getLogin())
      .execute();
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

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient()));
  }

}
