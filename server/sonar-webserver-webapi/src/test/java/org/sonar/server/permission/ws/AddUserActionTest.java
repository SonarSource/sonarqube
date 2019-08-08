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
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class AddUserActionTest extends BasePermissionWsTest<AddUserAction> {

  private UserDto user;
  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private WsParameters wsParameters = new WsParameters(permissionService);

  @Before
  public void setUp() {
    user = db.users().insertUser("ray.bradbury");
    db.organizations().addMember(db.getDefaultOrganization(), user);
  }

  @Override
  protected AddUserAction buildWsAction() {
    return new AddUserAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService);
  }

  @Test
  public void add_permission_to_user_on_default_organization_if_organization_is_not_specified() {
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user, db.getDefaultOrganization())).containsOnly(ADMINISTER);
  }

  @Test
  public void add_permission_to_user_on_specified_organization() {
    OrganizationDto organization = db.organizations().insert();
    addUserAsMemberOfOrganization(organization);
    loginAsAdmin(organization);

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user, organization)).containsOnly(ADMINISTER);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_id() {
    OrganizationDto organization = db.organizations().insert();
    addUserAsMemberOfOrganization(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    loginAsAdmin(organization);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user, organization)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_project_referenced_by_its_key() {
    ComponentDto project = db.components().insertPrivateProject();
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user, db.getDefaultOrganization())).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void add_permission_to_view() {
    ComponentDto view = db.components().insertComponent(newView(db.getDefaultOrganization(), "view-uuid").setDbKey("view-key"));
    loginAsAdmin(db.getDefaultOrganization());

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, view.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    assertThat(db.users().selectPermissionsOfUser(user, db.getDefaultOrganization())).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user, view)).containsOnly(SYSTEM_ADMIN);
  }

  @Test
  public void fail_when_project_uuid_is_unknown() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
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
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component '" + file.getDbKey() + "' (id: " + file.uuid() + ") must be a project or a view.");

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_permission_without_project() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() {
    db.components().insertComponent(newFileDto(newPrivateProjectDto(db.organizations().insert(), "project-uuid"), null, "file-uuid"));
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(ServerException.class);

    newRequest()
      .setMethod("GET")
      .setParam(PARAM_USER_LOGIN, "george.orwell")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_user_login_is_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, "jrr.tolkien")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() {
    db.components().insertPrivateProject();
    loginAsAdmin(db.getDefaultOrganization());

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
  public void adding_global_permission_fails_if_not_administrator_of_organization() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void adding_project_permission_fails_if_not_administrator_of_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .execute();
  }

  /**
   * User is project administrator but not system administrator
   */
  @Test
  public void adding_project_permission_is_allowed_to_project_administrators() {
    ComponentDto project = db.components().insertPrivateProject();

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectProjectPermissionsOfUser(user, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void organization_parameter_must_be_the_organization_of_the_project() {
    ComponentDto project = db.components().insertPrivateProject();
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Organization key is incorrect.");

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_ORGANIZATION, "an_org")
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void organization_parameter_and_project_is_working_when_it_s_the_organization_of_the_project() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    addUserAsMemberOfOrganization(org);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_KEY, project.getDbKey())
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_to_add_permission_when_user_is_not_member_of_given_organization() {
    // User is not member of given organization
    OrganizationDto otherOrganization = db.organizations().insert();
    addUserAsMemberOfOrganization(otherOrganization);
    OrganizationDto organization = db.organizations().insert(organizationDto -> organizationDto.setKey("Organization key"));
    loginAsAdmin(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User 'ray.bradbury' is not member of organization 'Organization key'");

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void no_effect_when_adding_USER_permission_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    addUserAsMemberOfOrganization(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, USER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(organization, project)).isEmpty();
  }

  @Test
  public void no_effect_when_adding_CODEVIEWER_permission_on_a_public_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    addUserAsMemberOfOrganization(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PERMISSION, CODEVIEWER)
      .execute();

    assertThat(db.users().selectAnyonePermissions(organization, project)).isEmpty();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    addUserAsMemberOfOrganization(organization);
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project key '%s' not found", branch.getDbKey()));

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PROJECT_KEY, branch.getDbKey())
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_using_branch_uuid() {
    OrganizationDto organization = db.organizations().insert();
    addUserAsMemberOfOrganization(organization);
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project id '%s' not found", branch.uuid()));

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .setParam(PARAM_USER_LOGIN, user.getLogin())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  private void addUserAsMemberOfOrganization(OrganizationDto organization) {
    db.organizations().addMember(organization, user);
  }

}
