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
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.ws.TestRequest;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserActionTest extends BasePermissionWsTest<RemoveUserAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";
  private static final String A_LOGIN = "ray.bradbury";

  private UserDto user;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);

  @Before
  public void setUp() {
    user = db.users().insertUser(A_LOGIN);
  }

  @Override
  protected RemoveUserAction buildWsAction() {
    return new RemoveUserAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService);
  }

  @Test
  public void remove_permission_from_user() {
    db.users().insertPermissionOnUser(user, PROVISION_PROJECTS);
    db.users().insertPermissionOnUser(user, ADMINISTER_QUALITY_GATES);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, QUALITY_GATE_ADMIN)
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user)).containsOnly(PROVISION_PROJECTS);
  }

  @Test
  public void admin_can_not_remove_his_global_admin_right() {
    db.users().insertPermissionOnUser(user, ADMINISTER);
    loginAsAdmin();
    UserDto admin = db.users().insertUser(userSession.getLogin());
    db.users().insertPermissionOnUser(admin, ADMINISTER);

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, userSession.getLogin())
      .setParam(PARAM_PERMISSION, ADMINISTER.getKey());

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("As an admin, you can't remove your own admin right");
  }

  @Test
  public void project_admin_can_not_remove_his_project_admin_right() {
    loginAsAdmin();
    UserDto admin = db.users().insertUser(userSession.getLogin());
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(admin, ADMINISTER.getKey(), project);

    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, userSession.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ADMINISTER.getKey());

    assertThatThrownBy(() -> request.execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("As an admin, you can't remove your own admin right");
  }

  @Test
  public void fail_to_remove_admin_permission_if_last_admin() {
    db.users().insertPermissionOnUser(user, ADMINISTER);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Last user with permission 'admin'. Permission cannot be removed.");
  }

  @Test
  public void remove_permission_from_project() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_with_project_key() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, project);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(CODEVIEWER);
  }

  @Test
  public void remove_with_view_uuid() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, view);
    db.users().insertProjectPermissionOnUser(user, ADMIN, view);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, view.getKey())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectProjectPermissionsOfUser(user, view)).containsOnly(ADMIN);
  }

  @Test
  public void fail_when_project_does_not_exist() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
        .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_project_permission_without_permission() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
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
    ComponentDto portfolio = db.components().insertPrivatePortfolio();
    ComponentDto file = db.components().insertComponent(newSubPortfolio(portfolio));

    failIfComponentIsNotAProjectOrView(file);
  }

  private void failIfComponentIsNotAProjectOrView(ComponentDto file) {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, file.uuid())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component '" + file.getKey() + "' (id: " + file.uuid() + ") must be a project or a view.");
  }

  @Test
  public void fail_when_get_request() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setMethod("GET")
        .setParam(PARAM_USER_LOGIN, "george.orwell")
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(ServerException.class);
  }

  @Test
  public void fail_when_user_login_is_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_permission_is_missing() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject();
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key can be provided, not both.");
  }

  @Test
  public void removing_global_permission_fails_if_not_system_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
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
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
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
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, project);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(CODEVIEWER);
  }

  @Test
  public void fail_when_removing_USER_permission_on_a_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PERMISSION, USER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");

  }

  @Test
  public void fail_when_removing_CODEVIEWER_permission_on_a_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PERMISSION, CODEVIEWER)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PROJECT_ID, branch.uuid())
        .setParam(PARAM_USER_LOGIN, user.getLogin())
        .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Project id '%s' not found", branch.uuid()));
  }

}
