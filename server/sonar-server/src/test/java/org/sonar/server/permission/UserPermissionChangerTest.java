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
package org.sonar.server.permission;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;

public class UserPermissionChangerTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UserPermissionChanger underTest = new UserPermissionChanger(db.getDbClient());
  private OrganizationDto org1;
  private OrganizationDto org2;
  private UserDto user1;
  private UserDto user2;
  private ComponentDto privateProject;
  private ComponentDto publicProject;

  @Before
  public void setUp() throws Exception {
    org1 = db.organizations().insert();
    org2 = db.organizations().insert();
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    privateProject = db.components().insertPrivateProject(org1);
    publicProject = db.components().insertPublicProject(org1);
  }

  @Test
  public void apply_adds_any_organization_permission_to_user() {
    OrganizationPermission.all()
      .forEach(perm -> {
        UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), perm.getKey(), null, UserId.from(user1));

        apply(change);

        assertThat(db.users().selectPermissionsOfUser(user1, org1)).contains(perm);
      });
  }

  @Test
  public void apply_removes_any_organization_permission_to_user() {
    // give ADMIN perm to user2 so that user1 is not the only one with this permission and it can be removed from user1
    db.users().insertPermissionOnUser(org1, user2, OrganizationPermission.ADMINISTER);
    OrganizationPermission.all()
      .forEach(perm -> db.users().insertPermissionOnUser(org1, user1, perm));
    assertThat(db.users().selectPermissionsOfUser(user1, org1)).containsOnly(OrganizationPermission.values());

    OrganizationPermission.all()
      .forEach(perm -> {
        UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), perm.getKey(), null, UserId.from(user1));

        apply(change);

        assertThat(db.users().selectPermissionsOfUser(user1, org1)).doesNotContain(perm);
      });
  }

  @Test
  public void apply_has_no_effect_when_adding_permission_USER_on_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), USER, new ProjectId(publicProject), UserId.from(user1));

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).doesNotContain(USER);
  }

  @Test
  public void apply_has_no_effect_when_adding_permission_CODEVIEWER_on_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), CODEVIEWER, new ProjectId(publicProject), UserId.from(user1));

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).doesNotContain(CODEVIEWER);
  }

  @Test
  public void apply_adds_permission_ADMIN_on_a_public_project() {
    applyAddsPermissionOnAPublicProject(ADMIN);
  }

  @Test
  public void apply_adds_permission_ISSUE_ADMIN_on_a_public_project() {
    applyAddsPermissionOnAPublicProject(ISSUE_ADMIN);
  }

  @Test
  public void apply_adds_permission_SCAN_EXECUTION_on_a_public_project() {
    applyAddsPermissionOnAPublicProject(SCAN_EXECUTION);
  }

  private void applyAddsPermissionOnAPublicProject(String permission) {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), permission, new ProjectId(publicProject), UserId.from(user1));

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).containsOnly(permission);
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_permission_USER_from_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), USER, new ProjectId(publicProject), UserId.from(user1));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Permission user can't be removed from a public component");

    apply(change);
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_permission_CODEVIEWER_from_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), CODEVIEWER, new ProjectId(publicProject), UserId.from(user1));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Permission codeviewer can't be removed from a public component");

    apply(change);
  }

  @Test
  public void apply_removes_permission_ADMIN_from_a_public_project() {
    applyRemovesPermissionFromPublicProject(ADMIN);
  }

  @Test
  public void apply_removes_permission_ISSUE_ADMIN_from_a_public_project() {
    applyRemovesPermissionFromPublicProject(ISSUE_ADMIN);
  }

  @Test
  public void apply_removes_permission_SCAN_EXECUTION_from_a_public_project() {
    applyRemovesPermissionFromPublicProject(SCAN_EXECUTION);
  }

  private void applyRemovesPermissionFromPublicProject(String permission) {
    db.users().insertProjectPermissionOnUser(user1, permission, publicProject);
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), permission, new ProjectId(publicProject), UserId.from(user1));

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).isEmpty();
  }

  @Test
  public void apply_adds_any_permission_to_a_private_project() {
    ProjectPermissions.ALL
      .forEach(permission -> {
        UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), permission, new ProjectId(privateProject), UserId.from(user1));

        apply(change);

        assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).contains(permission);
      });
  }

  @Test
  public void apply_removes_any_permission_from_a_private_project() {
    ProjectPermissions.ALL
      .forEach(permission -> db.users().insertProjectPermissionOnUser(user1, permission, privateProject));

    ProjectPermissions.ALL
      .forEach(permission -> {
        UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), permission, new ProjectId(privateProject), UserId.from(user1));

        apply(change);

        assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).doesNotContain(permission);
      });
  }

  @Test
  public void add_global_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), SCAN_EXECUTION, null, UserId.from(user1));

    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1, org1)).containsOnly(SCAN);
    assertThat(db.users().selectPermissionsOfUser(user1, org2)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).isEmpty();
    assertThat(db.users().selectPermissionsOfUser(user2, org1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user2, privateProject)).isEmpty();
  }

  @Test
  public void add_project_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), ISSUE_ADMIN, new ProjectId(privateProject), UserId.from(user1));
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1, org1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).contains(ISSUE_ADMIN);
    assertThat(db.users().selectPermissionsOfUser(user2, org1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user2, privateProject)).isEmpty();
  }

  @Test
  public void do_nothing_when_adding_global_permission_that_already_exists() {
    db.users().insertPermissionOnUser(org1, user1, ADMINISTER_QUALITY_GATES);

    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), QUALITY_GATE_ADMIN, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1, org1)).containsOnly(ADMINISTER_QUALITY_GATES);
  }

  @Test
  public void fail_to_add_global_permission_on_project() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid project permission 'gateadmin'. Valid values are [admin, codeviewer, issueadmin, scan, user]");

    UserPermissionChange change = new UserPermissionChange(ADD, org1.getUuid(), QUALITY_GATE_ADMIN, new ProjectId(privateProject), UserId.from(user1));
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
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, privateProject);

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), QUALITY_GATE_ADMIN, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1, org1)).containsOnly(SCAN);
    assertThat(db.users().selectPermissionsOfUser(user1, org2)).containsOnly(ADMINISTER_QUALITY_GATES);
    assertThat(db.users().selectPermissionsOfUser(user2, org1)).containsOnly(ADMINISTER_QUALITY_GATES);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_project_permission_from_user() {
    ComponentDto project2 = db.components().insertPrivateProject(org1);
    db.users().insertPermissionOnUser(user1, ADMINISTER_QUALITY_GATES);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, privateProject);
    db.users().insertProjectPermissionOnUser(user1, USER, privateProject);
    db.users().insertProjectPermissionOnUser(user2, ISSUE_ADMIN, privateProject);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, project2);

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), ISSUE_ADMIN, new ProjectId(privateProject), UserId.from(user1));
    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).containsOnly(USER);
    assertThat(db.users().selectProjectPermissionsOfUser(user2, privateProject)).containsOnly(ISSUE_ADMIN);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, project2)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void do_not_fail_if_removing_a_global_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), QUALITY_GATE_ADMIN, null, UserId.from(user1));
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1, org1)).isEmpty();
  }

  @Test
  public void do_not_fail_if_removing_a_project_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), ISSUE_ADMIN, new ProjectId(privateProject), UserId.from(user1));
    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).isEmpty();
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
    db.users().insertPermissionOnUser(org1, user1, ADMINISTER);
    GroupDto admins = db.users().insertGroup(org1, "admins");
    db.users().insertMember(admins, user2);
    db.users().insertPermissionOnGroup(admins, ADMINISTER);

    UserPermissionChange change = new UserPermissionChange(REMOVE, org1.getUuid(), ADMINISTER.getKey(), null, UserId.from(user1));
    underTest.apply(db.getSession(), change);

    assertThat(db.users().selectPermissionsOfUser(user1, org1)).isEmpty();
  }

  private void apply(UserPermissionChange change) {
    underTest.apply(db.getSession(), change);
    db.commit();
  }
}
