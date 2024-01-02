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

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class AddGroupActionTest extends BasePermissionWsTest<AddGroupAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";

  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);

  @Override
  protected AddGroupAction buildWsAction() {
    return new AddGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService);
  }

  @Test
  public void verify_definition() {
    Action wsDef = wsTester.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'name' instead."));
  }

  @Test
  public void add_permission_to_group_referenced_by_its_name() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly("admin");
  }

  @Test
  public void reference_group_by_its_name() {
    GroupDto group = db.users().insertGroup("the-group");
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, PROVISION_PROJECTS.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly("provisioning");
  }

  @Test
  public void add_permission_to_group_referenced_by_its_id() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_ID, group.getUuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_id() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
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
  public void add_permission_to_project_referenced_by_its_key() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
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
  public void add_with_portfolio_uuid() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, portfolio.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, portfolio)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void fail_if_project_uuid_is_not_found() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PROJECT_ID, "not-found")
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
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
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component '" + file.getKey() + "' (id: " + file.uuid() + ") must be a project or a view.");
  }

  @Test
  public void adding_a_project_permission_fails_if_project_is_not_set() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    assertThatThrownBy(() -> {
      executeRequest(group, UserRole.ISSUE_ADMIN);
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void adding_a_project_permission_fails_if_component_is_not_a_project() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project, null, "file-uuid"));
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_when_get_request() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setMethod("GET")
        .setParam(PARAM_GROUP_NAME, "sonar-administrators")
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(ServerException.class);
  }

  @Test
  public void fail_when_group_name_and_group_id_are_missing() {
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
  public void fail_when_permission_is_missing() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_not_global_administrator() {
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key can be provided, not both.");
  }

  @Test
  public void adding_global_permission_fails_if_not_administrator() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    userSession.logIn().addPermission(SCAN);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, PROVISION_PROJECTS.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, PROVISION_PROJECTS.getKey())
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void adding_project_permission_is_allowed_to_project_administrators() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
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

    permissionService.getAllProjectPermissions()
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
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_to_group_AnyOne_on_a_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_USER_permission_to_group_on_a_public_project() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_to_group_on_a_public_project() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project)).isEmpty();
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
        .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Project id '%s' not found", branch.uuid()));
  }

  private void executeRequest(GroupDto groupDto, String permission) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_PERMISSION, permission)
      .execute();
  }

}
