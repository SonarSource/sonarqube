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
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_ID;
import static org.sonar.test.JsonAssert.assertJson;

@Category(DbTests.class)
public class GroupsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient;
  DbSession dbSession;
  WsActionTester ws;

  GroupsAction underTest;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    PermissionFinder permissionFinder = new PermissionFinder(dbClient);
    underTest = new GroupsAction(dbClient, userSession, permissionFinder, new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient)));
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);

    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name").setDescription("group-1-description"));
    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name").setDescription("group-2-description"));
    GroupDto group3 = insertGroup(new GroupDto().setName("group-3-name").setDescription("group-3-description"));
    insertGroupRole(new GroupRoleDto().setGroupId(group1.getId()).setRole(SCAN_EXECUTION));
    insertGroupRole(new GroupRoleDto().setGroupId(group2.getId()).setRole(SCAN_EXECUTION));
    insertGroupRole(new GroupRoleDto().setGroupId(group3.getId()).setRole(SYSTEM_ADMIN));
  }

  @Test
  public void search_for_groups_with_one_permission() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .execute().getInput();

    assertJson(result).isSimilarTo(Resources.getResource(getClass(), "GroupsActionTest/groups.json"));
  }

  @Test
  public void search_with_selection() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermissions.SCAN_EXECUTION)
      .setParam(SELECTED, SelectionMode.ALL.value())
      .execute().getInput();

    assertThat(result).containsSequence(DefaultGroups.ANYONE, "group-1", "group-2", "group-3");
  }

  @Test
  public void search_with_admin_does_not_return_anyone() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermissions.SYSTEM_ADMIN)
      .setParam(SELECTED, SelectionMode.ALL.value())
      .execute().getInput();

    assertThat(result).containsSequence("group-1", "group-2", "group-3")
      .doesNotContain(DefaultGroups.ANYONE);
  }

  @Test
  public void search_groups_with_pagination() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, "scan")
      .setParam(Param.PAGE_SIZE, "1")
      .setParam(Param.PAGE, "2")
      .execute().getInput();

    assertThat(result).contains("group-2")
      .doesNotContain("group-1")
      .doesNotContain("group-3");
  }

  @Test
  public void search_groups_with_query() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, "scan")
      .setParam(Param.TEXT_QUERY, "group-")
      .execute().getInput();

    assertThat(result)
      .contains("group-1", "group-2", "group-3")
      .doesNotContain(DefaultGroups.ANYONE);
  }

  @Test
  public void search_groups_with_project_permissions() {
    dbClient.componentDao().insert(dbSession, newProjectDto("project-uuid").setKey("project-key"));
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(dbSession, "project-uuid");
    GroupDto group = insertGroup(new GroupDto().setName("project-group-name"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(project.getId()));
    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, "project-uuid");

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains("project-group-name")
      .doesNotContain("group-1")
      .doesNotContain("group-2")
      .doesNotContain("group-3");
  }

  @Test
  public void fail_if_project_permission_without_project() {
    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    ws.newRequest()
      .setParam(PARAM_PERMISSION, "scan")
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.login("login");

    ws.newRequest()
      .setParam(PARAM_PERMISSION, "scan")
      .execute();
  }

  @Test
  public void fail_if_permission_is_not_specified() {
    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest()
      .execute();
  }

  @Test
  public void fail_if_project_uuid_and_project_key_are_provided() {
    expectedException.expect(BadRequestException.class);
    dbClient.componentDao().insert(dbSession, newProjectDto("project-uuid").setKey("project-key"));

    ws.newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  private GroupDto insertGroup(GroupDto group) {
    GroupDto result = dbClient.groupDao().insert(dbSession, group);
    commit();

    return result;
  }

  private void insertGroupRole(GroupRoleDto groupRole) {
    dbClient.roleDao().insertGroupRole(dbSession, groupRole);
    commit();
  }

  private void commit() {
    dbSession.commit();
  }
}
