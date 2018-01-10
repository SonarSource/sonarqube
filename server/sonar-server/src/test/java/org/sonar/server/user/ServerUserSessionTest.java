/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
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
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class ServerUserSessionTest {
  private static final String LOGIN = "marius";

  private static final String PUBLIC_PROJECT_UUID = "public_project";
  private static final String PRIVATE_PROJECT_UUID = "private_project";
  private static final String FILE_KEY = "com.foo:Bar:BarFile.xoo";
  private static final String FILE_UUID = "BCDE";
  private static final UserDto ROOT_USER_DTO = new UserDto() {
    {
      setRoot(true);
    }
  }.setLogin("root_user");
  private static final UserDto NON_ROOT_USER_DTO = new UserDto() {
    {
      setRoot(false);
    }
  }.setLogin("regular_user");

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private UserDto user;
  private GroupDto groupOfUser;
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OrganizationDto organization;
  private ComponentDto publicProject;
  private ComponentDto privateProject;

  @Before
  public void setUp() throws Exception {
    organization = db.organizations().insert();
    publicProject = db.components().insertPublicProject(organization, PUBLIC_PROJECT_UUID);
    privateProject = db.components().insertPrivateProject(organization, dto -> dto.setUuid(PRIVATE_PROJECT_UUID).setProjectUuid(PRIVATE_PROJECT_UUID).setPrivate(true));
    db.components().insertComponent(ComponentTesting.newFileDto(publicProject, null, FILE_UUID).setDbKey(FILE_KEY));
    user = db.users().insertUser(LOGIN);
    groupOfUser = db.users().insertGroup(organization);
  }

  @Test
  public void anonymous_is_not_logged_in_and_does_not_have_login() {
    UserSession session = newAnonymousSession();

    assertThat(session.getLogin()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void getGroups_is_empty_on_anonymous() {
    assertThat(newAnonymousSession().getGroups()).isEmpty();
  }

  @Test
  public void getGroups_is_empty_if_user_is_not_member_of_any_group() {
    assertThat(newUserSession(user).getGroups()).isEmpty();
  }

  @Test
  public void getGroups_returns_the_groups_of_logged_in_user() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    assertThat(newUserSession(user).getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId(), group2.getId());
  }

  @Test
  public void getGroups_keeps_groups_in_cache() {
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
    assertThat(newUserSession(ROOT_USER_DTO).isRoot()).isTrue();
    assertThat(newUserSession(NON_ROOT_USER_DTO).isRoot()).isFalse();
  }

  @Test
  public void checkIsRoot_throws_IPFE_if_flag_root_is_false_on_UserDto() {
    UserSession underTest = newUserSession(NON_ROOT_USER_DTO);

    expectInsufficientPrivilegesForbiddenException();

    underTest.checkIsRoot();
  }

  @Test
  public void checkIsRoot_does_not_fail_if_flag_root_is_true_on_UserDto() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.checkIsRoot()).isSameAs(underTest);
  }

  @Test
  public void hasComponentUuidPermission_returns_true_when_flag_root_is_true_on_UserDto_no_matter_if_user_has_project_permission_for_given_uuid() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.hasComponentUuidPermission(UserRole.USER, FILE_UUID)).isTrue();
    assertThat(underTest.hasComponentUuidPermission(UserRole.CODEVIEWER, FILE_UUID)).isTrue();
    assertThat(underTest.hasComponentUuidPermission(UserRole.ADMIN, FILE_UUID)).isTrue();
    assertThat(underTest.hasComponentUuidPermission("whatever", "who cares?")).isTrue();
  }

  @Test
  public void checkComponentUuidPermission_succeeds_if_user_has_permission_for_specified_uuid_in_db() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.checkComponentUuidPermission(UserRole.USER, FILE_UUID)).isSameAs(underTest);
    assertThat(underTest.checkComponentUuidPermission("whatever", "who cares?")).isSameAs(underTest);
  }

  @Test
  public void checkComponentUuidPermission_fails_with_FE_when_user_has_not_permission_for_specified_uuid_in_db() {
    addProjectPermissions(privateProject, UserRole.USER);
    UserSession session = newUserSession(user);

    expectInsufficientPrivilegesForbiddenException();

    session.checkComponentUuidPermission(UserRole.USER, "another-uuid");
  }

  @Test
  public void checkPermission_throws_ForbiddenException_when_user_doesnt_have_the_specified_permission_on_organization() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertUser(NON_ROOT_USER_DTO);

    expectInsufficientPrivilegesForbiddenException();

    newUserSession(NON_ROOT_USER_DTO).checkPermission(PROVISION_PROJECTS, org);
  }

  @Test
  public void checkPermission_succeeds_when_user_has_the_specified_permission_on_organization() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertUser(NON_ROOT_USER_DTO);
    db.users().insertPermissionOnUser(org, NON_ROOT_USER_DTO, PROVISIONING);

    newUserSession(NON_ROOT_USER_DTO).checkPermission(PROVISION_PROJECTS, org);
  }

  @Test
  public void checkPermission_succeeds_when_user_is_root() {
    OrganizationDto org = db.organizations().insert();

    newUserSession(ROOT_USER_DTO).checkPermission(PROVISION_PROJECTS, org);
  }

  @Test
  public void test_hasPermission_on_organization_for_logged_in_user() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
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
    ServerUserSession underTest = newAnonymousSession();

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_global_permissions() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnAnyone("p1", publicProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_group_permissions() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnGroup(db.users().insertGroup(organization), "p1", publicProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_permissions_USER_and_CODEVIEWER_on_public_projects_with_user_permissions() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p1", publicProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, publicProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_without_permissions() {
    ServerUserSession underTest = newUserSession(user);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_with_group_permissions() {
    ServerUserSession underTest = newUserSession(user);
    db.users().insertProjectPermissionOnGroup(db.users().insertGroup(organization), "p1", privateProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_authenticated_user_for_permissions_USER_and_CODEVIEWER_on_private_projects_with_user_permissions() {
    ServerUserSession underTest = newUserSession(user);
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p1", privateProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.USER, privateProject)).isFalse();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, UserRole.CODEVIEWER, privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_anonymous_user_for_inserted_permissions_on_group_AnyOne_on_public_projects() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnAnyone("p1", publicProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_group_on_public_projects() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnGroup(groupOfUser, "p1", publicProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_group_on_private_projects() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnGroup(groupOfUser, "p1", privateProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_user_on_public_projects() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnUser(user, "p1", publicProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", publicProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_false_for_anonymous_user_for_inserted_permissions_on_user_on_private_projects() {
    ServerUserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnUser(user, "p1", privateProject);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isFalse();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_returns_true_for_any_project_or_permission_for_root_user() {
    ServerUserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "does not matter", publicProject)).isTrue();
  }

  @Test
  public void hasComponentPermissionByDtoOrUuid_keeps_cache_of_permissions_of_logged_in_user() {
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
    UserSession underTest = newAnonymousSession();

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
  }

  @Test
  public void keepAuthorizedComponents_filters_components_with_granted_permissions_for_logged_in_user() {
    UserSession underTest = newUserSession(user);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, privateProject);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ISSUE_ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject))).containsExactly(privateProject);
  }

  @Test
  public void keepAuthorizedComponents_filters_components_with_granted_permissions_for_anonymous() {
    UserSession underTest = newAnonymousSession();
    db.users().insertProjectPermissionOnAnyone(UserRole.ISSUE_ADMIN, publicProject);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject))).isEmpty();
    assertThat(underTest.keepAuthorizedComponents(UserRole.ISSUE_ADMIN, Arrays.asList(privateProject, publicProject))).containsExactly(publicProject);
  }

  @Test
  public void keepAuthorizedComponents_returns_all_specified_components_if_root() {
    user = db.users().makeRoot(user);
    UserSession underTest = newUserSession(user);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, Arrays.asList(privateProject, publicProject)))
      .containsExactly(privateProject, publicProject);
  }

  @Test
  public void keepAuthorizedComponents_on_branches() {
    user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, privateProject);
    ComponentDto privateBranchProject = db.components().insertProjectBranch(privateProject);
    UserSession underTest = newUserSession(user);

    assertThat(underTest.keepAuthorizedComponents(UserRole.ADMIN, asList(privateProject, privateBranchProject)))
      .containsExactlyInAnyOrder(privateProject, privateBranchProject);
  }

  @Test
  public void isSystemAdministrator_returns_true_if_org_feature_is_enabled_and_user_is_root() {
    organizationFlags.setEnabled(true);
    user = db.users().makeRoot(user);
    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_not_root() {
    organizationFlags.setEnabled(true);
    user = db.users().makeNotRoot(user);
    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_administrator_of_default_organization() {
    organizationFlags.setEnabled(true);
    user = db.users().makeNotRoot(user);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN);
    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_true_if_org_feature_is_disabled_and_user_is_administrator_of_default_organization() {
    organizationFlags.setEnabled(false);
    user = db.users().makeNotRoot(user);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN);
    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_disabled_and_user_is_not_administrator_of_default_organization() {
    organizationFlags.setEnabled(true);
    user = db.users().makeNotRoot(user);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, PROVISIONING);
    UserSession session = newUserSession(user);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void keep_isSystemAdministrator_flag_in_cache() {
    organizationFlags.setEnabled(false);
    user = db.users().makeNotRoot(user);
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
    user = db.users().makeRoot(user);
    UserSession session = newUserSession(user);

    session.checkIsSystemAdministrator();
  }

  @Test
  public void checkIsSystemAdministrator_throws_ForbiddenException_if_not_system_administrator() {
    organizationFlags.setEnabled(true);
    user = db.users().makeNotRoot(user);
    UserSession session = newUserSession(user);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    session.checkIsSystemAdministrator();
  }

  @Test
  public void hasComponentPermission_on_branch_checks_permissions_of_its_project() {
    ComponentDto branch = db.components().insertProjectBranch(privateProject, b -> b.setKey("feature/foo"));
    ComponentDto fileInBranch = db.components().insertComponent(newChildComponent("fileUuid", branch, branch));

    // permissions are defined on the project, not on the branch
    db.users().insertProjectPermissionOnUser(user, "p1", privateProject);

    UserSession underTest = newUserSession(user);
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", privateProject)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", branch)).isTrue();
    assertThat(hasComponentPermissionByDtoOrUuid(underTest, "p1", fileInBranch)).isTrue();
  }

  private ServerUserSession newUserSession(@Nullable UserDto userDto) {
    return new ServerUserSession(dbClient, organizationFlags, defaultOrganizationProvider, userDto);
  }

  private ServerUserSession newAnonymousSession() {
    return newUserSession(null);
  }

  private void addProjectPermissions(ComponentDto component, String... permissions) {
    addPermissions(component, permissions);
  }

  private void addPermissions(@Nullable ComponentDto component, String... permissions) {
    for (String permission : permissions) {
      if (component == null) {
        db.users().insertPermissionOnUser(user, OrganizationPermission.fromKey(permission));
      } else {
        db.users().insertProjectPermissionOnUser(user, permission, component);
      }
    }
  }

  private void expectInsufficientPrivilegesForbiddenException() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
  }

}
