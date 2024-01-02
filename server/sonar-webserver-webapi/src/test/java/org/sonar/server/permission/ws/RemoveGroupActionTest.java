/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
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
    aGroup = db.users().insertGroup("sonar-administrators");
  }

  @Override
  protected RemoveGroupAction buildWsAction() {
    return new RemoveGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService);
  }

  @Test
  public void verify_definition() {
    Action wsDef = wsTester.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'groupName' instead."));
  }

  @Test
  public void remove_permission_using_group_name() {
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, PROVISION_PROJECTS);
    loginAsAdmin();

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
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_ID, aGroup.getUuid())
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
    loginAsAdmin();

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
    ComponentDto view = db.components().insertPrivatePortfolio();
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertProjectPermissionOnGroup(aGroup, ADMIN, view);
    db.users().insertProjectPermissionOnGroup(aGroup, ISSUE_ADMIN, view);
    loginAsAdmin();

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
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void fail_to_remove_last_admin_permission() {
    db.users().insertPermissionOnGroup(aGroup, ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, PROVISION_PROJECTS);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      executeRequest(aGroup, SYSTEM_ADMIN);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Last group with permission 'admin'. Permission cannot be removed.");
  }

  @Test
  public void fail_when_project_does_not_exist() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
        .setParam(PARAM_PERMISSION, ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project id 'unknown-project-uuid' not found");
  }

  @Test
  public void fail_when_project_project_permission_without_project() {
    loginAsAdmin();

    assertThatThrownBy(() -> executeRequest(aGroup, ISSUE_ADMIN))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Invalid global permission 'issueadmin'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");
  }

  @Test
  public void fail_when_component_is_a_module() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));

    failIfComponentIsNotAProjectOrView(module);
  }

  @Test
  public void fail_when_component_is_a_directory() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newDirectory(project, "A/B"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void fail_when_component_is_a_file() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "file-uuid"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void fail_when_component_is_a_subview() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newSubPortfolio(project));

    failIfComponentIsNotAProjectOrView(file);
  }

  private void failIfComponentIsNotAProjectOrView(ComponentDto file) {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component '" + file.getKey() + "' (id: " + file.uuid() + ") must be a project or a view.");
  }

  @Test
  public void fail_when_group_name_is_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Group name or group id must be provided");
  }

  @Test
  public void fail_when_permission_name_and_id_are_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'permission' parameter is missing");
  }

  @Test
  public void fail_when_group_id_does_not_exist() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .setParam(PARAM_GROUP_ID, "999999")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No group with id '999999'");
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key can be provided, not both.");
  }

  private void executeRequest(GroupDto groupDto, String permission) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_PERMISSION, permission)
      .execute();
  }

  @Test
  public void removing_global_permission_fails_if_not_system_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .setParam(PARAM_PERMISSION, PROVISIONING)
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void removing_project_permission_fails_if_not_administrator_of_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, aGroup.getName())
        .setParam(PARAM_PERMISSION, PROVISIONING)
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
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

        assertThat(db.users().selectAnyonePermissions(project)).contains(permission);
      });
  }

  @Test
  public void fail_when_removing_USER_permission_from_group_AnyOne_on_a_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, "anyone")
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PERMISSION, USER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void fail_when_removing_CODEVIEWER_permission_from_group_AnyOne_on_a_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, "anyone")
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PERMISSION, CODEVIEWER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void fail_when_removing_USER_permission_from_group_on_a_public_project() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PERMISSION, USER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void fail_when_removing_CODEVIEWER_permission_from_group_on_a_public_project() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PERMISSION, CODEVIEWER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void fail_when_using_branch_uuid() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PROJECT_ID, branch.uuid())
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Project id '%s' not found", branch.uuid()));
  }

  private void unsafeInsertProjectPermissionOnAnyone(String perm, ComponentDto project) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(perm)
      .setComponentUuid(project.uuid())
      .setComponentName(project.name());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, project, null);
    db.commit();
  }
}
