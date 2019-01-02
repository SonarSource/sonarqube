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
package org.sonar.server.user;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newChildComponent;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class ServerUserSessionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private DbClient dbClient = db.getDbClient();
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  @Test
  public void anonymous_is_not_logged_in_and_does_not_have_login() {
    UserSession session = newAnonymousSession();

    assertThat(session.getLogin()).isNull();
    assertThat(session.getUuid()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void getGroups_is_empty_on_anonymous() {
    assertThat(newAnonymousSession().getGroups()).isEmpty();
  }

  @Test
  public void getGroups_is_empty_if_user_is_not_member_of_any_group() {
    UserDto user = db.users().insertUser();
    assertThat(newUserSession(user).getGroups()).isEmpty();
  }

  @Test
  public void getGroups_returns_the_groups_of_logged_in_user() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    assertThat(newUserSession(user).getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId(), group2.getId());
  }

  @Test
  public void getGroups_keeps_groups_in_cache() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user);

    ServerUserSession session = newUserSession(user);
    assertThat(session.getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId());

    // membership updated but not cache
    db.users().insertMember(group2, user);
    assertThat(session.getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId());
  }

  @Test
  public void isRoot_is_false_is_flag_root_is_false_on_UserDto() {
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);
    assertThat(newUserSession(root).isRoot()).isTrue();

    UserDto notRoot = db.users().insertUser();
    assertThat(newUserSession(notRoot).isRoot()).isFalse();
  }

  @Test
  public void checkIsRoot_throws_IPFE_if_flag_root_is_false_on_UserDto() {
    UserDto user = db.users().insertUser();
    UserSession underTest = newUserSession(user);

    expectInsufficientPrivilegesForbiddenException();

    underTest.checkIsRoot();
  }

  @Test
  public void checkIsRoot_does_not_fail_if_flag_root_is_true_on_UserDto() {
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);

    UserSession underTest = newUserSession(root);

    assertThat(underTest.checkIsRoot()).isSameAs(underTest);
  }

  @Test
  public void hasComponentUuidPermission_returns_true_when_flag_root_is_true_on_UserDto_no_matter_if_user_has_project_permission_for_given_uuid() {
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    UserSession underTest = newUserSession(root);

    assertThat(underTest.hasComponentUuidPermission(UserRole.USER, file.uuid())).isTrue();
    assertThat(underTest.hasComponentUuidPermission(UserRole.CODEVIEWER, file.uuid())).isTrue();
    assertThat(underTest.hasComponentUuidPermission(UserRole.ADMIN, file.uuid())).isTrue();
    assertThat(underTest.hasComponentUuidPermission("whatever", "who cares?")).isTrue();
  }

  @Test
  public void checkComponentUuidPermission_succeeds_if_user_has_permission_for_specified_uuid_in_db() {
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    UserSession underTest = newUserSession(root);

    assertThat(underTest.checkComponentUuidPermission(UserRole.USER, file.uuid())).isSameAs(underTest);
    assertThat(underTest.checkComponentUuidPermission("whatever", "who cares?")).isSameAs(underTest);
  }

  @Test
  public void checkComponentUuidPermission_fails_with_FE_when_user_has_not_permission_for_specified_uuid_in_db() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project);
    UserSession session = newUserSession(user);

    expectInsufficientPrivilegesForbiddenException();

    session.checkComponentUuidPermission(UserRole.USER, "another-uuid");
  }

  @Test
  public void checkPermission_throws_ForbiddenException_when_user_doesnt_have_the_specified_permission_on_organization() {
    OrganizationDto org = db.organizations().insert();
    UserDto user = db.users().insertUser();

    expectInsufficientPrivilegesForbiddenException();

    newUserSession(user).checkPermission(PROVISION_PROJECTS, org);
  }

  @Test
  public void checkPermission_succeeds_when_user_has_the_specified_permission_on_organization() {
    OrganizationDto org = db.organizations().insert();
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);
    db.users().insertPermissionOnUser(org, root, PROVISIONING);

    newUserSession(root).checkPermission(PROVISION_PROJECTS, org);
  }

  @Test
  public void checkPermission_succeeds_when_user_is_root() {
    OrganizationDto org = db.organizations().insert();
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);

    newUserSession(root).checkPermission(PROVISION_PROJECTS, org);
  }

  @Test
  public void test_hasPermission_on_organization_for_logged_in_user() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(org, user, PROVISION_PROJECTS);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, project);

    UserSession session = newUserSession(user);
    assertThat(session.hasPermission(PROVISION_PROJECTS, org.getUuid())).isTrue();
    assertThat(session.hasPermission(ADMINISTER, org.getUuid())).isFalse();
    assertThat(session.hasPermission(PROVISION_PROJECTS, "another-org")).isFalse();
  }

  @Test
  public void test_hasPermission_on_organization_for_anonymous_user() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertPermissionOnAnyone(org, PROVISION_PROJECTS);

    UserSession session = newAnonymousSession();
    assertThat(session.hasPermission(PROVISION_PROJECTS, org.getUuid())).isTrue();
    assertThat(session.hasPermission(ADMINISTER, org.getUuid())).isFalse();
    assertThat(session.hasPermission(PROVISION_PROJECTS, "another-org")).isFalse();
  }

  @Test
  public void hasPermission_on_organization_keeps_cache_of_permissions_of_logged_in_user() {
    OrganizationDto org = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(org, user, PROVISIONING);

    UserSession session = newUserSession(user);

    // feed the cache
    assertThat(session.hasPermission(PROVISION_PROJECTS, org.getUuid())).isTrue();

    // change permissions without updating the cache
    db.users().deletePermissionFromUser(org, user, PROVISION_PROJECTS);
    db.users().insertPermissionOnUser(org, user, SCAN);
    assertThat(session.hasPermission(PROVISION_PROJECTS, org.getUuid())).isTrue();
    assertThat(session.hasPermission(ADMINISTER, org.getUuid())).isFalse();
    assertThat(session.hasPermission(SCAN, org.getUuid())).isFalse();
  }

  @Test
  public void hasPermission_on_organization_keeps_cache_of_permissions_of_anonymous_user() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertPermissionOnAnyone(org, PROVISION_PROJECTS);

    UserSession session = newAnonymousSession();

    // feed the cache
    assertThat(session.hasPermission(PROVISION_PROJECTS, org.getUuid())).isTrue();

    // change permissions without updating the cache
    db.users().insertPermissionOnAnyone(org, SCAN);
    assertThat(session.hasPermission(PROVISION_PROJECTS, org.getUuid())).isTrue();
    assertThat(session.hasPermission(SCAN, org.getUuid())).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_without_permissions() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_global_permissions() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnAnyone("p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_group_permissions() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnGroup(db.users().insertGroup(organization), "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_user_permissions() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_without_permissions() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();

    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_with_group_permissions() {
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    db.users().insertProjectPermissionOnGroup(db.users().insertGroup(organization), "p1", privateProject);

    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_with_user_permissions() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p1", privateProject);

    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_inserted_permissions_on_group_AnyOne_on_public_projects() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnAnyone("p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_group_on_public_projects() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    GroupDto group = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group, "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_group_on_private_projects() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    GroupDto group = db.users().insertGroup(organization);
    db.users().insertProjectPermissionOnGroup(group, "p1", privateProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_user_on_public_projects() {
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnUser(user, "p1", publicProject);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_user_on_private_projects() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, "p1", project);

    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", project)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_any_project_or_permission_for_root_user() {
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);

    ServerUserSession underTest = newUserSession(root);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "does not matter", publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_keeps_cache_of_permissions_of_logged_in_user() {
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, publicProject);

    UserSession underTest = newUserSession(user);

    // feed the cache
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.ADMIN, publicProject)).isTrue();

    // change permissions without updating the cache
    db.users().deletePermissionFromUser(publicProject, user, UserRole.ADMIN);
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, publicProject);
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.ADMIN, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.ISSUE_ADMIN, publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_keeps_cache_of_permissions_of_anonymous_user() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    db.users().insertProjectPermissionOnAnyone(UserRole.ADMIN, publicProject);

    UserSession underTest = newAnonymousSession();

    // feed the cache
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.ADMIN, publicProject)).isTrue();

    // change permissions without updating the cache
    db.users().deleteProjectPermissionFromAnyone(publicProject, UserRole.ADMIN);
    db.users().insertProjectPermissionOnAnyone(UserRole.ISSUE_ADMIN, publicProject);
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.ADMIN, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.ISSUE_ADMIN, publicProject)).isFalse();
  }

  private boolean hasComponentPermissionByDtoOrUuid(UserSession underTest, String permission, ComponentDto component) {
    boolean b1 = underTest.hasComponentPermission(permission, component);
    boolean b2 = underTest.hasComponentUuidPermission(permission, component.uuid());
    checkState(b1 == b2, "Different behaviors");
    return b1;
  }

  @Test
  public void keepAuthorizedComponents_returns_empty_list_if_no_permissions_are_granted() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    ComponentDto privateProject = db.components().insertPrivateProject(organization);

    UserSession underTest = newAnonymousSession();

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
  }

  @Test
  public void keepAuthorizedComponents_filters_components_with_granted_permissions_for_logged_in_user() {
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, privateProject);

    UserSession underTest = newUserSession(user);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ISSUE_ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject))).containsExactly(privateProject);
  }

  @Test
  public void keepAuthorizedComponents_filters_components_with_granted_permissions_for_anonymous() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    db.users().insertProjectPermissionOnAnyone(UserRole.ISSUE_ADMIN, publicProject);

    UserSession underTest = newAnonymousSession();

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(UserRole.ISSUE_ADMIN, Arrays.asList(privateProject, publicProject))).containsExactly(publicProject);
  }

  @Test
  public void keepAuthorizedComponents_returns_all_specified_components_if_root() {
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);
    OrganizationDto organization = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    ComponentDto privateProject = db.components().insertPrivateProject(organization);

    UserSession underTest = newUserSession(root);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject)))
      .containsExactly(privateProject, publicProject);
  }

  @Test
  public void keepAuthorizedComponents_on_branches() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, privateProject);
    ComponentDto privateBranchProject = db.components().insertProjectBranch(privateProject);

    UserSession underTest = newUserSession(user);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, asList(privateProject, privateBranchProject)))
      .containsExactlyInAnyOrder(privateProject, privateBranchProject);
  }

  @Test
  public void isSystemAdministrator_returns_true_if_org_feature_is_enabled_and_user_is_root() {
    organizationFlags.setEnabled(true);
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);

    UserSession session = newUserSession(root);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_not_root() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser();

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_administrator_of_default_organization() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN);

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_true_if_org_feature_is_disabled_and_user_is_administrator_of_default_organization() {
    organizationFlags.setEnabled(false);
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN);

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_disabled_and_user_is_not_administrator_of_default_organization() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, PROVISIONING);

    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void keep_isSystemAdministrator_flag_in_cache() {
    organizationFlags.setEnabled(false);
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN);

    UserSession session = newUserSession(user);

    session.checkIsSystemAdministrator();

    db.getDbClient().userDao().deactivateUser(db.getSession(), user);
    db.commit();

    // should fail but succeeds because flag is kept in cache
    session.checkIsSystemAdministrator();
  }

  @Test
  public void checkIsSystemAdministrator_succeeds_if_system_administrator() {
    organizationFlags.setEnabled(true);
    UserDto root = db.users().insertUser();
    root = db.users().makeRoot(root);

    UserSession session = newUserSession(root);

    session.checkIsSystemAdministrator();
  }

  @Test
  public void checkIsSystemAdministrator_throws_ForbiddenException_if_not_system_administrator() {
    organizationFlags.setEnabled(true);
    UserDto user = db.users().insertUser();

    UserSession session = newUserSession(user);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    session.checkIsSystemAdministrator();
  }

  @Test
  public void hasComponentPermission_on_branch_checks_permissions_of_its_project() {
    UserDto user = db.users().insertUser();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(privateProject, b -> b.setKey("feature/foo"));
    ComponentDto fileInBranch = db.components().insertComponent(newChildComponent("fileUuid", branch, branch));

    // permissions are defined on the project, not on the branch
    db.users().insertProjectPermissionOnUser(user, "p1", privateProject);

    UserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", branch)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", fileInBranch)).isTrue();
  }

  @Test
  public void hasMembership() {
    OrganizationDto organization = db.organizations().insert();
    UserDto notMember = db.users().insertUser();
    UserDto member = db.users().insertUser();
    db.organizations().addMember(organization, member);
    UserDto root = db.users().makeRoot(db.users().insertUser());

    assertThat(newUserSession(member).hasMembership(organization)).isTrue();
    assertThat(newUserSession(notMember).hasMembership(organization)).isFalse();
    assertThat(newUserSession(root).hasMembership(organization)).isTrue();
  }

  @Test
  public void hasMembership_keeps_membership_in_cache() {
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);

    ServerUserSession session = newUserSession(user);
    assertThat(session.hasMembership(organization)).isTrue();

    // membership updated but not cache
    db.getDbClient().organizationMemberDao().delete(db.getSession(), organization.getUuid(), user.getId());
    db.commit();
    assertThat(session.hasMembership(organization)).isTrue();
  }

  @Test
  public void checkMembership_throws_ForbiddenException_when_user_is_not_member_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    UserDto notMember = db.users().insertUser();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(String.format("You're not member of organization '%s'", organization.getKey()));

    newUserSession(notMember).checkMembership(organization);
  }

  @Test
  public void checkMembership_succeeds_when_user_is_member_of_organization() {
    OrganizationDto organization = db.organizations().insert();
    UserDto member = db.users().insertUser();
    db.organizations().addMember(organization, member);

    newUserSession(member).checkMembership(organization);
  }

  @Test
  public void checkMembership_succeeds_when_user_is_not_member_of_organization_but_root() {
    OrganizationDto organization = db.organizations().insert();
    UserDto root = db.users().makeRoot(db.users().insertUser());

    newUserSession(root).checkMembership(organization);
  }

  private ServerUserSession newUserSession(@Nullable UserDto userDto) {
    return new ServerUserSession(dbClient, organizationFlags, defaultOrganizationProvider, userDto);
  }

  private ServerUserSession newAnonymousSession() {
    return newUserSession(null);
  }

  private void expectInsufficientPrivilegesForbiddenException() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
  }

}
