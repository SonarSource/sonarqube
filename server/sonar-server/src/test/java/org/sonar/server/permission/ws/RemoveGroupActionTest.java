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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.server.permission.ws.RemoveGroupAction.ACTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class RemoveGroupActionTest extends BasePermissionWsTest<RemoveGroupAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";

  private GroupDto aGroup;

  @Before
  public void setUp() {
    aGroup = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
  }

  @Override
  protected RemoveGroupAction buildWsAction() {
    return new RemoveGroupAction(db.getDbClient(), newPermissionUpdater(), newPermissionWsSupport());
  }

  @Test
  public void remove_permission_using_group_name() throws Exception {
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertPermissionOnGroup(aGroup, PROVISIONING);

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void remove_permission_using_group_id() throws Exception {
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertPermissionOnGroup(aGroup, PROVISIONING);

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_ID, aGroup.getId().toString())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void remove_project_permission() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_with_view_uuid() throws Exception {
    ComponentDto view = db.components().insertComponent(newView("view-uuid").setKey("view-key"));
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, view);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, view);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, view.uuid())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
    assertThat(db.users().selectGroupPermissions(aGroup, view)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_with_project_key() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void fail_to_remove_last_sysadmin_permission() throws Exception {
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertPermissionOnGroup(aGroup, PROVISIONING);
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Last group with 'admin' permission. Permission cannot be removed.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project id 'unknown-project-uuid' not found");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_project_permission_without_project() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid global permission 'issueadmin'. Valid values are [admin, profileadmin, gateadmin, scan, provisioning]");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    ComponentDto file = db.components().insertComponent(newFileDto(newProjectDto(A_PROJECT_UUID), null, "file-uuid"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'KEY_file-uuid' (id: file-uuid) must be a project or a module.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);
    expectedException.expectMessage("HTTP method POST is required");

    wsTester.newGetRequest(CONTROLLER, ACTION)
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_group_name_is_missing() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Group name or group id must be provided");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_name_and_id_are_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'permission' parameter is missing");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .execute();
  }

  @Test
  public void fail_when_group_id_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '42'");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_GROUP_ID, "42")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return wsTester.newPostRequest(CONTROLLER, ACTION);
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

}
