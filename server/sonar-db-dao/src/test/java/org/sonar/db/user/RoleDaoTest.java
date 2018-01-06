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
package org.sonar.db.user;

import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class RoleDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = db.getSession();
  private RoleDao underTest = db.getDbClient().roleDao();

  private UserDto user1;
  private UserDto user2;
  private ComponentDto project1;
  private ComponentDto project2;

  @Before
  public void setUp() throws Exception {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    project1 = db.components().insertPrivateProject();
    project2 = db.components().insertPrivateProject();
  }

  @Test
  public void selectComponentIdsByPermissionAndUserId_throws_IAR_if_permission_USER_is_specified() {
    expectUnsupportedUserAndCodeViewerPermission();

    underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.USER, new Random().nextInt(55));
  }

  @Test
  public void selectComponentIdsByPermissionAndUserId_throws_IAR_if_permission_CODEVIEWER_is_specified() {
    expectUnsupportedUserAndCodeViewerPermission();

    underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.CODEVIEWER, new Random().nextInt(55));
  }

  private void expectUnsupportedUserAndCodeViewerPermission() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Permissions [user, codeviewer] are not supported by selectComponentIdsByPermissionAndUserId");
  }

  @Test
  public void selectComponentIdsByPermissionAndUserId() {
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project2);
    // global permission - not returned
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    // project permission on another user id - not returned
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, project1);
    // project permission on another permission - not returned
    db.users().insertProjectPermissionOnUser(user1, UserRole.ISSUE_ADMIN, project1);

    List<Long> projectIds = underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.ADMIN, user1.getId());

    assertThat(projectIds).containsExactly(project1.getId(), project2.getId());
  }

  @Test
  public void selectComponentIdsByPermissionAndUserId_group_permissions() {
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

    List<Long> result = underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.ADMIN, user1.getId());

    assertThat(result).containsExactly(project1.getId(), project2.getId());
  }

  @Test
  public void delete_all_group_permissions_by_group_id() {
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnGroup(group1, "admin");
    db.users().insertProjectPermissionOnGroup(group1, "profileadmin", project);
    db.users().insertPermissionOnGroup(group1, "gateadmin");
    db.users().insertPermissionOnGroup(group2, "gateadmin");
    db.users().insertProjectPermissionOnGroup(group2, "admin", project);
    db.users().insertPermissionOnAnyone(db.getDefaultOrganization(), "scan");
    db.users().insertPermissionOnAnyone(db.getDefaultOrganization(), "provisioning");

    underTest.deleteGroupRolesByGroupId(db.getSession(), group1.getId());
    db.getSession().commit();

    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), db.getDefaultOrganization().getUuid(), group1.getId())).isEmpty();
    assertThat(db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), db.getDefaultOrganization().getUuid(), group1.getId(), project.getId())).isEmpty();
    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), db.getDefaultOrganization().getUuid(), group2.getId())).containsOnly("gateadmin");
    assertThat(db.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(db.getSession(), db.getDefaultOrganization().getUuid(), group2.getId(), project.getId())).containsOnly("admin");
    assertThat(db.getDbClient().groupPermissionDao().selectGlobalPermissionsOfGroup(db.getSession(), db.getDefaultOrganization().getUuid(), null)).containsOnly("scan", "provisioning");
  }
}
