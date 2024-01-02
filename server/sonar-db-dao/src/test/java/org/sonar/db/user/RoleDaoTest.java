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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;

public class RoleDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();
  private RoleDao underTest = db.getDbClient().roleDao();

  private UserDto user1;
  private UserDto user2;
  private ComponentDto project1;
  private ComponentDto project2;

  @Before
  public void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    project1 = db.components().insertPrivateProject();
    project2 = db.components().insertPrivateProject();
  }

  @Test
  public void selectComponentUuidsByPermissionAndUserId_throws_IAR_if_permission_USER_is_specified() {
    expectUnsupportedUserAndCodeViewerPermission(() -> underTest.selectComponentUuidsByPermissionAndUserUuid(dbSession, UserRole.USER, Uuids.createFast()));
  }

  @Test
  public void selectComponentUuidsByPermissionAndUserId_throws_IAR_if_permission_CODEVIEWER_is_specified() {
    expectUnsupportedUserAndCodeViewerPermission(() -> underTest.selectComponentUuidsByPermissionAndUserUuid(dbSession, UserRole.CODEVIEWER, Uuids.createFast()));
  }

  private void expectUnsupportedUserAndCodeViewerPermission(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Permissions [user, codeviewer] are not supported by selectComponentUuidsByPermissionAndUserUuid");
  }

  @Test
  public void selectComponentIdsByPermissionAndUserUuid() {
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project2);
    // global permission - not returned
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    // project permission on another user id - not returned
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, project1);
    // project permission on another permission - not returned
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, project1);

    List<String> projectUuids = underTest.selectComponentUuidsByPermissionAndUserUuid(dbSession, UserRole.ADMIN, user1.getUuid());

    assertThat(projectUuids).containsExactly(project1.uuid(), project2.uuid());
  }

  @Test
  public void selectComponentIdsByPermissionAndUserUuid_group_permissions() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group1, UserRole.ADMIN, project1);
    db.users().insertMember(group1, user1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project2);
    // global permission - not returned
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    db.users().insertPermissionOnGroup(group1, ADMINISTER);
    // project permission on another user id - not returned
    db.users().insertPermissionOnGroup(group2, ADMINISTER);
    db.users().insertMember(group2, user2);
    // project permission on another permission - not returned
    db.users().insertProjectPermissionOnGroup(group1, UserRole.ISSUE_ADMIN, project1);

    List<String> result = underTest.selectComponentUuidsByPermissionAndUserUuid(dbSession, UserRole.ADMIN, user1.getUuid());

    assertThat(result).containsExactly(project1.uuid(), project2.uuid());
  }

  @Test
  public void delete_all_group_permissions_by_group_uuid() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
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
    assertThat(db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), group1.getUuid(), project.uuid()))
      .isEmpty();
    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), group2.getUuid()))
      .containsOnly("gateadmin");
    assertThat(db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), group2.getUuid(), project.uuid()))
      .containsOnly("admin");
    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), null)).containsOnly("scan",
      "provisioning");
  }
}
