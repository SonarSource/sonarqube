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
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

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

  private final ComponentTypes componentTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(componentTypes);
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

        assertThat(db.users().selectAnyonePermissions(privateProject.getUuid())).map(ProjectPermission::fromKey).contains(perm);
      });
  }

  @Test
  public void apply_adds_permission_USER_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(ProjectPermission.USER);
  }

  @Test
  public void apply_adds_permission_CODEVIEWER_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(ProjectPermission.CODEVIEWER);
  }

  @Test
  public void apply_adds_permission_ADMIN_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(ProjectPermission.ADMIN);
  }

  @Test
  public void apply_adds_permission_ISSUE_ADMIN_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(ProjectPermission.ISSUE_ADMIN);
  }

  @Test
  public void apply_adds_permission_SCAN_EXECUTION_to_group_on_private_project() {
    applyAddsPermissionToGroupOnPrivateProject(ProjectPermission.SCAN);
  }

  private void applyAddsPermissionToGroupOnPrivateProject(ProjectPermission permission) {

    apply(new GroupPermissionChange(Operation.ADD, permission, privateProject, group, permissionService));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, privateProject)).containsOnly(permission.getKey());
  }

  @Test
  public void apply_removes_permission_USER_from_group_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(ProjectPermission.USER);
  }

  @Test
  public void apply_removes_permission_CODEVIEWER_from_group_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(ProjectPermission.CODEVIEWER);
  }

  @Test
  public void apply_removes_permission_ADMIN_from_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(ProjectPermission.ADMIN);
  }

  @Test
  public void apply_removes_permission_ISSUE_ADMIN_from_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(ProjectPermission.ISSUE_ADMIN);
  }

  @Test
  public void apply_removes_permission_SCAN_EXECUTION_from_on_private_project() {
    applyRemovesPermissionFromGroupOnPrivateProject(ProjectPermission.SCAN);
  }

  private void applyRemovesPermissionFromGroupOnPrivateProject(ProjectPermission permission) {
    db.users().insertEntityPermissionOnGroup(group, permission, privateProject);

    apply(new GroupPermissionChange(Operation.ADD, permission, privateProject, group, permissionService), permission.getKey());

    assertThat(db.users().selectGroupPermissions(group, privateProject)).containsOnly(permission.getKey());
  }

  @Test
  public void apply_has_no_effect_when_adding_USER_permission_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, ProjectPermission.USER, publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_has_no_effect_when_adding_CODEVIEWER_permission_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, ProjectPermission.CODEVIEWER, publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_fails_with_BadRequestException_when_adding_permission_ADMIN_to_group_AnyOne_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.ADD, ProjectPermission.ADMIN, publicProject, null, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("It is not possible to add the 'admin' permission to group 'Anyone'.");
  }

  @Test
  public void apply_adds_permission_ISSUE_ADMIN_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, ProjectPermission.ISSUE_ADMIN, publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).containsOnly(ProjectPermission.ISSUE_ADMIN.getKey());
  }

  @Test
  public void apply_adds_permission_SCAN_EXECUTION_to_group_AnyOne_on_a_public_project() {
    apply(new GroupPermissionChange(Operation.ADD, GlobalPermission.SCAN.getKey(), publicProject, null, permissionService));

    assertThat(db.users().selectAnyonePermissions(publicProject.getUuid())).containsOnly(GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_USER_permission_from_group_AnyOne_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, ProjectPermission.USER, publicProject, null, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_CODEVIEWER_permission_from_group_AnyOne_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, ProjectPermission.CODEVIEWER.getKey(), publicProject, null, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void apply_removes_ADMIN_permission_from_group_AnyOne_on_a_public_project() {
    applyRemovesPermissionFromGroupAnyOneOnAPublicProject(ProjectPermission.ADMIN.getKey());
  }

  @Test
  public void apply_removes_ISSUE_ADMIN_permission_from_group_AnyOne_on_a_public_project() {
    applyRemovesPermissionFromGroupAnyOneOnAPublicProject(ProjectPermission.ISSUE_ADMIN.getKey());
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
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, ProjectPermission.USER, publicProject, group, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission user can't be removed from a public component");
  }

  @Test
  public void apply_fails_with_BadRequestException_when_removing_CODEVIEWER_permission_from_a_group_on_a_public_project() {
    GroupPermissionChange change = new GroupPermissionChange(Operation.REMOVE, ProjectPermission.CODEVIEWER, publicProject, group, permissionService);
    assertThatThrownBy(() -> apply(change))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Permission codeviewer can't be removed from a public component");
  }

  @Test
  public void add_permission_to_anyone() {
    apply(new GroupPermissionChange(Operation.ADD, ADMINISTER_QUALITY_PROFILES, null, permissionService));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectAnyonePermissions(null)).containsOnly(ADMINISTER_QUALITY_PROFILES.getKey());
  }

  @Test
  public void do_nothing_when_adding_permission_that_already_exists() {
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);

    apply(new GroupPermissionChange(Operation.ADD, ADMINISTER_QUALITY_GATES, group, permissionService), ADMINISTER_QUALITY_GATES.getKey());

    assertThat(db.users().selectGroupPermissions(group, null)).containsExactly(ADMINISTER_QUALITY_GATES.getKey());
  }

  @Test
  public void fail_to_add_global_permission_but_SCAN_and_ADMIN_on_private_project() {
    permissionService.getGlobalPermissions().stream()
      .filter(perm -> !ADMINISTER.equals(perm) && !GlobalPermission.SCAN.equals(perm))
      .forEach(perm -> {
        var permissionKey = perm.getKey();
        assertThatThrownBy(() -> new GroupPermissionChange(Operation.ADD, permissionKey, privateProject, group, permissionService))
          .withFailMessage("a BadRequestException should have been thrown for permission " + perm)
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Invalid project permission '" + perm + "'. Valid values are [" + StringUtils.join(permissionService.getAllProjectPermissions(), ", ") + "]");
      });
  }

  @Test
  public void fail_to_add_global_permission_but_SCAN_and_ADMIN_on_public_project() {
    permissionService.getGlobalPermissions().stream()
      .filter(perm -> !ADMINISTER.equals(perm) && !GlobalPermission.SCAN.equals(perm))
      .forEach(perm -> {
        var permissionKey = perm.getKey();
        assertThatThrownBy(() -> new GroupPermissionChange(Operation.ADD, permissionKey, publicProject, group, permissionService))
          .withFailMessage("a BadRequestException should have been thrown for permission " + perm)
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Invalid project permission '" + perm + "'. Valid values are [" + StringUtils.join(permissionService.getAllProjectPermissions(), ", ") + "]");
      });
  }

  @Test
  public void fail_to_add_project_permission_but_SCAN_and_ADMIN_on_global_group() {
    permissionService.getAllProjectPermissions()
      .stream()
      .filter(perm -> !ProjectPermission.SCAN.equals(perm) && !ProjectPermission.ADMIN.equals(perm))
      .forEach(permission -> {
        var permissionKey = permission.getKey();
        assertThatThrownBy(() -> new GroupPermissionChange(Operation.ADD, permissionKey, null, group, permissionService))
          .withFailMessage("a BadRequestException should have been thrown for permission " + permission)
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Invalid global permission '" + permission + "'. Valid values are [admin, gateadmin, profileadmin, provisioning, scan]");
      });
  }

  @Test
  public void remove_permission_from_group() {
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);
    db.users().insertPermissionOnGroup(group, PROVISION_PROJECTS);

    apply(new GroupPermissionChange(Operation.REMOVE, ADMINISTER_QUALITY_GATES, group, permissionService), ADMINISTER_QUALITY_GATES.getKey(),
      PROVISION_PROJECTS.getKey());

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void remove_project_permission_from_group() {
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ISSUE_ADMIN, privateProject);
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.CODEVIEWER, privateProject);

    apply(new GroupPermissionChange(Operation.REMOVE, ProjectPermission.ISSUE_ADMIN.getKey(), privateProject, group, permissionService), ProjectPermission.ISSUE_ADMIN.getKey(),
      ProjectPermission.CODEVIEWER.getKey());

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(ADMINISTER_QUALITY_GATES.getKey());
    assertThat(db.users().selectGroupPermissions(group, privateProject)).containsOnly(ProjectPermission.CODEVIEWER.getKey());
  }

  @Test
  public void do_not_fail_if_removing_a_permission_that_does_not_exist() {
    apply(new GroupPermissionChange(Operation.REMOVE, ProjectPermission.ISSUE_ADMIN.getKey(), privateProject, group, permissionService));

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

  private void unsafeInsertProjectPermissionOnAnyone(ProjectPermission perm) {
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
