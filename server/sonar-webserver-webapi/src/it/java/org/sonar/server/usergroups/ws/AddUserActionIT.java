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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.group.service.GroupMembershipService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;

public class AddUserActionIT {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private GroupMembershipService groupMembershipService = new GroupMembershipService(db.getDbClient(), db.getDbClient().userGroupDao(), db.getDbClient().userDao(), db.getDbClient()
    .groupDao());

  private final WsActionTester ws = new WsActionTester(new AddUserAction(db.getDbClient(), userSession, newGroupWsSupport(), managedInstanceChecker, groupMembershipService));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("10.4", "Deprecated. Use POST /api/v2/authorizations/group-memberships instead"),
      tuple("10.0", "Parameter 'id' is removed. Use 'name' instead."),
      tuple("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));
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
      .setParam(PARAM_GROUP_NAME, admins.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).containsOnly(admins.getUuid(), users.getUuid());
  }

  @Test
  public void fail_if_user_is_already_member_of_group() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup(g -> g.setName("group1"));
    UserDto user = db.users().insertUser(u -> u.setLogin("user1"));
    db.users().insertMember(group, user);
    loginAsAdmin();

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatIllegalArgumentException().isThrownBy(testRequest::execute)
      .withMessage("User 'user1' is already a member of group 'group1'");
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
      .setParam(PARAM_GROUP_NAME, users.getName())
      .setParam(PARAM_LOGIN, user2.getLogin())
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
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_group_does_not_exist() {
    UserDto user = db.users().insertUser();
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, "unknown")
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No group with name 'unknown'");
  }

  @Test
  public void fail_if_user_does_not_exist() {
    GroupDto group = db.users().insertGroup("admins");
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, "my-admin");

    assertThatThrownBy(request::execute)
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
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, defaultGroup.getName())
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void fail_if_instance_is_externally_managed() {
    loginAsAdmin();
    BadRequestException exception = BadRequestException.create("Not allowed");
    doThrow(exception).when(managedInstanceChecker).throwIfInstanceIsManaged();
    TestRequest testRequest = newRequest()
      .setParam("name", "long-desc");
    assertThatThrownBy(testRequest::execute)
      .isEqualTo(exception);
  }

  private void executeRequest(GroupDto groupDto, UserDto userDto) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_LOGIN, userDto.getLogin())
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
