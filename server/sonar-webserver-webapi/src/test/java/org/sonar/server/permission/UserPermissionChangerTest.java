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
package org.sonar.server.permission;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.permission.PermissionChange.Operation.ADD;
import static org.sonar.server.permission.PermissionChange.Operation.REMOVE;

public class UserPermissionChangerTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);


  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final UserPermissionChanger underTest = new UserPermissionChanger(db.getDbClient(), new SequenceUuidFactory());
  private UserDto user1;
  private UserDto user2;
  private ComponentDto privateProject;
  private ComponentDto publicProject;

  @Before
  public void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    privateProject = db.components().insertPrivateProject();
    publicProject = db.components().insertPublicProject();
  }

  @Test
  public void apply_adds_any_global_permission_to_user() {
    permissionService.getGlobalPermissions()
      .forEach(perm -> {
        UserPermissionChange change = new UserPermissionChange(ADD, perm.getKey(), null, UserIdDto.from(user1), permissionService);

        apply(change);

        assertThat(db.users().selectPermissionsOfUser(user1)).contains(perm);
      });
  }

  @Test
  public void apply_removes_any_global_permission_to_user() {
    // give ADMIN perm to user2 so that user1 is not the only one with this permission and it can be removed from user1
    db.users().insertPermissionOnUser(user2, GlobalPermission.ADMINISTER);
    permissionService.getGlobalPermissions()
      .forEach(perm -> db.users().insertPermissionOnUser(user1, perm));
    assertThat(db.users().selectPermissionsOfUser(user1))
      .containsOnly(permissionService.getGlobalPermissions().toArray(new GlobalPermission[0]));

    permissionService.getGlobalPermissions()
      .forEach(perm -> {
        UserPermissionChange change = new UserPermissionChange(REMOVE, perm.getKey(), null, UserIdDto.from(user1), permissionService);

        apply(change);

        assertThat(db.users().selectPermissionsOfUser(user1)).doesNotContain(perm);
      });
  }

  @Test
  public void apply_has_no_effect_when_adding_permission_USER_on_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(ADD, USER, publicProject, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).doesNotContain(USER);
  }

  @Test
  public void apply_has_no_effect_when_adding_permission_CODEVIEWER_on_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(ADD, CODEVIEWER, publicProject, UserIdDto.from(user1), permissionService);

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
    UserPermissionChange change = new UserPermissionChange(ADD, permission, publicProject, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).containsOnly(permission);
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_permission_USER_from_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, USER, publicProject, UserIdDto.from(user1), permissionService);

    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_permission_CODEVIEWER_from_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, CODEVIEWER, publicProject, UserIdDto.from(user1), permissionService);

    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
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
    UserPermissionChange change = new UserPermissionChange(REMOVE, permission, publicProject, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, publicProject)).isEmpty();
  }

  @Test
  public void apply_adds_any_permission_to_a_private_project() {
    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        UserPermissionChange change = new UserPermissionChange(ADD, permission, privateProject, UserIdDto.from(user1), permissionService);

        apply(change);

        assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).contains(permission);
      });
  }

  @Test
  public void apply_removes_any_permission_from_a_private_project() {
    permissionService.getAllProjectPermissions()
      .forEach(permission -> db.users().insertProjectPermissionOnUser(user1, permission, privateProject));

    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        UserPermissionChange change = new UserPermissionChange(REMOVE, permission, privateProject, UserIdDto.from(user1), permissionService);

        apply(change);

        assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).doesNotContain(permission);
      });
  }

  @Test
  public void add_global_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, SCAN_EXECUTION, null, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).containsOnly(SCAN);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).isEmpty();
    assertThat(db.users().selectPermissionsOfUser(user2)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user2, privateProject)).isEmpty();
  }

  @Test
  public void add_project_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, ISSUE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).contains(ISSUE_ADMIN);
    assertThat(db.users().selectPermissionsOfUser(user2)).isEmpty();
    assertThat(db.users().selectProjectPermissionsOfUser(user2, privateProject)).isEmpty();
  }

  @Test
  public void do_nothing_when_adding_global_permission_that_already_exists() {
    db.users().insertPermissionOnUser(user1, ADMINISTER_QUALITY_GATES);

    UserPermissionChange change = new UserPermissionChange(ADD, QUALITY_GATE_ADMIN, null, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).containsOnly(ADMINISTER_QUALITY_GATES);
  }

  @Test
  public void fail_to_add_global_permission_on_project() {
    assertThatThrownBy(() -> {
      UserPermissionChange change = new UserPermissionChange(ADD, QUALITY_GATE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
      apply(change);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Invalid project permission 'gateadmin'. Valid values are [" + StringUtils.join(permissionService.getAllProjectPermissions(), ", ") + "]");
  }

  @Test
  public void fail_to_add_project_permission() {
    assertThatThrownBy(() -> {
      UserPermissionChange change = new UserPermissionChange(ADD, ISSUE_ADMIN, null, UserIdDto.from(user1), permissionService);
      apply(change);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Invalid global permission 'issueadmin'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");
  }

  @Test
  public void remove_global_permission_from_user() {
    db.users().insertPermissionOnUser(user1, QUALITY_GATE_ADMIN);
    db.users().insertPermissionOnUser(user1, SCAN_EXECUTION);
    db.users().insertPermissionOnUser(user2, QUALITY_GATE_ADMIN);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, privateProject);

    UserPermissionChange change = new UserPermissionChange(REMOVE, QUALITY_GATE_ADMIN, null, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).containsOnly(SCAN);
    assertThat(db.users().selectPermissionsOfUser(user2)).containsOnly(ADMINISTER_QUALITY_GATES);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void remove_project_permission_from_user() {
    ComponentDto project2 = db.components().insertPrivateProject();
    db.users().insertPermissionOnUser(user1, ADMINISTER_QUALITY_GATES);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, privateProject);
    db.users().insertProjectPermissionOnUser(user1, USER, privateProject);
    db.users().insertProjectPermissionOnUser(user2, ISSUE_ADMIN, privateProject);
    db.users().insertProjectPermissionOnUser(user1, ISSUE_ADMIN, project2);

    UserPermissionChange change = new UserPermissionChange(REMOVE, ISSUE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).containsOnly(USER);
    assertThat(db.users().selectProjectPermissionsOfUser(user2, privateProject)).containsOnly(ISSUE_ADMIN);
    assertThat(db.users().selectProjectPermissionsOfUser(user1, project2)).containsOnly(ISSUE_ADMIN);
  }

  @Test
  public void do_not_fail_if_removing_a_global_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, QUALITY_GATE_ADMIN, null, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).isEmpty();
  }

  @Test
  public void do_not_fail_if_removing_a_project_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, ISSUE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectProjectPermissionsOfUser(user1, privateProject)).isEmpty();
  }

  @Test
  public void fail_to_remove_admin_global_permission_if_no_more_admins() {
    db.users().insertPermissionOnUser(user1, SYSTEM_ADMIN);

    assertThatThrownBy(() -> {
      UserPermissionChange change = new UserPermissionChange(REMOVE, SYSTEM_ADMIN, null, UserIdDto.from(user1), permissionService);
      underTest.apply(db.getSession(), change);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Last user with permission 'admin'. Permission cannot be removed.");
  }

  @Test
  public void remove_admin_user_if_still_other_admins() {
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    GroupDto admins = db.users().insertGroup("admins");
    db.users().insertMember(admins, user2);
    db.users().insertPermissionOnGroup(admins, ADMINISTER);

    UserPermissionChange change = new UserPermissionChange(REMOVE, ADMINISTER.getKey(), null, UserIdDto.from(user1), permissionService);
    underTest.apply(db.getSession(), change);

    assertThat(db.users().selectPermissionsOfUser(user1)).isEmpty();
  }

  private void apply(UserPermissionChange change) {
    underTest.apply(db.getSession(), change);
    db.commit();
  }
}
