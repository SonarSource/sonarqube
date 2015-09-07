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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserRoleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_ID;
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

  UsersAction underTest;

  @Before
  public void setUp() {
    PermissionFinder permissionFinder = new PermissionFinder(dbClient);
    PermissionDependenciesFinder dependenciesFinder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient));
    underTest = new UsersAction(dbClient, userSession, permissionFinder, dependenciesFinder);
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);

    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));
    insertUserRole(new UserRoleDto().setRole(SCAN_EXECUTION).setUserId(user1.getId()));
    insertUserRole(new UserRoleDto().setRole(SCAN_EXECUTION).setUserId(user2.getId()));
    insertUserRole(new UserRoleDto().setRole(SYSTEM_ADMIN).setUserId(user3.getId()));
    commit();
  }

  @Test
  public void search_for_users_with_response_example() {
    db.truncateTables();
    UserDto user1 = insertUser(new UserDto().setLogin("admin").setName("Administrator").setEmail("admin@admin.com"));
    UserDto user2 = insertUser(new UserDto().setLogin("george.orwell").setName("George Orwell").setEmail("george.orwell@1984.net"));
    insertUserRole(new UserRoleDto().setRole(SCAN_EXECUTION).setUserId(user1.getId()));
    insertUserRole(new UserRoleDto().setRole(SCAN_EXECUTION).setUserId(user2.getId()));
    commit();

    String result = ws.newRequest().setParam("permission", "scan").execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("users-example.json"));
  }

  @Test
  public void search_for_users_with_one_permission() {
    String result = ws.newRequest().setParam("permission", "scan").execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("UsersActionTest/users.json"));
  }

  @Test
  public void search_for_users_with_permission_on_project() {
    dbClient.componentDao().insert(dbSession, newProjectDto("project-uuid").setKey("project-key"));
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(dbSession, "project-uuid");
    UserDto user = insertUser(newUserDto().setLogin("project-user-login").setName("project-user-name"));
    insertUserRole(new UserRoleDto().setRole(ISSUE_ADMIN).setUserId(user.getId()).setResourceId(project.getId()));
    commit();
    userSession.login().addProjectUuidPermissions(SYSTEM_ADMIN, "project-uuid");

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains("project-user-login")
      .doesNotContain("login-1");
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
  public void fail_if_project_permission_without_project() {
    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute();
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
      .setParam("permission", SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    ws.newRequest()
      .setParam("permission", SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_if_project_uuid_and_project_key_are_provided() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");
    dbClient.componentDao().insert(dbSession, newProjectDto("project-uuid").setKey("project-key"));
    commit();

    ws.newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  private UserDto insertUser(UserDto userDto) {
    UserDto user = dbClient.userDao().insert(dbSession, userDto.setActive(true));
    commit();
    return user;
  }

  private void insertUserRole(UserRoleDto userRoleDto) {
    dbClient.roleDao().insertUserRole(dbSession, userRoleDto);
    commit();
  }

  private void commit() {
    dbSession.commit();
  }
}
