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
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.ws.WsTester;

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
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class GroupsActionTest extends BasePermissionWsTest<GroupsAction> {

  private GroupDto group1;
  private GroupDto group2;
  private GroupDto group3;

  @Override
  protected GroupsAction buildWsAction() {
    return new GroupsAction(
      db.getDbClient(),
      userSession,
      newPermissionWsSupport());
  }

  @Before
  public void setUp() {
    OrganizationDto defOrg = defaultOrganizationProvider.getDto();
    group1 = db.users().insertGroup(defOrg, "group-1-name");
    group2 = db.users().insertGroup(defOrg, "group-2-name");
    group3 = db.users().insertGroup(defOrg, "group-3-name");
    db.users().insertPermissionOnGroup(group1, SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(group2, SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(group3, SYSTEM_ADMIN);
    db.users().insertPermissionOnAnyone(defOrg, SCAN_EXECUTION);
    db.commit();
  }

  @Test
  public void search_for_groups_with_one_permission() throws Exception {
    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .execute()
      .assertJson("{\n" +
        "  \"paging\": {\n" +
        "    \"pageIndex\": 1,\n" +
        "    \"pageSize\": 20,\n" +
        "    \"total\": 3\n" +
        "  },\n" +
        "  \"groups\": [\n" +
        "    {\n" +
        "      \"name\": \"Anyone\",\n" +
        "      \"permissions\": [\n" +
        "        \"scan\"\n" +
        "      ]\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"group-1-name\",\n" +
        "      \"description\": \"" + group1.getDescription() + "\",\n" +
        "      \"permissions\": [\n" +
        "        \"scan\"\n" +
        "      ]\n" +
        "    },\n" +
        "    {\n" +
        "      \"name\": \"group-2-name\",\n" +
        "      \"description\": \"" + group2.getDescription() + "\",\n" +
        "      \"permissions\": [\n" +
        "        \"scan\"\n" +
        "      ]\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
  }

  @Test
  public void search_with_selection() throws Exception {
    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .execute()
      .outputAsString();

    assertThat(result).containsSequence(DefaultGroups.ANYONE, "group-1", "group-2");
  }

  @Test
  public void search_groups_with_pagination() throws Exception {
    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .setParam(PAGE_SIZE, "1")
      .setParam(PAGE, "3")
      .execute()
      .outputAsString();

    assertThat(result).contains("group-2")
      .doesNotContain("group-1")
      .doesNotContain("group-3");
  }

  @Test
  public void search_groups_with_query() throws Exception {
    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .setParam(TEXT_QUERY, "group-")
      .execute()
      .outputAsString();

    assertThat(result)
      .contains("group-1", "group-2")
      .doesNotContain(DefaultGroups.ANYONE);
  }

  @Test
  public void search_groups_with_project_permissions() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto("project-uuid"));
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "project-group-name");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    ComponentDto anotherProject = db.components().insertComponent(newProjectDto());
    GroupDto anotherGroup = db.users().insertGroup(defaultOrganizationProvider.getDto(), "another-project-group-name");
    db.users().insertProjectPermissionOnGroup(anotherGroup, ISSUE_ADMIN, anotherProject);

    GroupDto groupWithoutPermission = db.users().insertGroup(defaultOrganizationProvider.getDto(), "group-without-permission");

    userSession.login().addProjectUuidPermissions(ADMIN, "project-uuid");
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .execute()
      .outputAsString();

    assertThat(result).contains(group.getName())
      .doesNotContain(anotherGroup.getName())
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_also_groups_without_permission_when_search_query() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto("project-uuid"));
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "group-with-permission");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    GroupDto groupWithoutPermission = db.users().insertGroup(defaultOrganizationProvider.getDto(), "group-without-permission");
    GroupDto anotherGroup = db.users().insertGroup(defaultOrganizationProvider.getDto(), "another-group");

    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(TEXT_QUERY, "group-with")
      .execute()
      .outputAsString();

    assertThat(result).contains(group.getName())
      .doesNotContain(groupWithoutPermission.getName())
      .doesNotContain(anotherGroup.getName());
  }

  @Test
  public void return_only_groups_with_permission_when_no_search_query() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto("project-uuid"));
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "project-group-name");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    GroupDto groupWithoutPermission = db.users().insertGroup(defaultOrganizationProvider.getDto(), "group-without-permission");

    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute()
      .outputAsString();

    assertThat(result).contains(group.getName())
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_anyone_group_when_search_query_and_no_param_permission() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto("project-uuid"));
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "group-with-permission");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(TEXT_QUERY, "nyo")
      .execute()
      .outputAsString();

    assertThat(result).contains("Anyone");
  }

  @Test
  public void search_groups_on_views() throws Exception {
    ComponentDto view = db.components().insertComponent(newView("view-uuid").setKey("view-key"));
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "project-group-name");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, view);

    loginAsAdminOnDefaultOrganization();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "view-uuid")
      .execute()
      .outputAsString();

    assertThat(result).contains("project-group-name")
      .doesNotContain("group-1")
      .doesNotContain("group-2")
      .doesNotContain("group-3");
  }

  @Test
  public void fail_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    userSession.anonymous();

    newRequest()
      .setParam(PARAM_PERMISSION, "scan")
      .execute();
  }

  @Test
  public void fail_if_insufficient_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);

    userSession.login("login");
    newRequest()
      .setParam(PARAM_PERMISSION, "scan")
      .execute();
  }

  @Test
  public void fail_if_project_uuid_and_project_key_are_provided() throws Exception {
    db.components().insertComponent(newProjectDto("project-uuid").setKey("project-key"));

    expectedException.expect(BadRequestException.class);

    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam(PARAM_PERMISSION, SCAN_EXECUTION)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return wsTester.newPostRequest(CONTROLLER, "groups");
  }
}
