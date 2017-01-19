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
package org.sonar.server.user;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.user.ServerUserSession.createForAnonymous;
import static org.sonar.server.user.ServerUserSession.createForUser;

public class ServerUserSessionTest {
  private static final String LOGIN = "marius";

  private static final String PROJECT_UUID = "ABCD";
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
  private UserDto userDto = newUserDto().setLogin(LOGIN);
  private OrganizationDto organization;
  private ComponentDto project;

  @Before
  public void setUp() throws Exception {
    organization = db.organizations().insert();
    project = db.components().insertProject(organization, PROJECT_UUID);
    db.components().insertComponent(ComponentTesting.newFileDto(project, null, FILE_UUID).setKey(FILE_KEY));
    db.users().insertUser(userDto);
  }

  @Test
  public void isRoot_is_false_is_flag_root_is_false_on_UserDto() {
    assertThat(newUserSession(ROOT_USER_DTO).isRoot()).isTrue();
    assertThat(newUserSession(NON_ROOT_USER_DTO).isRoot()).isFalse();
  }

  @Test
  public void checkIsRoot_fails_with_ForbiddenException_when_flag_is_false_on_UserDto() {
    expectInsufficientPrivilegesForbiddenException();

    newUserSession(NON_ROOT_USER_DTO).checkIsRoot();
  }

