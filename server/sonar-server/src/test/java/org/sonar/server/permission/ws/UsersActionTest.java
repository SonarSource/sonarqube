/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.PermissionDbTester;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPermissionDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class UsersActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  UserDbTester userDb = new UserDbTester(db);
  PermissionDbTester permissionDb = new PermissionDbTester(db);
  ComponentDbTester componentDbTester = new ComponentDbTester(db);

  WsActionTester ws;
  ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");

  UsersAction underTest;

  @Before
  public void setUp() {
    PermissionFinder permissionFinder = new PermissionFinder(dbClient);
    PermissionDependenciesFinder dependenciesFinder = new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient), new UserGroupFinder(dbClient), resourceTypes);
    underTest = new UsersAction(dbClient, userSession, permissionFinder, dependenciesFinder);
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void search_for_users_with_response_example() {
    UserDto user2 = userDb.insertUser(new UserDto().setLogin("george.orwell").setName("George Orwell").setEmail("george.orwell@1984.net"));
    UserDto user1 = userDb.insertUser(new UserDto().setLogin("admin").setName("Administrator").setEmail("admin@admin.com"));
    permissionDb.addGlobalPermissionToUser(SCAN_EXECUTION, user2.getId());
    permissionDb.addGlobalPermissionToUser(SYSTEM_ADMIN, user1.getId());
    permissionDb.addGlobalPermissionToUser(QUALITY_GATE_ADMIN, user1.getId());
    permissionDb.addGlobalPermissionToUser(QUALITY_PROFILE_ADMIN, user1.getId());

    String result = ws.newRequest().execute().getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(getClass().getResource("users-example.json"));
  }

  @Test
  public void search_for_users_with_one_permission() {
    insertUsersHavingGlobalPermissions();
    String result = ws.newRequest().setParam("permission", "scan").execute().getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(getClass().getResource("UsersActionTest/users.json"));
  }

  @Test
  public void search_for_users_with_permission_on_project() {
    userSession.login().addProjectUuidPermissions(SYSTEM_ADMIN, "project-uuid");

    // User have permission on project
    ComponentDto project = componentDbTester.insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    UserDto user = userDb.insertUser(newUserDto());
    insertUserRole(new UserPermissionDto().setPermission(ISSUE_ADMIN).setUserId(user.getId()).setComponentId(project.getId()));

    // User have permission on another project
    ComponentDto anotherProject = componentDbTester.insertComponent(newProjectDto());
    UserDto userHavePermissionOnAnotherProject = userDb.insertUser(newUserDto());
    insertUserRole(new UserPermissionDto().setPermission(ISSUE_ADMIN).setUserId(userHavePermissionOnAnotherProject.getId()).setComponentId(anotherProject.getId()));

    // User has no permission
    UserDto withoutPermission = userDb.insertUser(newUserDto());

    dbSession.commit();

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains(user.getLogin())
      .doesNotContain(userHavePermissionOnAnotherProject.getLogin())
      .doesNotContain(withoutPermission.getLogin());
  }

  @Test
  public void search_only_for_users_with_permission_when_no_search_query() {
    userSession.login().setGlobalPermissions(SYSTEM_ADMIN);

    // User have permission on project
    ComponentDto project = componentDbTester.insertComponent(newProjectDto());
    UserDto user = userDb.insertUser(newUserDto());
    insertUserRole(new UserPermissionDto().setPermission(ISSUE_ADMIN).setUserId(user.getId()).setComponentId(project.getId()));

    // User has no permission
    UserDto withoutPermission = userDb.insertUser(newUserDto());

    dbSession.commit();

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute().getInput();

    assertThat(result).contains(user.getLogin())
      .doesNotContain(withoutPermission.getLogin());
  }

  @Test
  public void search_also_for_users_without_permission_when_search_query() {
    userSession.login().setGlobalPermissions(SYSTEM_ADMIN);

    // User have permission on project
    ComponentDto project = componentDbTester.insertComponent(newProjectDto());
    UserDto user = userDb.insertUser(newUserDto("with-permission", "with-permission", null));
    insertUserRole(new UserPermissionDto().setPermission(ISSUE_ADMIN).setUserId(user.getId()).setComponentId(project.getId()));

    // User has no permission
    UserDto withoutPermission = userDb.insertUser(newUserDto("without-permission", "without-permission", null));
    UserDto anotherUser = userDb.insertUser(newUserDto("another-user", "another-user", null));

    dbSession.commit();

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(TEXT_QUERY, "with")
      .execute().getInput();

    assertThat(result).contains(user.getLogin())
      .contains(withoutPermission.getLogin())
      .doesNotContain(anotherUser.getLogin());
  }

  @Test
  public void search_for_users_with_query_as_a_parameter() {
    insertUsersHavingGlobalPermissions();
    String result = ws.newRequest()
      .setParam("permission", "scan")
      .setParam(TEXT_QUERY, "ame-1")
      .execute().getInput();

    assertThat(result).contains("login-1")
      .doesNotContain("login-2")
      .doesNotContain("login-3");
  }

  @Test
  public void search_for_users_with_select_as_a_parameter() {
    insertUsersHavingGlobalPermissions();
    String result = ws.newRequest()
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
    dbSession.commit();

    ws.newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  @Test
  public void fail_if_search_query_is_too_short() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'q' parameter must have at least 3 characters");

    ws.newRequest().setParam(TEXT_QUERY, "ab").execute();
  }

  private UserDto insertUser(UserDto userDto) {
    UserDto user = dbClient.userDao().insert(dbSession, userDto.setActive(true));
    dbSession.commit();
    return user;
  }

  private void insertUserRole(UserPermissionDto userPermissionDto) {
    dbClient.roleDao().insertUserRole(dbSession, userPermissionDto);
    dbSession.commit();
  }

  private void insertUsersHavingGlobalPermissions() {
    UserDto user3 = insertUser(new UserDto().setLogin("login-3").setName("name-3").setEmail("email-3"));
    UserDto user2 = insertUser(new UserDto().setLogin("login-2").setName("name-2").setEmail("email-2"));
    UserDto user1 = insertUser(new UserDto().setLogin("login-1").setName("name-1").setEmail("email-1"));
    insertUserRole(new UserPermissionDto().setPermission(SCAN_EXECUTION).setUserId(user1.getId()));
    insertUserRole(new UserPermissionDto().setPermission(SYSTEM_ADMIN).setUserId(user3.getId()));
    insertUserRole(new UserPermissionDto().setPermission(SCAN_EXECUTION).setUserId(user2.getId()));
    dbSession.commit();
  }
}
