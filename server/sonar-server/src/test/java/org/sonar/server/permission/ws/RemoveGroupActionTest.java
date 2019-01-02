/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class RemoveGroupActionTest extends BasePermissionWsTest<RemoveGroupAction> {

  private GroupDto aGroup;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);

  @Before
  public void setUp() {
    aGroup = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
  }

  @Override
  protected RemoveGroupAction buildWsAction() {
    return new RemoveGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService);
  }

  @Test
  public void remove_permission_using_group_name() {
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, PROVISION_PROJECTS);
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(ADMINISTER.getKey());
  }

  @Test
  public void remove_permission_using_group_id() {
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, PROVISION_PROJECTS);
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_ID, aGroup.getId().toString())
      .setParam(PARAM_PERMISSION, PROVISION_PROJECTS.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(ADMINISTER.getKey());
  }

  @Test
  public void remove_project_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_with_view_uuid() {
    ComponentDto view = db.components().insertView();
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, view);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, view);
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, view.uuid())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, view)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_with_project_key() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void fail_to_remove_last_admin_permission() throws Exception {
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, PROVISION_PROJECTS);
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Last group with permission 'admin'. Permission cannot be removed.");

    executeRequest(aGroup, SYSTEM_ADMIN);
  }

  @Test
  public void fail_when_project_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project id 'unknown-project-uuid' not found");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, ADMINISTER.getKey())
      .execute();
  }

  @Test
  public void fail_when_project_project_permission_without_project() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid global permission 'issueadmin'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");

    executeRequest(aGroup, ISSUE_ADMIN);
  }

  @Test
  public void fail_when_component_is_a_module() {
    ComponentDto module = db.components().insertComponent(newModuleDto(ComponentTesting.newPrivateProjectDto(db.organizations().insert())));

    failIfComponentIsNotAProjectOrView(module);
  }

  @Test
  public void fail_when_component_is_a_directory() {
    ComponentDto file = db.components().insertComponent(newDirectory(ComponentTesting.newPrivateProjectDto(db.organizations().insert()), "A/B"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void fail_when_component_is_a_file() {
    ComponentDto file = db.components().insertComponent(newFileDto(ComponentTesting.newPrivateProjectDto(db.organizations().insert()), null, "file-uuid"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void fail_when_component_is_a_subview() {
    ComponentDto file = db.components().insertComponent(newSubView(ComponentTesting.newView(db.organizations().insert())));

    failIfComponentIsNotAProjectOrView(file);
  }

  private void failIfComponentIsNotAProjectOrView(ComponentDto file) {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component '" + file.getDbKey() + "' (id: " + file.uuid() + ") must be a project or a view.");

    newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
  }

  @Test
  public void fail_when_group_name_is_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Group name or group id must be provided");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_name_and_id_are_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'permission' parameter is missing");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .execute();
  }

  @Test
  public void fail_when_group_id_does_not_exist() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '999999'");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_GROUP_ID, "999999")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .execute();
  }

  private void executeRequest(GroupDto groupDto, String permission) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_PERMISSION, permission)
      .execute();
  }

  private void executeRequest(GroupDto groupDto, OrganizationDto organizationDto, String permission) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_PERMISSION, permission)
      .setParam(PARAM_ORGANIZATION, organizationDto.getKey())
      .execute();
  }

  @Test
  public void removing_global_permission_fails_if_not_administrator_of_organization() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();
  }

  @Test
  public void removing_project_permission_fails_if_not_administrator_of_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .execute();
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void removing_project_permission_is_allowed_to_project_administrators() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnGroup(aGroup, CODEVIEWER, project);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, project);

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(CODEVIEWER);
  }

  @Test
  public void no_effect_when_removing_any_permission_from_group_AnyOne_on_a_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> unsafeInsertProjectPermissionOnAnyone(perm, project));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        newRequest()
          .setParam(PARAM_GROUP_NAME, "anyone")
          .setParam(PARAM_PROJECT_ID, project.uuid())
          .setParam(PARAM_PERMISSION, permission)
          .execute();

        assertThat(db.users().selectAnyonePermissions(db.getDefaultOrganization(), project)).contains(permission);
      });
  }

  @Test
  public void fail_when_removing_USER_permission_from_group_AnyOne_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Permission user can't be removed from a public component");

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();
  }

  @Test
  public void fail_when_removing_CODEVIEWER_permission_from_group_AnyOne_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Permission codeviewer can't be removed from a public component");

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();
  }

  @Test
  public void fail_when_removing_USER_permission_from_group_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Permission user can't be removed from a public component");

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();
  }

  @Test
  public void fail_when_removing_CODEVIEWER_permission_from_group_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Permission codeviewer can't be removed from a public component");

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project key '%s' not found", branch.getDbKey()));

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PROJECT_KEY, branch.getDbKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_using_branch_uuid() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project id '%s' not found", branch.uuid()));

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  private void unsafeInsertProjectPermissionOnAnyone(String perm, ComponentDto project) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setOrganizationUuid(project.getOrganizationUuid())
      .setGroupId(null)
      .setRole(perm)
      .setResourceId(project.getId());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto);
    db.commit();
  }
}
