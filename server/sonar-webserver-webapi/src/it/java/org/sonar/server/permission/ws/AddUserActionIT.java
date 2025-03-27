/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.config.Configuration;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class AddUserActionIT extends BasePermissionWsIT<AddUserAction> {

  private UserDto user;
  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final Configuration configuration = mock(Configuration.class);
  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  @Before
  public void setUp() {
    user = db.users().insertUser("ray.bradbury");
  }

  @Override
  protected AddUserAction buildWsAction() {
    return new AddUserAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(),
      wsParameters, permissionService, configuration, managedInstanceChecker);
  }

  @Test
  public void add_permission_to_user() {
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user)).containsOnly(GlobalPermission.ADMINISTER);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_id() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user, project.getUuid())).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void add_permission_to_project_referenced_by_its_key() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user, project.getUuid())).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void add_permission_to_view() {
    PortfolioDto portfolioDto = db.components().insertPrivatePortfolioDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, portfolioDto.getUuid())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user, portfolioDto.getUuid())).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void fail_when_project_uuid_is_unknown() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
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
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void fail_when_project_permission_without_project() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, ProjectPermission.ISSUE_ADMIN.getKey())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_when_component_is_not_a_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newFileDto(project, null, "file-uuid"));
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, "file-uuid")
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void succeed_when_project_is_managed_and_user_is_sysadmin() {
    loginAsAdmin();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    doThrow(new IllegalStateException("Managed project")).when(managedInstanceChecker).throwIfProjectIsManaged(any(), eq(project.getUuid()));

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, ProjectPermission.SCAN.getKey())
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user, project.getUuid())).containsOnly(ProjectPermission.SCAN.getKey());
  }

  @Test
  public void fail_when_project_is_managed_and_user_not_sysadmin() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    doThrow(new IllegalStateException("Managed project")).when(managedInstanceChecker).throwIfProjectIsManaged(any(), eq(project.getUuid()));

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ProjectPermission.CODEVIEWER.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Managed project");

    assertThat(db.users().selectEntityPermissionOfUser(user, project.getUuid())).doesNotContain(ProjectPermission.CODEVIEWER.getKey());
  }

  @Test
  public void fail_when_get_request() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setMethod("GET")
        .setParam(PARAM_USER_LOGIN, "george.orwell")
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(ServerException.class);
  }

  @Test
  public void fail_when_user_login_is_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_permission_is_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, "jrr.tolkien")
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    db.components().insertPrivateProject().getMainBranchComponent();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, "project-uuid")
        .setParam(PARAM_PROJECT_KEY, "project-key")
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key can be provided, not both.");
  }

  @Test
  public void adding_global_permission_fails_if_not_system_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .setParam(PARAM_PROJECT_KEY, project.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void adding_project_permission_fails_if_user_doesnt_exist_and_not_administrator_of_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, "unknown")
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .setParam(PARAM_PROJECT_KEY, project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project_and_login_param_is_missing() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    TestRequest request = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .setParam(PARAM_PROJECT_KEY, project.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void adding_project_permission_is_allowed_to_project_administrators() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ProjectPermission.ISSUE_ADMIN.getKey())
      .execute();

    assertThat(db.users().selectEntityPermissionOfUser(user, project.getUuid())).containsOnly(ProjectPermission.ISSUE_ADMIN.getKey());
  }

  @Test
  public void no_effect_when_adding_USER_permission_on_a_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, ProjectPermission.USER.getKey())
      .execute();

    assertThat(db.users().selectAnyonePermissions(project.getUuid())).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_on_a_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, ProjectPermission.CODEVIEWER.getKey())
      .execute();

    assertThat(db.users().selectAnyonePermissions(project.getUuid())).isEmpty();
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    TestRequest request = newRequest()
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void adding_permission_to_user_fails_if_not_authenticated_and_existing_login() {
    userSession.anonymous();

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void adding_permission_to_user_fails_if_not_authenticated_and_non_existing_login() {
    userSession.anonymous();

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, "non-existing-login")
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }
}
