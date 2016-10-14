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
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.server.permission.ws.RemoveUserAction.ACTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserActionTest extends BasePermissionWsTest<RemoveUserAction> {

  private static final String A_PROJECT_UUID = "project-uuid";
  private static final String A_PROJECT_KEY = "project-key";
  private static final String A_LOGIN = "ray.bradbury";

  private UserDto user;

  @Before
  public void setUp() {
    user = db.users().insertUser(A_LOGIN);
  }

  @Override
  protected RemoveUserAction buildWsAction() {
    return new RemoveUserAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport());
  }

  @Test
  public void remove_permission_from_user() throws Exception {
    db.users().insertPermissionOnUser(user, PROVISIONING);
    db.users().insertPermissionOnUser(user, QUALITY_GATE_ADMIN);
    loginAsAdminOnDefaultOrganization();

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, QUALITY_GATE_ADMIN)
      .execute();

    assertThat(db.users().selectUserPermissions(user, null)).containsOnly(PROVISIONING);
  }

  @Test
  public void fail_to_remove_admin_permission_if_last_admin() throws Exception {
    db.users().insertPermissionOnUser(user, CODEVIEWER);
    db.users().insertPermissionOnUser(user, ADMIN);
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Last user with 'admin' permission. Permission cannot be removed.");

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, ADMIN)
      .execute();
  }

  @Test
  public void remove_permission_from_project() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, project);
    loginAsAdminOnDefaultOrganization();

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectUserPermissions(user, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_with_project_key() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, project);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    loginAsAdminOnDefaultOrganization();

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectUserPermissions(user, project)).containsOnly(CODEVIEWER);
  }

  @Test
  public void remove_with_view_uuid() throws Exception {
    ComponentDto view = db.components().insertComponent(newView("view-uuid").setKey("view-key"));
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, view);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, view);
    loginAsAdminOnDefaultOrganization();

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, view.getKey())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectUserPermissions(user, view)).containsOnly(CODEVIEWER);
  }

  @Test
  public void fail_when_project_does_not_exist() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_permission_without_permission() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    db.components().insertComponent(newFileDto(newProjectDto(), null, "file-uuid"));
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(ServerException.class);

    wsTester.newGetRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, "george.orwell")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_user_login_is_missing() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    ComponentDto project = db.components().insertComponent(newProjectDto(A_PROJECT_UUID).setKey(A_PROJECT_KEY));
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute();
  }

  @Test
  public void sets_root_flag_to_false_when_removing_user_admin_permission_of_default_organization_without_org_parameter() throws Exception {
    UserDto lastAdminUser = db.users().insertRootByUserPermission();
    UserDto adminUser = db.users().insertRootByUserPermission();
    loginAsAdminOnDefaultOrganization();

    executeRequest(adminUser, SYSTEM_ADMIN);

    db.rootFlag().verify(adminUser, false);
  }

  @Test
  public void sets_root_flag_to_false_when_removing_user_admin_permission_of_default_organization_with_org_parameter() throws Exception {
    UserDto lastAdminUser = db.users().insertRootByUserPermission();
    UserDto adminUser = db.users().insertRootByUserPermission();
    loginAsAdminOnDefaultOrganization();

    executeRequest(adminUser, db.getDefaultOrganization(), SYSTEM_ADMIN);

    db.rootFlag().verify(adminUser, false);
  }

  @Test
  public void does_not_set_root_flag_to_false_when_removing_user_admin_permission_of_other_organization() throws Exception {
    UserDto rootUser = db.users().insertRootByUserPermission();
    UserDto notRootUser = db.users().insertUser();
    OrganizationDto otherOrganization = db.organizations().insert();
    db.users().insertPermissionOnUser(otherOrganization, rootUser, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(otherOrganization, notRootUser, SYSTEM_ADMIN);
    loginAsAdmin(otherOrganization);

    executeRequest(rootUser, otherOrganization, SYSTEM_ADMIN);
    db.rootFlag().verify(rootUser, true);
    db.rootFlag().verifyUnchanged(notRootUser);

    executeRequest(notRootUser, otherOrganization, SYSTEM_ADMIN);
    db.rootFlag().verify(rootUser, true);
    db.rootFlag().verify(notRootUser, false);
  }

  private void executeRequest(UserDto userDto, OrganizationDto organizationDto, String permission) throws Exception {
    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, userDto.getLogin())
      .setParam(PARAM_PERMISSION, permission)
      .setParam(PARAM_ORGANIZATION_KEY, organizationDto.getKey())
      .execute();
  }

  private void executeRequest(UserDto userDto, String permission) throws Exception {
    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, userDto.getLogin())
      .setParam(PARAM_PERMISSION, permission)
      .execute();
  }

  @Test
  public void removing_global_permission_fails_if_not_administrator_of_organization() throws Exception {
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, PROVISIONING)
      .execute();
  }

  @Test
  public void removing_project_permission_fails_if_not_administrator_of_project() throws Exception {
    ComponentDto project = db.components().insertProject();
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_KEY, project.key())
      .execute();
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void removing_project_permission_is_allowed_to_project_administrators() throws Exception {
    ComponentDto project = db.components().insertProject();
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(user, ISSUE_ADMIN, project);
    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, project.uuid());

    wsTester.newPostRequest(CONTROLLER, ACTION)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectUserPermissions(user, project)).containsOnly(CODEVIEWER);
  }

}
