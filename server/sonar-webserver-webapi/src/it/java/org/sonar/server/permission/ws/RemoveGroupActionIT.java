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

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

@RunWith(DataProviderRunner.class)
public class RemoveGroupActionIT extends BasePermissionWsIT<RemoveGroupAction> {

  private GroupDto aGroup;
  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  @Before
  public void setUp() {
    aGroup = db.users().insertGroup("sonar-administrators");
  }

  @Override
  protected RemoveGroupAction buildWsAction() {
    return new RemoveGroupAction(db.getDbClient(), userSession, newPermissionUpdater(), newPermissionWsSupport(), wsParameters, permissionService, managedInstanceChecker);
  }

  @Test
  public void wsAction_shouldHaveDefinition() {
    Action wsDef = wsTester.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("10.0", "Parameter 'groupId' is removed. Use 'groupName' instead."),
      tuple("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'groupName' instead."));
  }

  @Test
  public void wsAction_shouldRemoveGlobalPermission() {
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.PROVISION_PROJECTS);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, GlobalPermission.PROVISION_PROJECTS.getKey())
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  @Test
  public void wsAction_shouldRemoveProjectPermission() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.ADMINISTER);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ADMIN, project);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ISSUE_ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(GlobalPermission.ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void wsAction_whenUsingViewUuid_shouldRemovePermission() {
    EntityDto portfolio = db.components().insertPrivatePortfolioDto();
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.ADMINISTER);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ADMIN, portfolio);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ISSUE_ADMIN, portfolio);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, portfolio.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(GlobalPermission.ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, portfolio)).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void wsAction_whenUsingProjectKey_shouldRemovePermission() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.ADMINISTER);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ADMIN, project);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ISSUE_ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, UserRole.ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, null)).containsOnly(GlobalPermission.ADMINISTER.getKey());
    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void wsAction_whenLastAdminPermission_shouldFail() {
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.ADMINISTER);
    db.users().insertPermissionOnGroup(aGroup, GlobalPermission.PROVISION_PROJECTS);
    loginAsAdmin();

    String administerPermission = GlobalPermission.ADMINISTER.getKey();
    assertThatThrownBy(() -> executeRequest(aGroup, administerPermission))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Last group with permission 'admin'. Permission cannot be removed.");
  }

  @Test
  public void wsAction_whenProjectNotFound_shouldFail() {
    loginAsAdmin();

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void wsAction_whenUsingProjectPermissionWithoutProject_shouldFail() {
    loginAsAdmin();

    assertThatThrownBy(() -> executeRequest(aGroup, UserRole.ISSUE_ADMIN))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Invalid global permission 'issueadmin'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");
  }

  @Test
  public void wsAction_whenComponentIsDirectory_shouldFail() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newDirectory(project, "A/B"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void wsAction_whenComponentIsFile_shouldFail() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "file-uuid"));

    failIfComponentIsNotAProjectOrView(file);
  }

  @Test
  public void wsAction_whenComponentIsSubview_shouldFail() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newSubPortfolio(project));

    failIfComponentIsNotAProjectOrView(file);
  }

  private void failIfComponentIsNotAProjectOrView(ComponentDto file) {
    loginAsAdmin();
    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, file.uuid())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void wsAction_whenGroupAndProjectAreManaged_shouldFailAndNotRemovePermissions() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.CODEVIEWER, project);

    doThrow(new IllegalStateException("Managed project and group")).when(managedInstanceChecker).throwIfGroupAndProjectAreManaged(any(), eq(aGroup.getUuid()), eq(project.getUuid()));

    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_KEY, project.getKey())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Managed project and group");

    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void wsAction_whenGroupNameIsMissing_shouldFail() {
    loginAsAdmin();

    TestRequest testRequest = newRequest().setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'groupName' parameter is missing");
  }

  @Test
  public void wsAction_whenPermissionNameAndIdMissing_shouldFail() {
    loginAsAdmin();

    TestRequest testRequest = newRequest().setParam(PARAM_GROUP_NAME, aGroup.getName());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'permission' parameter is missing");
  }

  @Test
  public void wsAction_whenProjectUuidAndProjectKeyAreProvided_shouldFail() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    loginAsAdmin();

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(PARAM_PROJECT_KEY, project.getKey());

    assertThatThrownBy(testRequest::execute)
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
  public void wsAction_whenRemovingGlobalPermissionAndNotSystemAdmin_shouldFail() {
    userSession.logIn();

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, GlobalPermission.PROVISION_PROJECTS.getKey());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void wsAction_whenRemovingProjectPermissionAndNotProjectAdmin_shouldFail() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PERMISSION, GlobalPermission.PROVISION_PROJECTS.getKey())
      .setParam(PARAM_PROJECT_KEY, project.getKey());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void wsAction_whenRemovingProjectPermissionAsProjectAdminButNotSystemAdmin_shouldRemovePermission() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.CODEVIEWER, project);
    db.users().insertEntityPermissionOnGroup(aGroup, UserRole.ISSUE_ADMIN, project);

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    newRequest()
      .setParam(PARAM_GROUP_NAME, aGroup.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();

    assertThat(db.users().selectGroupPermissions(aGroup, project)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void wsAction_whenRemovingAnyPermissionFromGroupAnyoneOnPrivateProject_shouldHaveNoEffect() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> unsafeInsertProjectPermissionOnAnyone(perm, project));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        newRequest()
          .setParam(PARAM_GROUP_NAME, "anyone")
          .setParam(PARAM_PROJECT_ID, project.getUuid())
          .setParam(PARAM_PERMISSION, permission)
          .execute();

        assertThat(db.users().selectAnyonePermissions(project.getUuid())).contains(permission);
      });
  }

  @Test
  public void wsAction_whenRemovingBrowsePermissionFromGroupAnyoneOnPublicProject_shouldFail() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.USER);

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void wsAction_whenRemovingCodeviewerPermissionFromGroupAnyoneOnPublicProject_shouldFail() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, "anyone")
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.CODEVIEWER);

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void wsAction_whenRemovingBrowsePermissionFromGroupOnPublicProject_shouldFail() {
    GroupDto group = db.users().insertGroup();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.USER);

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void wsAction_whenRemovingCodeviewerPermissionFromGroupOnPublicProject() {
    GroupDto group = db.users().insertGroup();
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    TestRequest testRequest = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_PERMISSION, UserRole.CODEVIEWER);

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void wsAction_whenUsingBranchUuid_shouldFail() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    TestRequest testRequest = newRequest()
      .setParam(PARAM_PROJECT_ID, branch.uuid())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .setParam(PARAM_PERMISSION, GlobalPermission.ADMINISTER.getKey());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void wsAction_whenRemovingLastOwnBrowsePermissionForPrivateProject_shouldFail() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();
    GroupDto projectAdminGroup = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(projectAdminGroup, UserRole.USER, project);
    db.users().insertEntityPermissionOnGroup(projectAdminGroup, UserRole.ADMIN, project);

    userSession.logIn(user).setGroups(projectAdminGroup).addProjectPermission(UserRole.USER, project).addProjectPermission(UserRole.ADMIN, project);

    TestRequest testRequest = newRequest()
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_GROUP_NAME, projectAdminGroup.getName())
      .setParam(PARAM_PERMISSION, UserRole.USER);

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission 'Browse' cannot be removed from a private project for a project administrator.");
  }

  @Test
  public void wsAction_whenRemovingOwnBrowsePermissionAndHavePermissionFromOtherGroup_shouldRemovePermission() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();
    GroupDto projectAdminGroup = db.users().insertGroup();
    GroupDto otherProjectAdminGroup = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(projectAdminGroup, UserRole.USER, project);
    db.users().insertEntityPermissionOnGroup(projectAdminGroup, UserRole.ADMIN, project);
    db.users().insertEntityPermissionOnGroup(otherProjectAdminGroup, UserRole.USER, project);
    db.users().insertEntityPermissionOnGroup(otherProjectAdminGroup, UserRole.ADMIN, project);
    userSession.logIn(user).setGroups(projectAdminGroup, otherProjectAdminGroup).addProjectPermission(UserRole.USER, project).addProjectPermission(UserRole.ADMIN, project);

    newRequest()
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .setParam(PARAM_GROUP_NAME, projectAdminGroup.getName())
      .setParam(PARAM_PERMISSION, UserRole.USER)
      .execute();

    assertThat(db.users().selectGroupPermissions(projectAdminGroup, project)).containsOnly(UserRole.ADMIN);
    assertThat(db.users().selectGroupPermissions(otherProjectAdminGroup, project)).containsExactlyInAnyOrder(UserRole.USER, UserRole.ADMIN);
  }

  private void unsafeInsertProjectPermissionOnAnyone(String perm, ProjectDto project) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(perm)
      .setEntityUuid(project.getUuid())
      .setEntityName(project.getName());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, project, null);
    db.commit();
  }
}
