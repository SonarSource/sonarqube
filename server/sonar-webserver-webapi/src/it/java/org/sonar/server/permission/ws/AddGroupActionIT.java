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
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class AddGroupActionIT extends BasePermissionWsIT<AddGroupAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";

  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  @Override
  protected AddGroupAction buildWsAction() {
    return new AddGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService, managedInstanceChecker);
  }

  @Test
  public void verify_definition() {
    Action wsDef = wsTester.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("10.0", "Parameter 'groupId' is removed. Use 'groupName' instead."),
      tuple("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'groupName' instead."));
  }

  @Test
  public void add_permission_to_group_referenced_by_its_name() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly("admin");
  }

  @Test
  public void reference_group_by_its_name() {
    GroupDto group = db.users().insertGroup("the-group");
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, GlobalPermission.PROVISION_PROJECTS.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly("provisioning");
  }


  @Test
  public void add_permission_to_project_referenced_by_its_id() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void add_permission_to_project_referenced_by_its_key() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ProjectDto project = db.components().insertPrivateProject(A_PROJECT_UUID, c -> c.setKey(A_PROJECT_KEY)).getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_KEY, A_PROJECT_KEY)
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void add_with_portfolio_uuid() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, portfolio.getUuid())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, portfolio)).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void fail_if_project_uuid_is_not_found() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, "not-found")
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_component_is_a_directory() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newDirectory(project, "A/B"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void fail_when_component_is_a_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "file-uuid"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void fail_when_component_is_a_subview() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newSubPortfolio(project));

    failIfComponentIsNotAProjectOrView(file);
  }

  private void failIfComponentIsNotAProjectOrView(ComponentDto file) {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void fail_when_project_is_managed_and_no_permissions_update() {
    GroupDto group = db.users().insertGroup("whatever");
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    doThrow(new IllegalStateException("Managed project")).when(managedInstanceChecker).throwIfProjectIsManaged(any(), eq(project.getUuid()));

    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, UserRole.CODEVIEWER);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Managed project");

    assertThat(db.users().selectGroupPermissions(group, project)).doesNotContain(UserRole.CODEVIEWER);
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
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project, null, "file-uuid"));
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_get_request() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setMethod("GET")
        .setParam(PARAM_GROUP_NAME, "sonar-administrators")
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(ServerException.class);
  }

  @Test
  public void fail_when_group_name_is_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'groupName' parameter is missing");
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
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
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
    userSession.logIn().addPermission(GlobalPermission.SCAN);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, GlobalPermission.PROVISION_PROJECTS.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project() {
    GroupDto group = db.users().insertGroup("sonar-administrators");
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .setParam(PARAM_PERMISSION, GlobalPermission.PROVISION_PROJECTS.getKey())
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
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void fails_when_adding_any_permission_to_group_AnyOne_on_a_private_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        try {
          newRequest()
            .setParam(PARAM_GROUP_NAME, "anyone")
            .setParam(PARAM_PROJECT_ID, project.getUuid())
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
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project.getUuid())).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_to_group_AnyOne_on_a_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project.getUuid())).isEmpty();
  }

  @Test
  public void no_effect_when_adding_USER_permission_to_group_on_a_public_project() {
    GroupDto group = db.users().insertGroup();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project.getUuid())).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_to_group_on_a_public_project() {
    GroupDto group = db.users().insertGroup();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(project.getUuid())).isEmpty();
  }

  @Test
  public void fail_when_using_branch_uuid() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    assertThatThrownBy(() -> newRequest()
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  private void executeRequest(GroupDto groupDto, String permission) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, groupDto.getName())
      .setParam(PARAM_PERMISSION, permission)
      .execute();
  }

}
