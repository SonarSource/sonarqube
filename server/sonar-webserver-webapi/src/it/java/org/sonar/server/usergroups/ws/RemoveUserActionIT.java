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
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.group.service.GroupMembershipService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;

public class RemoveUserActionIT {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private final GroupMembershipService groupMembershipService = new GroupMembershipService(db.getDbClient(), db.getDbClient().userGroupDao(), db.getDbClient().userDao(),
    db.getDbClient().groupDao());
  private final WsActionTester ws = new WsActionTester(
    new RemoveUserAction(db.getDbClient(), userSession, new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient())), managedInstanceChecker,
      groupMembershipService));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("10.0", "Parameter 'id' is removed. Use 'name' instead."),
      tuple("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));
  }

  @Test
  public void does_nothing_if_user_is_not_in_group() {
    // keep an administrator
    insertAnAdministrator();
    insertDefaultGroup();

    GroupDto group = db.users().insertGroup("admins");
    UserDto user = db.users().insertUser("my-admin");
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).isEmpty();
  }

  @Test
  public void remove_user_by_group_name() {
    insertAnAdministrator();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup("a_group");
    UserDto user = db.users().insertUser("a_user");
    db.users().insertMember(group, user);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).isEmpty();
  }

  @Test
  public void remove_user_only_from_one_group() {
    // keep an administrator
    insertAnAdministrator();
    insertDefaultGroup();

    GroupDto users = db.users().insertGroup("user");
    GroupDto admins = db.users().insertGroup("admins");
    UserDto user = db.users().insertUser("user");
    db.users().insertMember(users, user);
    db.users().insertMember(admins, user);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, admins.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(db.users().selectGroupUuidsOfUser(user)).containsOnly(users.getUuid());
  }

  @Test
  public void response_status_is_no_content() {
    // keep an administrator
    insertAnAdministrator();
    insertDefaultGroup();
    GroupDto users = db.users().insertGroup("users");
    UserDto user = db.users().insertUser("my-admin");
    db.users().insertMember(users, user);
    loginAsAdmin();

    TestResponse response = newRequest()
      .setParam(PARAM_GROUP_NAME, users.getName())
      .setParam(PARAM_LOGIN, user.getLogin())
      .execute();

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void fail_if_unknown_group() {
    UserDto user = db.users().insertUser("my-admin");
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, "unknown")
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_unknown_user() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup("admins");
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, "my-admin");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void throw_ForbiddenException_if_not_administrator() {
    GroupDto group = db.users().insertGroup("a-group");
    UserDto user = db.users().insertUser();
    db.users().insertMember(group, user);
    userSession.logIn("admin");
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_LOGIN, user.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_to_remove_the_last_administrator() {
    db.users().insertDefaultGroup();
    GroupDto adminGroup = db.users().insertGroup("sonar-admins");
    db.users().insertPermissionOnGroup(adminGroup, ADMINISTER);
    UserDto adminUser = db.users().insertUser("the-single-admin");
    db.users().insertMember(adminGroup, adminUser);
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, adminGroup.getName())
      .setParam(PARAM_LOGIN, adminUser.getLogin());

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The last administrator user cannot be removed");
  }

  @Test
  public void fail_to_remove_user_from_default_group() {
    UserDto user = db.users().insertUser();
    GroupDto defaultGroup = db.users().insertDefaultGroup();
    db.users().insertMember(defaultGroup, user);
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
    TestRequest testRequest = newRequest();
    assertThatThrownBy(testRequest::execute)
      .isEqualTo(exception);
  }

  private TestRequest newRequest() {
    return ws.newRequest();
  }

  private void loginAsAdmin() {
    userSession.logIn("admin").addPermission(ADMINISTER);
  }

  private void insertAnAdministrator() {
    db.users().insertAdminByUserPermission();
  }

  private void insertDefaultGroup() {
    db.users().insertDefaultGroup();
  }

}
