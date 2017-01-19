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

import javax.annotation.Nullable;
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
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class AddUserActionTest extends BasePermissionWsTest<AddUserAction> {

  private UserDto user;

  @Before
  public void setUp() {
    user = db.users().insertUser("ray.bradbury");
  }

  @Override
  protected AddUserAction buildWsAction() {
    return new AddUserAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport());
  }

  @Test
  public void add_permission_to_user() throws Exception {
    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGlobalPermissionsOfUser(user, db.getDefaultOrganization())).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_id() throws Exception {
    ComponentDto project = db.components().insertProject();

    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGlobalPermissionsOfUser(user, db.getDefaultOrganization())).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_key() throws Exception {
    ComponentDto project = db.components().insertProject();

    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGlobalPermissionsOfUser(user, db.getDefaultOrganization())).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_view() throws Exception {
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "view-uuid").setKey("view-key"));

    loginAsAdminOnDefaultOrganization();
    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, view.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectGlobalPermissionsOfUser(user, db.getDefaultOrganization())).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user, view)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void fail_when_project_uuid_is_unknown() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_permission_without_project() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    db.components().insertComponent(newFileDto(newProjectDto(db.organizations().insert(), "project-uuid"), null, "file-uuid"));
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(ServerException.class);

    newRequest()
      .setMethod("GET")
      .setParam(PARAM_USER_LOGIN, "george.orwell")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_user_login_is_missing() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, "jrr.tolkien")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    db.components().insertProject();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  @Test
  public void adding_global_permission_fails_if_not_administrator_of_organization() throws Exception {
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project() throws Exception {
    ComponentDto project = db.components().insertProject();
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .execute();
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void adding_project_permission_is_allowed_to_project_administrators() throws Exception {
    ComponentDto project = db.components().insertProject();

    userSession.login().addProjectUuidPermissions(UserRole.ADMIN, project.uuid());

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void sets_root_flag_to_true_when_adding_user_admin_permission_without_org_parameter() throws Exception {
    UserDto rootByUserPermissionUser = db.users().insertRootByUserPermission();
    UserDto rootByGroupPermissionUser = db.users().insertRootByGroupPermission();
    UserDto notRootUser = db.users().insertUser();
    loginAsAdminOnDefaultOrganization();

    executeRequest(notRootUser, SYSTEM_ADMIN);
    db.rootFlag().verify(notRootUser, true);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser);
    db.rootFlag().verifyUnchanged(rootByGroupPermissionUser);

    executeRequest(rootByUserPermissionUser, SYSTEM_ADMIN);
    db.rootFlag().verify(notRootUser, true);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser); // because already has specified user permission
    db.rootFlag().verifyUnchanged(rootByGroupPermissionUser);

    executeRequest(rootByGroupPermissionUser, SYSTEM_ADMIN);
    db.rootFlag().verify(notRootUser, true);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser);
    db.rootFlag().verify(rootByGroupPermissionUser, true);
  }

  @Test
  public void sets_root_flag_to_true_when_adding_user_admin_permission_with_default_organization_uuid() throws Exception {
    UserDto rootByUserPermissionUser = db.users().insertRootByUserPermission();
    UserDto rootByGroupPermissionUser = db.users().insertRootByGroupPermission();
    UserDto notRootUser = db.users().insertUser();
    loginAsAdminOnDefaultOrganization();

    executeRequest(notRootUser, SYSTEM_ADMIN, db.getDefaultOrganization());
    db.rootFlag().verify(notRootUser, true);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser);
    db.rootFlag().verifyUnchanged(rootByGroupPermissionUser);

    executeRequest(rootByUserPermissionUser, SYSTEM_ADMIN, db.getDefaultOrganization());
    db.rootFlag().verify(notRootUser, true);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser); // because already has specified user permission
    db.rootFlag().verifyUnchanged(rootByGroupPermissionUser);

    executeRequest(rootByGroupPermissionUser, SYSTEM_ADMIN, db.getDefaultOrganization());
    db.rootFlag().verify(notRootUser, true);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser);
    db.rootFlag().verify(rootByGroupPermissionUser, true);
  }

  @Test
  public void does_not_set_root_flag_when_adding_user_admin_permission_with_other_organization_uuid() throws Exception {
    OrganizationDto otherOrganization = db.organizations().insert();
    UserDto rootByUserPermissionUser = db.users().insertRootByUserPermission();
    UserDto rootByGroupPermissionUser = db.users().insertRootByGroupPermission();
    UserDto notRootUser = db.users().insertUser();
    loginAsAdmin(otherOrganization);

    executeRequest(notRootUser, SYSTEM_ADMIN, otherOrganization);
    db.rootFlag().verify(notRootUser, false);
    db.rootFlag().verifyUnchanged(rootByUserPermissionUser);
    db.rootFlag().verifyUnchanged(rootByGroupPermissionUser);

    executeRequest(rootByUserPermissionUser, SYSTEM_ADMIN, otherOrganization);
    db.rootFlag().verify(notRootUser, false);
    db.rootFlag().verify(rootByUserPermissionUser, true);
    db.rootFlag().verifyUnchanged(rootByGroupPermissionUser);

    executeRequest(rootByGroupPermissionUser, SYSTEM_ADMIN, otherOrganization);
    db.rootFlag().verify(notRootUser, false);
    db.rootFlag().verify(rootByUserPermissionUser, true);
    db.rootFlag().verify(rootByGroupPermissionUser, true);
  }

  @Test
  public void organization_parameter_must_not_be_set_on_project_permissions() {
    ComponentDto project = db.components().insertProject();
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Organization must not be set when project is set.");

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_ORGANIZATION_KEY, "an_org")
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  private void executeRequest(UserDto userDto, String permission) throws Exception {
    executeRequest(userDto, permission, null);
  }

  private void executeRequest(UserDto userDto, String permission, @Nullable OrganizationDto organizationDto) throws Exception {
    TestRequest request = newRequest()
      .setParam(PARAM_USER_LOGIN, userDto.getLogin())
      .setParam(PARAM_PERMISSION, permission);
    if (organizationDto != null) {
      request.setParam(PARAM_ORGANIZATION_KEY, organizationDto.getKey());
    }
    request.execute();
  }

}
