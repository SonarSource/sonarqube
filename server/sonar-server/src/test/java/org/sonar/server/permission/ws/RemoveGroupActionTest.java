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
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class RemoveGroupActionTest extends BasePermissionWsTest<RemoveGroupAction> {

  private GroupDto aGroup;

  @Before
  public void setUp() {
    aGroup = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
  }

  @Override
  protected RemoveGroupAction buildWsAction() {
    return new RemoveGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport());
  }

  @Test
  public void remove_permission_using_group_name() throws Exception {
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertPermissionOnGroup(aGroup, PROVISIONING);
    loginAsAdminOnDefaultOrganization();

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
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam(PARAM_GROUP_ID, aGroup.getId().toString())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void remove_project_permission() throws Exception {
    ComponentDto project = db.components().insertProject();
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);
    loginAsAdminOnDefaultOrganization();

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
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "view-uuid").setKey("view-key"));
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, view);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, view);
    loginAsAdminOnDefaultOrganization();

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
    ComponentDto project = db.components().insertProject();
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(SYSTEM_ADMIN);
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void fail_to_remove_last_admin_permission() throws Exception {
    db.users().insertPermissionOnGroup(aGroup, SYSTEM_ADMIN);
    db.users().insertPermissionOnGroup(aGroup, PROVISIONING);
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Last group with permission 'admin'. Permission cannot be removed.");

    executeRequest(aGroup, SYSTEM_ADMIN);
  }

  @Test
  public void fail_when_project_does_not_exist() throws Exception {
    loginAsAdminOnDefaultOrganization();

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
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid global permission 'issueadmin'. Valid values are [admin, profileadmin, gateadmin, scan, provisioning]");

    executeRequest(aGroup, ISSUE_ADMIN);
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    ComponentDto file = db.components().insertComponent(newFileDto(newProjectDto(db.organizations().insert()), null, "file-uuid"));
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'KEY_file-uuid' (id: file-uuid) must be a project or a module.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_group_name_is_missing() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Group name or group id must be provided");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_name_and_id_are_missing() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'permission' parameter is missing");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .execute();
  }

  @Test
  public void fail_when_group_id_does_not_exist() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '42'");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_GROUP_ID, "42")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    ComponentDto project = db.components().insertProject();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute();
  }

  @Test
  public void sets_root_flag_to_false_on_all_users_in_group_when_removing_admin_permission_from_group_of_default_organization_without_org_param() throws Exception {
    UserDto lastAdminUser = db.users().insertRootByUserPermission();
    GroupDto adminGroup = db.users().insertAdminGroup();
    UserDto user1 = db.users().insertRootByGroupPermission("user1", adminGroup);
    UserDto user2 = db.users().insertRootByGroupPermission("user2", adminGroup);
    loginAsAdminOnDefaultOrganization();

    executeRequest(adminGroup, SYSTEM_ADMIN);

    db.rootFlag().verify(user1, false);
    db.rootFlag().verify(user2, false);
    db.rootFlag().verifyUnchanged(lastAdminUser);
  }

  @Test
  public void sets_root_flag_to_false_on_all_users_in_group_when_removing_admin_permission_from_group_of_default_organization_with_org_param() throws Exception {
    UserDto lastAdminUser = db.users().insertRootByUserPermission();
    GroupDto adminGroup = db.users().insertAdminGroup();
    UserDto user1 = db.users().insertRootByGroupPermission("user1", adminGroup);
    UserDto user2 = db.users().insertRootByGroupPermission("user2", adminGroup);
    loginAsAdminOnDefaultOrganization();

    executeRequest(adminGroup, db.getDefaultOrganization(), SYSTEM_ADMIN);

    db.rootFlag().verify(user1, false);
    db.rootFlag().verify(user2, false);
    db.rootFlag().verifyUnchanged(lastAdminUser);
  }

  @Test
  public void does_not_set_root_flag_to_false_on_all_users_in_group_when_removing_admin_permission_from_group_of_other_organization() throws Exception {
    OrganizationDto otherOrganization = db.organizations().insert();
    UserDto lastAdmin = db.users().insertUser();
    db.users().insertPermissionOnUser(otherOrganization, lastAdmin, SYSTEM_ADMIN);
    GroupDto adminGroup = db.users().insertAdminGroup(otherOrganization);
    UserDto rootByUserPermissionUser = db.users().insertRootByUserPermission();
    UserDto rootByGroupPermissionUser = db.users().insertRootByGroupPermission();
    UserDto inAdminGroupUser = db.users().insertUser();
    UserDto notInGroupUser = db.users().insertUser();
    db.users().insertMembers(adminGroup, rootByUserPermissionUser, rootByGroupPermissionUser, inAdminGroupUser);
    loginAsAdmin(otherOrganization);

    executeRequest(adminGroup, otherOrganization, SYSTEM_ADMIN);

    db.rootFlag().verify(rootByUserPermissionUser, true);
    db.rootFlag().verify(rootByGroupPermissionUser, true);
    db.rootFlag().verify(inAdminGroupUser, false);
    db.rootFlag().verifyUnchanged(notInGroupUser);
  }

  private void executeRequest(GroupDto groupDto, String permission) throws Exception {
    newRequest()
        .setParam(PARAM_GROUP_NAME, groupDto.getName())
        .setParam(PARAM_PERMISSION, permission)
        .execute();
  }

  private void executeRequest(GroupDto groupDto, OrganizationDto organizationDto, String permission) throws Exception {
    newRequest()
        .setParam(PARAM_GROUP_NAME, groupDto.getName())
        .setParam(PARAM_PERMISSION, permission)
        .setParam(PARAM_ORGANIZATION_KEY, organizationDto.getKey())
        .execute();
  }

  @Test
  public void removing_global_permission_fails_if_not_administrator_of_organization() throws Exception {
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();
  }

  @Test
  public void removing_project_permission_fails_if_not_administrator_of_project() throws Exception {
    ComponentDto project = db.components().insertProject();
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .setParam(PARAM_PROJECT_KEY, project.key())
      .execute();
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void removing_project_permission_is_allowed_to_project_administrators() throws Exception {
    ComponentDto project = db.components().insertProject();
    db.users().insertProjectPermissionOnGroup(aGroup, CODEVIEWER, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);

    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, project.uuid());
    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(CODEVIEWER);
  }
}
