/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.user.UserTesting.newUserDto;

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
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
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
  public void getGroups_is_empty_on_anonymous() {
    assertThat(newAnonymousSession().getGroups()).isEmpty();
  }

  @Test
  public void getGroups_is_empty_if_user_is_not_member_of_any_group() {
    assertThat(newUserSession(userDto).getGroups()).isEmpty();
  }

  @Test
  public void getGroups_returns_the_groups_of_logged_in_user() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, userDto);
    db.users().insertMember(group2, userDto);

    assertThat(newUserSession(userDto).getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId(), group2.getId());
  }

  @Test
  public void getGroups_keeps_groups_in_cache() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMember(group1, userDto);

    ServerUserSession session = newUserSession(userDto);
    assertThat(session.getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId());

    // membership updated but not cache
    db.users().insertMember(group2, userDto);
    assertThat(session.getGroups()).extracting(GroupDto::getId).containsOnly(group1.getId());
  }

  @Test
  public void isRoot_is_false_is_flag_root_is_false_on_UserDto() {
    assertThat(newUserSession(ROOT_USER_DTO).isRoot()).isTrue();
    assertThat(newUserSession(NON_ROOT_USER_DTO).isRoot()).isFalse();
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
  public void anonymous_user() throws Exception {
    UserSession session = newAnonymousSession();

    assertThat(session.getLogin()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void checkOrganizationPermission_throws_ForbiddenException_when_user_doesnt_have_the_specified_permission_on_organization() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertUser(NON_ROOT_USER_DTO);

    expectInsufficientPrivilegesForbiddenException();

    newUserSession(NON_ROOT_USER_DTO).checkOrganizationPermission(org.getUuid(), PROVISIONING);
  }

  @Test
  public void checkOrganizationPermission_succeeds_when_user_has_the_specified_permission_on_organization() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertUser(NON_ROOT_USER_DTO);
    db.users().insertPermissionOnUser(org, NON_ROOT_USER_DTO, PROVISIONING);

    newUserSession(NON_ROOT_USER_DTO).checkOrganizationPermission(org.getUuid(), PROVISIONING);
  }

  @Test
  public void checkOrganizationPermission_succeeds_when_user_is_root() {
    OrganizationDto org = db.organizations().insert();

    newUserSession(ROOT_USER_DTO).checkOrganizationPermission(org.getUuid(), PROVISIONING);
  }

  @Test
  public void test_hasOrganizationPermission_for_logged_in_user() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertProject(org);
    db.users().insertPermissionOnUser(org, userDto, PROVISIONING);
    db.users().insertProjectPermissionOnUser(userDto, UserRole.ADMIN, project);

    UserSession session = newUserSession(userDto);
    assertThat(session.hasOrganizationPermission(org.getUuid(), PROVISIONING)).isTrue();
    assertThat(session.hasOrganizationPermission(org.getUuid(), SYSTEM_ADMIN)).isFalse();
    assertThat(session.hasOrganizationPermission("another-org", PROVISIONING)).isFalse();
  }

  @Test
  public void test_hasOrganizationPermission_for_anonymous_user() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertPermissionOnAnyone(org, PROVISIONING);

    UserSession session = newAnonymousSession();
    assertThat(session.hasOrganizationPermission(org.getUuid(), PROVISIONING)).isTrue();
    assertThat(session.hasOrganizationPermission(org.getUuid(), SYSTEM_ADMIN)).isFalse();
    assertThat(session.hasOrganizationPermission("another-org", PROVISIONING)).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_true_if_org_feature_is_enabled_and_user_is_root() {
    organizationFlags.setEnabled(true);
    userDto = db.users().makeRoot(userDto);
    UserSession session = newUserSession(userDto);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_not_root() {
    organizationFlags.setEnabled(true);
    userDto = db.users().makeNotRoot(userDto);
    UserSession session = newUserSession(userDto);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_enabled_and_user_is_administrator_of_default_organization() {
    organizationFlags.setEnabled(true);
    userDto = db.users().makeNotRoot(userDto);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), userDto, SYSTEM_ADMIN);
    UserSession session = newUserSession(userDto);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isSystemAdministrator_returns_true_if_org_feature_is_disabled_and_user_is_administrator_of_default_organization() {
    organizationFlags.setEnabled(false);
    userDto = db.users().makeNotRoot(userDto);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), userDto, SYSTEM_ADMIN);
    UserSession session = newUserSession(userDto);

    assertThat(session.isSystemAdministrator()).isTrue();
  }

  @Test
  public void isSystemAdministrator_returns_false_if_org_feature_is_disabled_and_user_is_not_administrator_of_default_organization() {
    organizationFlags.setEnabled(true);
    userDto = db.users().makeNotRoot(userDto);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), userDto, PROVISIONING);
    UserSession session = newUserSession(userDto);

    assertThat(session.isSystemAdministrator()).isFalse();
  }

  @Test
  public void keep_isSystemAdministrator_flag_in_cache() {
    organizationFlags.setEnabled(false);
    userDto = db.users().makeNotRoot(userDto);
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), userDto, SYSTEM_ADMIN);
    UserSession session = newUserSession(userDto);

    session.checkIsSystemAdministrator();

    db.getDbClient().userDao().deactivateUserByLogin(db.getSession(), userDto.getLogin());
    db.commit();

    // should fail but succeeds because flag is kept in cache
    session.checkIsSystemAdministrator();
  }

  @Test
  public void checkIsSystemAdministrator_succeeds_if_system_administrator() {
    organizationFlags.setEnabled(true);
    userDto = db.users().makeRoot(userDto);
    UserSession session = newUserSession(userDto);

    session.checkIsSystemAdministrator();
  }

  @Test
  public void checkIsSystemAdministrator_throws_ForbiddenException_if_not_system_administrator() {
    organizationFlags.setEnabled(true);
    userDto = db.users().makeNotRoot(userDto);
    UserSession session = newUserSession(userDto);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    session.checkIsSystemAdministrator();
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
        db.users().insertPermissionOnUser(userDto, permission);
      } else {
        db.users().insertProjectPermissionOnUser(userDto, permission, component);
      }
    }
  }

  private void expectInsufficientPrivilegesForbiddenException() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
  }

}
