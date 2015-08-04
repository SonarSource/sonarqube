/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;

@Category(DbTests.class)
public class UsersActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  WsActionTester ws;
  PermissionFinder permissionFinder;

  UsersAction underTest;

  @Before
  public void setUp() {
    permissionFinder = new PermissionFinder(dbClient);
    userSession.login("login").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    underTest = new UsersAction(userSession, permissionFinder);
    ws = new WsActionTester(underTest);

    UserDto user1 = dbClient.userDao().insert(dbSession, new UserDto()
      .setActive(true)
      .setLogin("login-1")
      .setName("name-1"));
    UserDto user2 = dbClient.userDao().insert(dbSession, new UserDto()
      .setActive(true)
      .setLogin("login-2")
      .setName("name-2"));
    UserDto user3 = dbClient.userDao().insert(dbSession, new UserDto()
      .setActive(true)
      .setLogin("login-3")
      .setName("name-3"));
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(GlobalPermissions.SCAN_EXECUTION)
      .setUserId(user1.getId()));
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(GlobalPermissions.SCAN_EXECUTION)
      .setUserId(user2.getId()));
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(GlobalPermissions.SYSTEM_ADMIN)
      .setUserId(user3.getId()));
    dbSession.commit();
  }

  @Test
  public void search_for_users_with_one_permission() {
    String result = ws.newRequest().setParam("permission", "scan").execute().getInput();

    assertJson(result).isSimilarTo(Resources.getResource(getClass(), "UsersActionTest/users.json"));
  }

  @Test
  public void search_for_users_with_response_example() {
    db.truncateTables();
    UserDto user1 = dbClient.userDao().insert(dbSession, new UserDto()
      .setActive(true)
      .setLogin("admin")
      .setName("Administrator"));
    UserDto user2 = dbClient.userDao().insert(dbSession, new UserDto()
      .setActive(true)
      .setLogin("george.orwell")
      .setName("George Orwell"));
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(GlobalPermissions.SCAN_EXECUTION)
      .setUserId(user1.getId()));
    dbClient.roleDao().insertUserRole(dbSession, new UserRoleDto()
      .setRole(GlobalPermissions.SCAN_EXECUTION)
      .setUserId(user2.getId()));
    dbSession.commit();

    String result = ws.newRequest().setParam("permission", "scan").execute().getInput();

    assertJson(result).isSimilarTo(Resources.getResource(getClass(), "users-example.json"));
  }

  @Test
  public void search_for_users_with_query_as_a_parameter() {
    String result = ws.newRequest()
      .setParam("permission", "scan")
      .setParam(Param.TEXT_QUERY, "ame-1")
      .execute().getInput();

    assertThat(result).contains("login-1")
      .doesNotContain("login-2")
      .doesNotContain("login-3");

  }

  @Test
  public void search_for_users_with_select_as_a_parameter() {
    String result = ws.newRequest()
      .setParam("permission", "scan")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute().getInput();

    assertThat(result).contains("login-1", "login-2", "login-3");
  }

  @Test
  public void fail_if_permission_parameter_is_not_filled() {
    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login("login");

    ws.newRequest()
      .setParam("permission", GlobalPermissions.SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    ws.newRequest()
      .setParam("permission", GlobalPermissions.SYSTEM_ADMIN)
      .execute();
  }
}
