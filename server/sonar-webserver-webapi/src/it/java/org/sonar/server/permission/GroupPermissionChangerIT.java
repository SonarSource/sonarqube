/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class GroupPermissionChangerIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final GroupPermissionChanger underTest = new GroupPermissionChanger(db.getDbClient(), new SequenceUuidFactory());
  private GroupDto group;

  private ProjectDto privateProject;
  private ProjectDto publicProject;

  @Before
  public void setUp() {
    group = db.users().insertGroup("a-group");
    privateProject = db.components().insertPrivateProject().getProjectDto();
    publicProject = db.components().insertPublicProject().getProjectDto();
  }

  @Test
  public void apply_adds_global_permission_to_group() {
    apply(new GroupPermissionChange(Operation.ADD, ADMINISTER_QUALITY_PROFILES.getKey(), null, group, permissionService));

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(ADMINISTER_QUALITY_PROFILES.getKey());
  }

  @Test
  public void apply_adds_global_permission_to_group_AnyOne() {
    apply(new GroupPermissionChange(Operation.ADD, ADMINISTER_QUALITY_PROFILES.getKey(), null, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(null)).containsOnly(ADMINISTER_QUALITY_PROFILES.getKey());
  }

  @Test
  public void apply_fails_with_BadRequestException_when_adding_any_permission_to_group_AnyOne_on_private_project() {
    permissionService.getAllProjectPermissions()
      .forEach(perm -> {
        GroupPermissionChange change = new GroupPermissionChange(Operation.ADD, perm, privateProject, null, permissionService);
        try {
          apply(change);
          fail("a BadRequestException should have been thrown");
        } catch (BadRequestException e) {
          assertThat(e).hasMessage("No permission can be granted to Anyone on a private component");
        }
      });
  }

  @Test
  public void apply_has_no_effect_when_removing_any_permission_to_group_AnyOne_on_private_project() {
    permissionService.getAllProjectPermissions()
      .forEach(this::unsafeInsertProjectPermissionOnAnyone);

    permissionService.getAllProjectPermissions()
      .forEach(perm -> {
        apply(new GroupPermissionChange(Operation.REMOVE, perm, privateProject, null, permissionService));

        assertThat(db.users().selectAnyonePermissions(privateProject.getUuid())).contains(perm);
      });
  }

  @Test
  public void apply_adds_permission_USER_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(UserRole.USER);
  }

  @Test
  public void apply_adds_permission_CODEVIEWER_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(UserRole.CODEVIEWER);
  }

  @Test
  public void apply_adds_permission_ADMIN_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(UserRole.ADMIN);
  }

  @Test
  public void apply_adds_permission_ISSUE_ADMIN_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void apply_adds_permission_SCAN_EXECUTION_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(GlobalPermission.SCAN.getKey());
  }

  private void applyAddsPermissionToGroupOnPrivateProject(String permission) {

    apply(new GroupPermissionChange(Operation.ADD, permission, privateProject, group, permissionService));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, privateProject)).containsOnly(permission);
  }

  @Test
  public void apply_removes_permission_USER_from_group_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(UserRole.USER);
  }

  @Test
  public void apply_removes_permission_CODEVIEWER_from_group_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(UserRole.CODEVIEWER);
  }

  @Test
  public void apply_removes_permission_ADMIN_from_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(UserRole.ADMIN);
  }

  @Test
  public void apply_removes_permission_ISSUE_ADMIN_from_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void apply_removes_permission_SCAN_EXECUTION_from_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(GlobalPermission.SCAN.getKey());
  }

  private void applyRemovesPermissionFromGroupOnPrivateProject(String permission) {
    db.users().insertEntityPermissionOnGroup(group, permission, privateProject);

    apply(new GroupPermissionChange(Operation.ADD, permission, privateProject, group, permissionService), permission);

    assertThat(db.users().selectGroupPermissions(group, privateProject)).containsOnly(permission);
  }

  @Test
  public void apply_has_no_effect_when_adding_USER_permission_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, UserRole.USER, publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_has_no_effect_when_adding_CODEVIEWER_permission_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, UserRole.CODEVIEWER, publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_fails_with_BadRequestException_when_adding_permission_ADMIN_to_group_AnyOne_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.ADD, UserRole.ADMIN, publicProject, null, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("It is not possible to add the 'admin' permission to group 'Anyone'.");
  }

  @Test
  public void apply_adds_permission_ISSUE_ADMIN_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, UserRole.ISSUE_ADMIN, publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void apply_adds_permission_SCAN_EXECUTION_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, GlobalPermission.SCAN.getKey(), publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_USER_permission_from_group_AnyOne_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, UserRole.USER, publicProject, null, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_CODEVIEWER_permission_from_group_AnyOne_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, UserRole.CODEVIEWER, publicProject, null, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void apply_removes_ADMIN_permission_from_group_AnyOne_on_a_public_project() {
    applyRemovesPermissionFromGroupAnyOneOnAPublicProject(UserRole.ADMIN);
  }

  @Test
  public void apply_removes_ISSUE_ADMIN_permission_from_group_AnyOne_on_a_public_project() {
    applyRemovesPermissionFromGroupAnyOneOnAPublicProject(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void apply_removes_SCAN_EXECUTION_permission_from_group_AnyOne_on_a_public_project() {
    applyRemovesPermissionFromGroupAnyOneOnAPublicProject(GlobalPermission.SCAN.getKey());
  }

  private void applyRemovesPermissionFromGroupAnyOneOnAPublicProject(String permission) {
    db.users().insertEntityPermissionOnAnyone(permission, publicProject);

    apply(new GroupPermissionChange(Operation.REMOVE, permission, publicProject, null, permissionService), permission);

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_USER_permission_from_a_group_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, UserRole.USER, publicProject, group, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_CODEVIEWER_permission_from_a_group_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, UserRole.CODEVIEWER, publicProject, group, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void add_permission_to_anyone() {
    apply(new GroupPermissionChange(Operation.ADD, ADMINISTER_QUALITY_PROFILES.getKey(), null, null, permissionService));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectAnyonePermissions(null)).containsOnly(ADMINISTER_QUALITY_PROFILES.getKey());
  }

  @Test
  public void do_nothing_when_adding_permission_that_already_exists() {
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);

    apply(new GroupPermissionChange(Operation.ADD, ADMINISTER_QUALITY_GATES.getKey(), null, group, permissionService), ADMINISTER_QUALITY_GATES.getKey());

    assertThat(db.users().selectGroupPermissions(group, null)).containsExactly(ADMINISTER_QUALITY_GATES.getKey());
  }

  @Test
  public void fail_to_add_global_permission_but_SCAN_and_ADMIN_on_private_project() {
    permissionService.getGlobalPermissions().stream()
      .map(GlobalPermission::getKey)
      .filter(perm -> !UserRole.ADMIN.equals(perm) && !GlobalPermission.SCAN.getKey().equals(perm))
      .forEach(perm -> {
        try {
          new GroupPermissionChange(Operation.ADD, perm, privateProject, group, permissionService);
          fail("a BadRequestException should have been thrown for permission " + perm);
        } catch (BadRequestException e) {
          assertThat(e).hasMessage("Invalid project permission '" + perm +
                                   "'. Valid values are [" + StringUtils.join(permissionService.getAllProjectPermissions(), ", ") + "]");
        }
      });
  }

  @Test
  public void fail_to_add_global_permission_but_SCAN_and_ADMIN_on_public_project() {
    permissionService.getGlobalPermissions().stream()
      .map(GlobalPermission::getKey)
      .filter(perm -> !UserRole.ADMIN.equals(perm) && !GlobalPermission.SCAN.getKey().equals(perm))
      .forEach(perm -> {
        try {
          new GroupPermissionChange(Operation.ADD, perm, publicProject, group, permissionService);
          fail("a BadRequestException should have been thrown for permission " + perm);
        } catch (BadRequestException e) {
          assertThat(e).hasMessage("Invalid project permission '" + perm +
                                   "'. Valid values are [" + StringUtils.join(permissionService.getAllProjectPermissions(), ", ") + "]");
        }
      });
  }

  @Test
  public void fail_to_add_project_permission_but_SCAN_and_ADMIN_on_global_group() {
    permissionService.getAllProjectPermissions()
      .stream()
      .filter(perm -> !GlobalPermission.SCAN.getKey().equals(perm) && !GlobalPermission.ADMINISTER.getKey().equals(perm))
      .forEach(permission -> {
        try {
          new GroupPermissionChange(Operation.ADD, permission, null, group, permissionService);
          fail("a BadRequestException should have been thrown for permission " + permission);
        } catch (BadRequestException e) {
          assertThat(e).hasMessage("Invalid global permission '" + permission + "'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");
        }
      });
  }

  @Test
  public void remove_permission_from_group() {
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);
    db.users().insertPermissionOnGroup(group, PROVISION_PROJECTS);

    apply(new GroupPermissionChange(Operation.REMOVE, ADMINISTER_QUALITY_GATES.getKey(), null, group, permissionService), ADMINISTER_QUALITY_GATES.getKey(),
      PROVISION_PROJECTS.getKey());

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void remove_project_permission_from_group() {
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);
    db.users().insertEntityPermissionOnGroup(group, UserRole.ISSUE_ADMIN, privateProject);
    db.users().insertEntityPermissionOnGroup(group, UserRole.CODEVIEWER, privateProject);

    apply(new GroupPermissionChange(Operation.REMOVE, UserRole.ISSUE_ADMIN, privateProject, group, permissionService), UserRole.ISSUE_ADMIN,
      UserRole.CODEVIEWER);

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(ADMINISTER_QUALITY_GATES.getKey());
    assertThat(db.users().selectGroupPermissions(group, privateProject)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void do_not_fail_if_removing_a_permission_that_does_not_exist() {
    apply(new GroupPermissionChange(Operation.REMOVE, UserRole.ISSUE_ADMIN, privateProject, group, permissionService));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, privateProject)).isEmpty();
  }

  @Test
  public void fail_to_remove_admin_permission_if_no_more_admins() {
    db.users().insertPermissionOnGroup(group, ADMINISTER);

    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, ADMINISTER.getKey(), null, group, permissionService);
    Set<String> permission = Set.of("admin");
    DbSession session = db.getSession();
    assertThatThrownBy(() -> underTest.apply(session, permission, change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Last group with permission 'admin'. Permission cannot be removed.");
  }

  @Test
  public void remove_admin_group_if_still_other_admins() {
    db.users().insertPermissionOnGroup(group, ADMINISTER);
    UserDto admin = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(admin, ADMINISTER);

    apply(new GroupPermissionChange(Operation.REMOVE, ADMINISTER.getKey(), null, group, permissionService), ADMINISTER.getKey());

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
  }

  private void apply(GroupPermissionChange change, String... existingPermissions) {
    underTest.apply(db.getSession(), Set.of(existingPermissions), change);
    db.commit();
  }

  private void unsafeInsertProjectPermissionOnAnyone(String perm) {
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(null)
      .setRole(perm)
      .setEntityUuid(privateProject.getUuid())
      .setEntityName(privateProject.getName());
    db.getDbClient().groupPermissionDao().insert(db.getSession(), dto, privateProject, null);
    db.commit();
  }
}
