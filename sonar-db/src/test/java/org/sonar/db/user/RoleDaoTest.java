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
package org.sonar.db.user;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;

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
  public void setUp() throws Exception {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    ComponentDbTester componentDbTester = new ComponentDbTester(db);
    project1 = componentDbTester.insertProject();
    project2 = componentDbTester.insertProject();
  }

  @Test
  public void selectComponentIdsByPermissionAndUserId() {
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project2);
    // global permission - not returned
    db.users().insertPermissionOnUser(user1, UserRole.ADMIN);
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
    db.users().insertPermissionOnUser(user1, GlobalPermissions.SYSTEM_ADMIN);
    db.users().insertPermissionOnGroup(group1, GlobalPermissions.SYSTEM_ADMIN);
    // project permission on another user id - not returned
    db.users().insertPermissionOnGroup(group2, GlobalPermissions.SYSTEM_ADMIN);
    db.users().insertMember(group2, user2);
    // project permission on another permission - not returned
    db.users().insertProjectPermissionOnGroup(group1, UserRole.ISSUE_ADMIN, project1);

    List<Long> result = underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.ADMIN, user1.getId());

    assertThat(result).containsExactly(project1.getId(), project2.getId());
  }

  @Test
  public void delete_all_group_permissions_by_group_id() {
    db.prepareDbUnit(getClass(), "deleteGroupPermissionsByGroupId.xml");

    underTest.deleteGroupRolesByGroupId(db.getSession(), 100L);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "deleteGroupPermissionsByGroupId-result.xml", "group_roles");
  }
}
