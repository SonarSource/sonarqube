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
package org.sonar.db.user;

import java.util.List;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;

class RoleDaoIT {

  private static final Set<String> PROJECT_QUALIFIER = Set.of(ComponentQualifiers.PROJECT);

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final RoleDao underTest = db.getDbClient().roleDao();

  private UserDto user1;
  private UserDto user2;
  private ProjectDto project1;
  private ProjectDto project2;

  @BeforeEach
  void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    project1 = db.components().insertPrivateProject(project -> project.setName("project1")).getProjectDto();
    project2 = db.components().insertPrivateProject(project -> project.setName("project2")).getProjectDto();
  }

  @Test
  void selectComponentUuidsByPermissionAndUserId_throws_IAR_if_permission_USER_is_specified() {
    expectUnsupportedUserAndCodeViewerPermission(() -> underTest.selectEntityUuidsByPermissionAndUserUuidAndQualifier(dbSession,
      UserRole.USER, Uuids.createFast(), PROJECT_QUALIFIER));
  }

  @Test
  void selectComponentUuidsByPermissionAndUserId_throws_IAR_if_permission_CODEVIEWER_is_specified() {
    expectUnsupportedUserAndCodeViewerPermission(() -> underTest.selectEntityUuidsByPermissionAndUserUuidAndQualifier(dbSession,
      UserRole.CODEVIEWER, Uuids.createFast(), PROJECT_QUALIFIER));
  }

  private void expectUnsupportedUserAndCodeViewerPermission(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Permissions [user, codeviewer] are not supported by selectEntityUuidsByPermissionAndUserUuidAndQualifier");
  }

  @Test
  void selectEntityIdsByPermissionAndUserUuid() {
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project2);
    // global permission - not returned
    db.users().insertGlobalPermissionOnUser(user1, ADMINISTER);
    // project permission on another user id - not returned
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, project1);
    // project permission on another permission - not returned
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, project1);

    List<String> entityUuids = underTest.selectEntityUuidsByPermissionAndUserUuidAndQualifier(dbSession, UserRole.ADMIN, user1.getUuid(),
      PROJECT_QUALIFIER);

    assertThat(entityUuids).containsExactly(project1.getUuid(), project2.getUuid());
  }

  @Test
  void selectComponentIdsByPermissionAndUserUuid_group_permissions() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertEntityPermissionOnGroup(group1, UserRole.ADMIN, project1);
    db.users().insertMember(group1, user1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project2);
    // global permission - not returned
    db.users().insertGlobalPermissionOnUser(user1, ADMINISTER);
    db.users().insertPermissionOnGroup(group1, ADMINISTER);
    // project permission on another user id - not returned
    db.users().insertPermissionOnGroup(group2, ADMINISTER);
    db.users().insertMember(group2, user2);
    // project permission on another permission - not returned
    db.users().insertEntityPermissionOnGroup(group1, UserRole.ISSUE_ADMIN, project1);

    List<String> result = underTest.selectEntityUuidsByPermissionAndUserUuidAndQualifier(dbSession, UserRole.ADMIN, user1.getUuid(),
      PROJECT_QUALIFIER);

    assertThat(result).containsExactlyInAnyOrder(project1.getUuid(), project2.getUuid());
  }

  @Test
  void delete_all_group_permissions_by_group_uuid() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.users().insertPermissionOnGroup(group1, "admin");
    db.users().insertProjectPermissionOnGroup(group1, "profileadmin", project);
    db.users().insertPermissionOnGroup(group1, "gateadmin");
    db.users().insertPermissionOnGroup(group2, "gateadmin");
    db.users().insertProjectPermissionOnGroup(group2, "admin", project);
    db.users().insertPermissionOnAnyone("scan");
    db.users().insertPermissionOnAnyone("provisioning");

    underTest.deleteGroupRolesByGroupUuid(db.getSession(), group1.getUuid());
    db.getSession().commit();

    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), group1.getUuid())).isEmpty();
    assertThat(db.getDbClient().groupPermissionDao().selectEntityPermissionsOfGroup(db.getSession(), group1.getUuid(), project.uuid()))
      .isEmpty();
    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), group2.getUuid()))
      .containsOnly("gateadmin");
    assertThat(db.getDbClient().groupPermissionDao().selectEntityPermissionsOfGroup(db.getSession(), group2.getUuid(), project.uuid()))
      .containsOnly("admin");
    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), null)).containsOnly("scan",
      "provisioning");
  }

  // TODO : add test for qualifier method.
}
