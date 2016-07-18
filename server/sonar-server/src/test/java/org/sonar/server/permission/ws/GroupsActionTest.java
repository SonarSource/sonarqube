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

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class GroupsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  ComponentDbTester componentDb = new ComponentDbTester(db);
  ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW, "DEV");

  DbClient dbClient;
  DbSession dbSession;
  WsActionTester ws;

  GroupsAction underTest;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    underTest = new GroupsAction(
      dbClient,
      userSession,
      new PermissionDependenciesFinder(dbClient, new ComponentFinder(dbClient), new UserGroupFinder(dbClient), resourceTypes));
    ws = new WsActionTester(underTest);

    userSession.login("login").setGlobalPermissions(SYSTEM_ADMIN);

    GroupDto group1 = insertGroup(new GroupDto().setName("group-1-name").setDescription("group-1-description"));
    GroupDto group2 = insertGroup(new GroupDto().setName("group-2-name").setDescription("group-2-description"));
    GroupDto group3 = insertGroup(new GroupDto().setName("group-3-name").setDescription("group-3-description"));
    insertGroupRole(new GroupRoleDto().setGroupId(group1.getId()).setRole(SCAN_EXECUTION));
    insertGroupRole(new GroupRoleDto().setGroupId(group2.getId()).setRole(SCAN_EXECUTION));
    insertGroupRole(new GroupRoleDto().setGroupId(null).setRole(SCAN_EXECUTION));
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
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .execute().getInput();

    assertThat(result).containsSequence(DefaultGroups.ANYONE, "group-1", "group-2");
  }

  @Test
  public void search_groups_with_pagination() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .setParam(PAGE_SIZE, "1")
      .setParam(PAGE, "3")
      .execute().getInput();

    assertThat(result).contains("group-2")
      .doesNotContain("group-1")
      .doesNotContain("group-3");
  }

  @Test
  public void search_groups_with_query() {
    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .setParam(TEXT_QUERY, "group-")
      .execute().getInput();

    assertThat(result)
      .contains("group-1", "group-2")
      .doesNotContain(DefaultGroups.ANYONE);
  }

  @Test
  public void search_groups_with_project_permissions() {
    userSession.login().addProjectUuidPermissions(ADMIN, "project-uuid");

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
    GroupDto group = insertGroup(new GroupDto().setName("project-group-name"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(project.getId()));

    ComponentDto anotherProject = componentDb.insertComponent(newProjectDto());
    GroupDto anotherGroup = insertGroup(new GroupDto().setName("another-project-group-name"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(anotherGroup.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(anotherProject.getId()));

    GroupDto groupWithoutPermission = insertGroup(new GroupDto().setName("group-without-permission"));

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(anotherGroup.getName())
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_also_groups_without_permission_when_search_query() {
    userSession.login().setGlobalPermissions(SYSTEM_ADMIN);

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
    GroupDto group = insertGroup(new GroupDto().setName("group-with-permission"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(project.getId()));

    GroupDto groupWithoutPermission = insertGroup(new GroupDto().setName("group-without-permission"));
    GroupDto anotherGroup = insertGroup(new GroupDto().setName("another-group"));

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(TEXT_QUERY, "group-with")
      .execute().getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(groupWithoutPermission.getName())
      .doesNotContain(anotherGroup.getName());
  }

  @Test
  public void return_only_groups_with_permission_when_no_search_query() {
    userSession.login().setGlobalPermissions(SYSTEM_ADMIN);

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
    GroupDto group = insertGroup(new GroupDto().setName("project-group-name"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(project.getId()));

    GroupDto groupWithoutPermission = insertGroup(new GroupDto().setName("group-without-permission"));

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute().getInput();

    assertThat(result).contains("project-group-name")
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_anyone_group_when_search_query_and_no_param_permission() {
    userSession.login().setGlobalPermissions(SYSTEM_ADMIN);

    ComponentDto project = componentDb.insertComponent(newProjectDto("project-uuid"));
    GroupDto group = insertGroup(new GroupDto().setName("group-with-permission"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(project.getId()));

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(TEXT_QUERY, "nyo")
      .execute().getInput();

    assertThat(result).contains("Anyone");
  }

  @Test
  public void search_groups_on_views() {
    ComponentDto view = componentDb.insertComponent(newView("view-uuid").setKey("view-key"));
    GroupDto group = insertGroup(new GroupDto().setName("project-group-name"));
    insertGroupRole(new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(ISSUE_ADMIN)
      .setResourceId(view.getId()));

    String result = ws.newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "view-uuid")
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
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
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
