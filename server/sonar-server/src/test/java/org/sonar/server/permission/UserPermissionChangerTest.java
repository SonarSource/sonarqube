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
package org.sonar.server.permission;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;


public class UserPermissionChangerTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private UserPermissionChanger underTest = new UserPermissionChanger(db.getDbClient(), defaultOrganizationProvider);
  private OrganizationDto org1;
  private OrganizationDto org2;
  private UserDto user1;
  private UserDto user2;
  private ComponentDto project;

  @Before
  public void setUp() throws Exception {
    org1 = db.organizations().insert();
    org2 = db.organizations().insert();
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    project = db.components().insertProject(org1);
  }

  @Test
  public void add_global_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), SCAN_EXECUTION, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org1)).containsOnly(SCAN_EXECUTION);
    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org2)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user1, project)).isEmpty();
    assertThat(db.users().selectGlobalPermissionsOfUser(user2, org1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user2, project)).isEmpty();
  }

  @Test
  public void add_project_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), ISSUE_ADMIN, new ProjectId(project), UserId.from(user1));
    apply(change);

    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user1, project)).contains(ISSUE_ADMIN);
    assertThat(db.users().selectGlobalPermissionsOfUser(user2, org1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user2, project)).isEmpty();
  }

  @Test
  public void do_nothing_when_adding_global_permission_that_already_exists() {
    db.users().insertPermissionOnUser(org1, user1, QUALITY_GATE_ADMIN);

    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), QUALITY_GATE_ADMIN, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org1)).hasSize(1).containsOnly(QUALITY_GATE_ADMIN);
  }

  @Test
  public void fail_to_add_global_permission_on_project() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid project permission 'gateadmin'. Valid values are [admin, codeviewer, issueadmin, scan, user]");

    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), QUALITY_GATE_ADMIN, new ProjectId(project), UserId.from(user1));
    apply(change);
  }

  @Test
  public void fail_to_add_project_permission_on_organization() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid global permission 'issueadmin'. Valid values are [admin, profileadmin, gateadmin, scan, provisioning]");

    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), ISSUE_ADMIN, null, UserId.from(user1));
    apply(change);
  }

  @Test
  public void remove_global_permission_from_user() {
    db.users().insertPermissionOnUser(org1, user1, QUALITY_GATE_ADMIN);
    db.users().insertPermissionOnUser(org1, user1, SCAN_EXECUTION);
    db.users().insertPermissionOnUser(org2, user1, QUALITY_GATE_ADMIN);
    db.users().insertPermissionOnUser(org1, user2, QUALITY_GATE_ADMIN);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, project);

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), QUALITY_GATE_ADMIN, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org1)).containsOnly(SCAN_EXECUTION);
    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org2)).containsOnly(QUALITY_GATE_ADMIN);
    assertThat(db.users().selectGlobalPermissionsOfUser(user2, org1)).containsOnly(QUALITY_GATE_ADMIN);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, project)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_project_permission_from_user() {
    ComponentDto project2 = db.components().insertProject(org1);
    db.users().insertPermissionOnUser(org1, user1, QUALITY_GATE_ADMIN);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, project);
    db.users().insertProjectPermissionOnUser(user1, USER, project);
    db.users().insertProjectPermissionOnUser(user2, ISSUE_ADMIN, project);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, project2);

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), ISSUE_ADMIN, new ProjectId(project), UserId.from(user1));
    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, project)).containsOnly(USER);
    assertThat(db.users().selectProjectPermissionsOfUser(user2, project)).containsOnly(ISSUE_ADMIN);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, project2)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void do_not_fail_if_removing_a_global_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), QUALITY_GATE_ADMIN, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org1)).isEmpty();
  }

  @Test
  public void do_not_fail_if_removing_a_project_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), ISSUE_ADMIN, new ProjectId(project), UserId.from(user1));
    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, project)).isEmpty();
  }

  @Test
  public void fail_to_remove_admin_global_permission_if_no_more_admins() {
    db.users().insertPermissionOnUser(org1, user1, SYSTEM_ADMIN);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Last user with permission 'admin'. Permission cannot be removed.");

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), SYSTEM_ADMIN, null, UserId.from(user1));
    underTest.apply(db.getSession(), change);
  }

  @Test
  public void remove_admin_user_if_still_other_admins() {
    db.users().insertPermissionOnUser(org1, user1, SYSTEM_ADMIN);
    GroupDto admins = db.users().insertGroup(org1, "admins");
    db.users().insertMember(admins, user2);
    db.users().insertPermissionOnGroup(admins, SYSTEM_ADMIN);

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), SYSTEM_ADMIN, null, UserId.from(user1));
    underTest.apply(db.getSession(), change);

    assertThat(db.users().selectGlobalPermissionsOfUser(user1, org1)).isEmpty();
  }

  private void apply(UserPermissionChange change) {
    underTest.apply(db.getSession(), change);
    db.commit();
  }
}
