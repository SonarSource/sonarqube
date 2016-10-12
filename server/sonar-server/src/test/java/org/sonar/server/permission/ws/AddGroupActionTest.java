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

import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.server.permission.ws.AddGroupAction.ACTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class AddGroupActionTest extends BasePermissionWsTest<AddGroupAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";

  @Override
  protected AddGroupAction buildWsAction() {
    return new AddGroupAction(db.getDbClient(), newPermissionUpdater(), newPermissionWsSupport());
  }

  @Test
  public void add_permission_to_group_referenced_by_its_name() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_group_referenced_by_its_id() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_ID, group.getId().toString())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_id() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, A_PROJECT_UUID)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_key() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_KEY, A_PROJECT_KEY)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_with_view_uuid() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    ComponentDto view = db.components().insertComponent(newView("view-uuid").setKey("view-key"));

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, view.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, view)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void fail_if_project_uuid_is_not_found() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    loginAsAdmin();

    expectedException.expect(NotFoundException.class);
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, "not-found")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void adding_a_project_permission_fails_if_project_is_not_set() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void adding_a_project_permission_fails_if_component_is_not_a_project() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project, null, "file-uuid"));
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    loginAsAdmin();

    expectedException.expect(ServerException.class);

    wsTester.newGetRequest(CONTROLLER, ACTION)
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_group_name_and_group_id_are_missing() throws Exception {
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Group name or group id must be provided");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    loginAsAdmin();

    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, A_PROJECT_KEY)
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void require_admin_permission() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    userSession.login("not-admin");

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return wsTester.newPostRequest(CONTROLLER, ACTION);
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

}