  @Test
  public void checkIsRoot_does_not_fails_when_flag_is_true_on_UserDto() {
    ServerUserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.checkIsRoot()).isSameAs(underTest);
  }

  @Test
  public void hasPermission_permission() {
    addGlobalPermissions("admin", "profileadmin");
    UserSession session = newUserSession(userDto);

    assertThat(session.hasPermission(QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(session.hasPermission(SYSTEM_ADMIN)).isTrue();
    assertThat(session.hasPermission(QUALITY_GATE_ADMIN)).isFalse();
  }

  @Test
  public void hasPermission_returns_true_when_flag_is_true_on_UserDto_no_matter_actual_global_permissions() {
    ServerUserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.hasPermission(QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(underTest.hasPermission(SYSTEM_ADMIN)).isTrue();
    assertThat(underTest.hasPermission("whatever!")).isTrue();
  }

  @Test
  public void checkPermission_succeeds_if_user_has_global_permission_in_db() {
    addGlobalPermissions("admin", "profileadmin");
    UserSession session = newUserSession(userDto);

    session.checkPermission(QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void checkPermission_fails_with_FE_if_user_has_not_global_permission_in_db() {
    addGlobalPermissions("admin", "profileadmin");
    UserSession session = newUserSession(userDto);

    expectInsufficientPrivilegesForbiddenException();

    session.checkPermission(QUALITY_GATE_ADMIN);
  }

  @Test
  public void checkPermission_succeeds_when_flag_is_true_on_UserDto_no_matter_actual_global_permissions() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.checkPermission(QUALITY_PROFILE_ADMIN)).isSameAs(underTest);
    assertThat(underTest.checkPermission(SYSTEM_ADMIN)).isSameAs(underTest);
    assertThat(underTest.checkPermission("whatever!")).isSameAs(underTest);
  }

  @Test
  public void has_component_permission() {
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    assertThat(session.hasComponentPermission(UserRole.USER, FILE_KEY)).isTrue();
    assertThat(session.hasComponentPermission(UserRole.CODEVIEWER, FILE_KEY)).isFalse();
    assertThat(session.hasComponentPermission(UserRole.ADMIN, FILE_KEY)).isFalse();
  }

  @Test
  public void hasComponentUuidPermission_returns_true_if_user_has_project_permission_for_given_uuid_in_db() {
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    assertThat(session.hasComponentUuidPermission(UserRole.USER, FILE_UUID)).isTrue();
    assertThat(session.hasComponentUuidPermission(UserRole.CODEVIEWER, FILE_UUID)).isFalse();
    assertThat(session.hasComponentUuidPermission(UserRole.ADMIN, FILE_UUID)).isFalse();
  }

  @Test
  public void hasComponentUuidPermission_returns_true_when_flag_is_true_on_UserDto_no_matter_if_user_has_project_permission_for_given_uuid() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.hasComponentUuidPermission(UserRole.USER, FILE_UUID)).isTrue();
    assertThat(underTest.hasComponentUuidPermission(UserRole.CODEVIEWER, FILE_UUID)).isTrue();
    assertThat(underTest.hasComponentUuidPermission(UserRole.ADMIN, FILE_UUID)).isTrue();
    assertThat(underTest.hasComponentUuidPermission("whatever", "who cares?")).isTrue();
  }

  @Test
  public void hasComponentPermission_returns_true_if_user_has_global_permission_in_db() {
    addGlobalPermissions(UserRole.USER);
    UserSession session = newUserSession(userDto);

    assertThat(session.hasComponentPermission(UserRole.USER, FILE_KEY)).isTrue();
    assertThat(session.hasComponentPermission(UserRole.CODEVIEWER, FILE_KEY)).isFalse();
    assertThat(session.hasComponentPermission(UserRole.ADMIN, FILE_KEY)).isFalse();
  }

  @Test
  public void has_component_uuid_permission_with_only_global_permission() {
    addGlobalPermissions(UserRole.USER);
    UserSession session = newUserSession(userDto);

    assertThat(session.hasComponentUuidPermission(UserRole.USER, FILE_UUID)).isTrue();
    assertThat(session.hasComponentUuidPermission(UserRole.CODEVIEWER, FILE_UUID)).isFalse();
    assertThat(session.hasComponentUuidPermission(UserRole.ADMIN, FILE_UUID)).isFalse();
  }

  @Test
  public void checkComponentPermission_succeeds_if_user_has_permission_for_specified_key_in_db() {
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    session.checkComponentPermission(UserRole.USER, FILE_KEY);
  }

  @Test
  public void checkComponentPermission_succeeds_if_user_has_global_permission_in_db() {
    addGlobalPermissions(UserRole.USER);
    UserSession session = newUserSession(userDto);

    session.checkComponentPermission(UserRole.USER, FILE_KEY);
  }

  @Test
  public void checkComponentPermission_succeeds_when_flag_is_true_on_UserDto_no_matter_if_user_has_permission_for_specified_key_in_db() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.checkComponentPermission(UserRole.USER, FILE_KEY)).isSameAs(underTest);
    assertThat(underTest.checkComponentPermission(UserRole.CODEVIEWER, FILE_KEY)).isSameAs(underTest);
    assertThat(underTest.checkComponentPermission("whatever", "who cares?")).isSameAs(underTest);
  }

  @Test
  public void checkComponentPermission_throws_FE_when_user_has_not_permission_for_specified_key_in_db() {
    ComponentDto project2 = db.components().insertComponent(ComponentTesting.newProjectDto(db.organizations().insert()));
    ComponentDto file2 = db.components().insertComponent(ComponentTesting.newFileDto(project2, null));
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    expectInsufficientPrivilegesForbiddenException();

    session.checkComponentPermission(UserRole.USER, file2.getKey());
  }

  @Test
  public void checkComponentPermission_throws_FE_when_project_does_not_exist_in_db() {
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    expectInsufficientPrivilegesForbiddenException();

    session.checkComponentPermission(UserRole.USER, "another");
  }

  @Test
  public void checkComponentPermission_fails_with_FE_when_project_of_specified_uuid_can_not_be_found() {
    ComponentDto project2 = db.components().insertComponent(ComponentTesting.newProjectDto(db.organizations().insert()));
    ComponentDto file2 = db.components().insertComponent(ComponentTesting.newFileDto(project2, null)
      // Simulate file is linked to an invalid project
      .setProjectUuid("INVALID"));
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    expectInsufficientPrivilegesForbiddenException();

    session.checkComponentPermission(UserRole.USER, file2.getKey());
  }

  @Test
  public void checkComponentUuidPermission_succeeds_if_user_has_permission_for_specified_uuid_in_db() {
    UserSession underTest = newUserSession(ROOT_USER_DTO);

    assertThat(underTest.checkComponentUuidPermission(UserRole.USER, FILE_UUID)).isSameAs(underTest);
    assertThat(underTest.checkComponentUuidPermission("whatever", "who cares?")).isSameAs(underTest);
  }

  @Test
  public void checkComponentUuidPermission_fails_with_FE_when_user_has_not_permission_for_specified_uuid_in_db() {
    addProjectPermissions(project, UserRole.USER);
    UserSession session = newUserSession(userDto);

    expectInsufficientPrivilegesForbiddenException();

    session.checkComponentUuidPermission(UserRole.USER, "another-uuid");
  }

  @Test
  public void deprecated_has_global_permission() throws Exception {
    addGlobalPermissions("profileadmin", "admin");
    UserSession session = newUserSession(userDto);

    assertThat(session.hasGlobalPermission(QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(session.hasGlobalPermission(SYSTEM_ADMIN)).isTrue();
    assertThat(session.hasGlobalPermission(QUALITY_GATE_ADMIN)).isFalse();
  }

  @Test
  public void deprecated_check_global_permission() throws Exception {
    addGlobalPermissions("profileadmin", "admin");
    UserSession session = newUserSession(userDto);

    session.checkGlobalPermission(QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void fail_if_user_dto_is_null() throws Exception {
    expectedException.expect(NullPointerException.class);
    newUserSession(null);
  }

  @Test
  public void anonymous_user() throws Exception {
    UserSession session = newAnonymousSession();

    assertThat(session.getLogin()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void has_global_permission_for_anonymous() throws Exception {
    addAnyonePermissions(db.getDefaultOrganization(), null, "profileadmin", "admin");
    UserSession session = newAnonymousSession();

    assertThat(session.getLogin()).isNull();
    assertThat(session.isLoggedIn()).isFalse();

    assertThat(session.hasPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(session.hasPermission(GlobalPermissions.SYSTEM_ADMIN)).isTrue();
    assertThat(session.hasPermission(GlobalPermissions.QUALITY_GATE_ADMIN)).isFalse();
  }

  @Test
  public void has_project_permission_for_anonymous() throws Exception {
    addAnyonePermissions(organization, project, UserRole.USER);
    UserSession session = newAnonymousSession();

    assertThat(session.hasComponentPermission(UserRole.USER, FILE_KEY)).isTrue();
    assertThat(session.hasComponentPermission(UserRole.CODEVIEWER, FILE_KEY)).isFalse();
    assertThat(session.hasComponentPermission(UserRole.ADMIN, FILE_KEY)).isFalse();
  }

  @Test
  public void checkOrganizationPermission_fails_with_ForbiddenException_when_user_has_no_permissions_on_organization() {
    expectInsufficientPrivilegesForbiddenException();

    newUserSession(NON_ROOT_USER_DTO).checkOrganizationPermission("org-uuid", "perm1");
  }

  @Test
  public void hasOrganizationPermission_for_logged_in_user() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertProject();
    db.users().insertPermissionOnUser(org, userDto, GlobalPermissions.PROVISIONING);
    db.users().insertProjectPermissionOnUser(userDto, UserRole.ADMIN, project);

    UserSession session = newUserSession(userDto);
    assertThat(session.hasOrganizationPermission(org.getUuid(), GlobalPermissions.PROVISIONING)).isTrue();
    assertThat(session.hasOrganizationPermission(org.getUuid(), GlobalPermissions.SYSTEM_ADMIN)).isFalse();
    assertThat(session.hasOrganizationPermission("another-org", GlobalPermissions.PROVISIONING)).isFalse();
  }

  @Test
  public void hasOrganizationPermission_for_anonymous_user() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertPermissionOnAnyone(org, GlobalPermissions.PROVISIONING);

    UserSession session = newAnonymousSession();
    assertThat(session.hasOrganizationPermission(org.getUuid(), GlobalPermissions.PROVISIONING)).isTrue();
    assertThat(session.hasOrganizationPermission(org.getUuid(), GlobalPermissions.SYSTEM_ADMIN)).isFalse();
    assertThat(session.hasOrganizationPermission("another-org", GlobalPermissions.PROVISIONING)).isFalse();
  }

  private ServerUserSession newUserSession(UserDto userDto) {
    return createForUser(dbClient, userDto);
  }

  private ServerUserSession newAnonymousSession() {
    return createForAnonymous(dbClient);
  }

  private void addGlobalPermissions(String... permissions) {
    addPermissions(null, permissions);
  }

  private void addProjectPermissions(ComponentDto component, String... permissions) {
    addPermissions(component, permissions);
  }

  private void addPermissions(@Nullable ComponentDto component, String... permissions) {
    for (String permission : permissions) {
      if (component == null) {
        db.users().insertPermissionOnUser(userDto, permission);
      } else {
        db.users().insertProjectPermissionOnUser(userDto, permission, component);
      }
    }
  }

  private void addAnyonePermissions(OrganizationDto organizationDto, @Nullable ComponentDto component, String... permissions) {
    for (String permission : permissions) {
      if (component == null) {
        db.users().insertPermissionOnAnyone(organizationDto, permission);
      } else {
        db.users().insertProjectPermissionOnAnyone(organizationDto, permission, component);
      }
    }
  }

  private void expectInsufficientPrivilegesForbiddenException() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
  }

}
