/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class AddGroupActionTest extends BasePermissionWsTest<AddGroupAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";

  @Override
  protected AddGroupAction buildWsAction() {
    return new AddGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport());
  }

  @Test
  public void add_permission_to_group_referenced_by_its_name() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void reference_group_by_its_name_in_organization() {
    OrganizationDto org = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org, "the-group");
    loginAsAdmin(org);

    newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(PROVISIONING);
  }

  @Test
  public void add_permission_to_group_referenced_by_its_id() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_ID, group.getId().toString())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_id() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), A_PROJECT_UUID).setDbKey(A_PROJECT_KEY));
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, A_PROJECT_UUID)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_key() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), A_PROJECT_UUID).setDbKey(A_PROJECT_KEY));
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_KEY, A_PROJECT_KEY)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_with_view_uuid() {
    OrganizationDto organizationDto = db.getDefaultOrganization();
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    ComponentDto view = db.components().insertComponent(newView(organizationDto, "view-uuid").setDbKey("view-key"));
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, view.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, view)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void fail_if_project_uuid_is_not_found() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, "not-found")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
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
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component '" + file.getDbKey() + "' (id: " + file.uuid() + ") must be a project or a view.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void adding_a_project_permission_fails_if_project_is_not_set() throws Exception {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    executeRequest(group, UserRole.ISSUE_ADMIN);
  }

  @Test
  public void adding_a_project_permission_fails_if_component_is_not_a_project() {
    OrganizationDto organizationDto = db.getDefaultOrganization();
    GroupDto group = db.users().insertGroup(organizationDto, "sonar-administrators");
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, A_PROJECT_UUID).setDbKey(A_PROJECT_KEY));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project, null, "file-uuid"));
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(ServerException.class);

    newRequest()
      .setMethod("GET")
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_group_name_and_group_id_are_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Group name or group id must be provided");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();
  }

  @Test
  public void fail_if_not_administrator_of_organization() {
    GroupDto group = db.users().insertGroup();
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();
  }

  @Test
  public void fail_if_administrator_of_other_organization_only() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group = db.users().insertGroup(org1, "the-group");
    loginAsAdmin(org2);

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_ID, group.getId().toString())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()));
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .execute();
  }

  @Test
  public void adding_global_permission_fails_if_not_administrator_of_organization() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    // user is administrator of another organization
    userSession.logIn().addPermission(ADMINISTER, "anotherOrg");

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .execute();
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void adding_project_permission_is_allowed_to_project_administrators() {
    GroupDto group = db.users().insertGroup(db.getDefaultOrganization(), "sonar-administrators");
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void fails_when_adding_any_permission_to_group_AnyOne_on_a_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    ProjectPermissions.ALL
      .forEach(permission -> {
        try {
          newRequest()
            .setParam(PARAM_GROUP_NAME, "anyone")
            .setParam(PARAM_PROJECT_ID, project.uuid())
            .setParam(PARAM_PERMISSION, permission)
            .execute();
          fail("a BadRequestException should have been raised for " + permission);
        } catch (BadRequestException e) {
          assertThat(e).hasMessage("No permission can be granted to Anyone on a private component");
        }
      });
  }

  @Test
  public void no_effect_when_adding_USER_permission_to_group_AnyOne_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(organization, project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_to_group_AnyOne_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(organization, project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_USER_permission_to_group_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(organization, project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_to_group_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(organization);
    ComponentDto project = db.components().insertPublicProject(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(organization, project)).isEmpty();
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
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
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
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  private void executeRequest(GroupDto groupDto, String permission) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_PERMISSION, permission)
      .execute();
  }

}
