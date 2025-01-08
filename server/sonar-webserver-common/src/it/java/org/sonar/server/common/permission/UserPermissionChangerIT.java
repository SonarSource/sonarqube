/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.permission;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.common.permission.Operation.ADD;
import static org.sonar.server.common.permission.Operation.REMOVE;
import static org.sonar.server.permission.PermissionServiceImpl.ALL_PROJECT_PERMISSIONS;

public class UserPermissionChangerIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
  private final UserPermissionChanger underTest = new UserPermissionChanger(db.getDbClient(), new SequenceUuidFactory());
  private UserDto user1;
  private UserDto user2;
  private EntityDto privateProject;
  private EntityDto publicProject;

  @Before
  public void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    privateProject = db.components().insertPrivateProject().getProjectDto();
    publicProject = db.components().insertPublicProject().getProjectDto();
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
    db.users().insertGlobalPermissionOnUser(user2, GlobalPermission.ADMINISTER);
    permissionService.getGlobalPermissions()
      .forEach(perm -> db.users().insertGlobalPermissionOnUser(user1, perm));
    assertThat(db.users().selectPermissionsOfUser(user1))
      .containsOnly(permissionService.getGlobalPermissions().toArray(new GlobalPermission[0]));

    permissionService.getGlobalPermissions()
      .forEach(perm -> {
        UserPermissionChange change = new UserPermissionChange(REMOVE, perm.getKey(), null, UserIdDto.from(user1), permissionService);

        apply(change, permissionService.getGlobalPermissions().stream().map(GlobalPermission::getKey).collect(toSet()));

        assertThat(db.users().selectPermissionsOfUser(user1)).doesNotContain(perm);
      });
  }

  @Test
  public void apply_has_no_effect_when_adding_permission_USER_on_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(ADD, UserRole.USER, publicProject, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectEntityPermissionOfUser(user1, publicProject.getUuid())).doesNotContain(UserRole.USER);
  }

  @Test
  public void apply_has_no_effect_when_adding_permission_CODEVIEWER_on_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(ADD, UserRole.CODEVIEWER, publicProject, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectEntityPermissionOfUser(user1, publicProject.getUuid())).doesNotContain(UserRole.CODEVIEWER);
  }

  @Test
  public void apply_adds_permission_ADMIN_on_a_public_project() {
    applyAddsPermissionOnAPublicProject(UserRole.ADMIN);
  }

  @Test
  public void apply_adds_permission_ISSUE_ADMIN_on_a_public_project() {
    applyAddsPermissionOnAPublicProject(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void apply_adds_permission_SCAN_EXECUTION_on_a_public_project() {
    applyAddsPermissionOnAPublicProject(GlobalPermission.SCAN.getKey());
  }

  private void applyAddsPermissionOnAPublicProject(String permission) {
    UserPermissionChange change = new UserPermissionChange(ADD, permission, publicProject, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectEntityPermissionOfUser(user1, publicProject.getUuid())).containsOnly(permission);
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_permission_USER_from_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, UserRole.USER, publicProject, UserIdDto.from(user1), permissionService);

    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_permission_CODEVIEWER_from_a_public_project() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, UserRole.CODEVIEWER, publicProject, UserIdDto.from(user1), permissionService);

    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void apply_removes_permission_ADMIN_from_a_public_project() {
    applyRemovesPermissionFromPublicProject(UserRole.ADMIN);
  }

  @Test
  public void apply_removes_permission_ISSUE_ADMIN_from_a_public_project() {
    applyRemovesPermissionFromPublicProject(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void apply_removes_permission_SCAN_EXECUTION_from_a_public_project() {
    applyRemovesPermissionFromPublicProject(GlobalPermission.SCAN.getKey());
  }

  private void applyRemovesPermissionFromPublicProject(String permission) {
    db.users().insertProjectPermissionOnUser(user1, permission, publicProject);
    UserPermissionChange change = new UserPermissionChange(REMOVE, permission, publicProject, UserIdDto.from(user1), permissionService);

    apply(change, Set.of(permission));

    assertThat(db.users().selectEntityPermissionOfUser(user1, publicProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_adds_any_permission_to_a_private_project() {
    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        UserPermissionChange change = new UserPermissionChange(ADD, permission, privateProject, UserIdDto.from(user1), permissionService);

        apply(change);

        assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).contains(permission);
      });
  }

  @Test
  public void apply_removes_any_permission_from_a_private_project() {
    permissionService.getAllProjectPermissions()
      .forEach(permission -> db.users().insertProjectPermissionOnUser(user1, permission, privateProject));

    permissionService.getAllProjectPermissions()
      .forEach(permission -> {
        UserPermissionChange change = new UserPermissionChange(REMOVE, permission, privateProject, UserIdDto.from(user1), permissionService);

        apply(change, ALL_PROJECT_PERMISSIONS);

        assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).doesNotContain(permission);
      });
  }

  @Test
  public void add_global_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, GlobalPermission.SCAN.getKey(), null, UserIdDto.from(user1), permissionService);

    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).containsOnly(GlobalPermission.SCAN);
    assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).isEmpty();
    assertThat(db.users().selectPermissionsOfUser(user2)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user2, privateProject.getUuid())).isEmpty();
  }

  @Test
  public void add_project_permission_to_user() {
    UserPermissionChange change = new UserPermissionChange(ADD, UserRole.ISSUE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).contains(UserRole.ISSUE_ADMIN);
    assertThat(db.users().selectPermissionsOfUser(user2)).isEmpty();
    assertThat(db.users().selectEntityPermissionOfUser(user2, privateProject.getUuid())).isEmpty();
  }

  @Test
  public void do_nothing_when_adding_global_permission_that_already_exists() {
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER_QUALITY_GATES);

    UserPermissionChange change = new UserPermissionChange(ADD, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), null, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).containsOnly(GlobalPermission.ADMINISTER_QUALITY_GATES);
  }

  @Test
  public void fail_to_add_global_permission_on_project() {
    assertThatThrownBy(() -> {
      UserPermissionChange change = new UserPermissionChange(ADD, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), privateProject, UserIdDto.from(user1), permissionService);
      apply(change);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Invalid project permission 'gateadmin'. Valid values are [" + StringUtils.join(permissionService.getAllProjectPermissions(), ", ") + "]");
  }

  @Test
  public void fail_to_add_project_permission() {
    assertThatThrownBy(() -> {
      UserPermissionChange change = new UserPermissionChange(ADD, UserRole.ISSUE_ADMIN, null, UserIdDto.from(user1), permissionService);
      apply(change);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Invalid global permission 'issueadmin'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");
  }

  @Test
  public void remove_global_permission_from_user() {
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER_QUALITY_GATES);
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user2, GlobalPermission.ADMINISTER_QUALITY_GATES);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, privateProject);

    UserPermissionChange change = new UserPermissionChange(REMOVE, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), null, UserIdDto.from(user1), permissionService);
    apply(change, Set.of(GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), GlobalPermission.SCAN.getKey(), UserRole.ISSUE_ADMIN));

    assertThat(db.users().selectPermissionsOfUser(user1)).containsOnly(GlobalPermission.SCAN);
    assertThat(db.users().selectPermissionsOfUser(user2)).containsOnly(GlobalPermission.ADMINISTER_QUALITY_GATES);
    assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void remove_project_permission_from_user() {
    EntityDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER_QUALITY_GATES);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, privateProject);
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, privateProject);
    db.users().insertProjectPermissionOnUser(user2, UserRole.ISSUE_ADMIN, privateProject);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, project2);

    UserPermissionChange change = new UserPermissionChange(REMOVE, UserRole.ISSUE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
    apply(change, Set.of(GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), UserRole.ISSUE_ADMIN, UserRole.USER));

    assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).containsOnly(UserRole.USER);
    assertThat(db.users().selectEntityPermissionOfUser(user2, privateProject.getUuid())).containsOnly(UserRole.ISSUE_ADMIN);
    assertThat(db.users().selectEntityPermissionOfUser(user1, project2.getUuid())).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void do_not_fail_if_removing_a_global_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey(), null, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectPermissionsOfUser(user1)).isEmpty();
  }

  @Test
  public void do_not_fail_if_removing_a_project_permission_that_does_not_exist() {
    UserPermissionChange change = new UserPermissionChange(REMOVE, UserRole.ISSUE_ADMIN, privateProject, UserIdDto.from(user1), permissionService);
    apply(change);

    assertThat(db.users().selectEntityPermissionOfUser(user1, privateProject.getUuid())).isEmpty();
  }

  @Test
  public void fail_to_remove_admin_global_permission_if_no_more_admins() {
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER);

    UserPermissionChange change = new UserPermissionChange(REMOVE, GlobalPermission.ADMINISTER.getKey(), null, UserIdDto.from(user1), permissionService);
    DbSession session = db.getSession();
    Set<String> permissions = Set.of(GlobalPermission.ADMINISTER.getKey());
    assertThatThrownBy(() -> underTest.apply(session, permissions, change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Last user with permission 'admin'. Permission cannot be removed.");
  }

  @Test
  public void remove_admin_user_if_still_other_admins() {
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER);
    GroupDto admins = db.users().insertGroup("admins");
    db.users().insertMember(admins, user2);
    db.users().insertPermissionOnGroup(admins, GlobalPermission.ADMINISTER);

    UserPermissionChange change = new UserPermissionChange(REMOVE, GlobalPermission.ADMINISTER.getKey(), null, UserIdDto.from(user1), permissionService);
    underTest.apply(db.getSession(), Set.of(GlobalPermission.ADMINISTER.getKey()), change);

    assertThat(db.users().selectPermissionsOfUser(user1)).isEmpty();
  }

  private void apply(UserPermissionChange change) {
    underTest.apply(db.getSession(), Set.of(), change);
    db.commit();
  }
  private void apply(UserPermissionChange change, Set<String> existingPermissions) {
    underTest.apply(db.getSession(), existingPermissions, change);
    db.commit();
  }
}
