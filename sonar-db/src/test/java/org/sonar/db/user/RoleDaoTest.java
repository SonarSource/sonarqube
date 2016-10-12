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
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GroupPermissionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;

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
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
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
  public void retrieve_global_group_permissions() {
    db.prepareDbUnit(getClass(), "globalGroupPermissions.xml");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "sonar-administrators", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN,
      GlobalPermissions.QUALITY_PROFILE_ADMIN,
      GlobalPermissions.QUALITY_GATE_ADMIN);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "sonar-users", null)).containsOnly(GlobalPermissions.QUALITY_GATE_ADMIN);
    assertThat(underTest.selectGroupPermissions(db.getSession(), DefaultGroups.ANYONE, null)).containsOnly(GlobalPermissions.PROVISIONING,
      GlobalPermissions.SCAN_EXECUTION);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "anyone", null)).containsOnly(GlobalPermissions.PROVISIONING, GlobalPermissions.SCAN_EXECUTION);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "AnYoNe", null)).containsOnly(GlobalPermissions.PROVISIONING, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void retrieve_resource_group_permissions() {
    db.prepareDbUnit(getClass(), "resourceGroupPermissions.xml");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "sonar-administrators", 1L)).containsOnly(UserRole.ADMIN, UserRole.CODEVIEWER);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "sonar-users", 1L)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void delete_global_group_permission() {
    db.prepareDbUnit(getClass(), "globalGroupPermissions.xml");

    GroupPermissionDto groupRoleToDelete = new GroupPermissionDto().setGroupId(100L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    underTest.deleteGroupRole(groupRoleToDelete, db.getSession());
    db.getSession().commit();

    db.assertDbUnit(getClass(), "globalGroupPermissions-result.xml", "group_roles");
  }

  @Test
  public void delete_resource_group_permission() {
    db.prepareDbUnit(getClass(), "resourceGroupPermissions.xml");

    GroupPermissionDto groupRoleToDelete = new GroupPermissionDto().setGroupId(100L).setRole(UserRole.CODEVIEWER).setResourceId(1L);

    underTest.deleteGroupRole(groupRoleToDelete, db.getSession());
    db.getSession().commit();

    db.assertDbUnit(getClass(), "resourceGroupPermissions-result.xml", "group_roles");
  }

  @Test
  public void delete_all_group_permissions_by_group_id() {
    db.prepareDbUnit(getClass(), "deleteGroupPermissionsByGroupId.xml");

    underTest.deleteGroupRolesByGroupId(db.getSession(), 100L);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "deleteGroupPermissionsByGroupId-result.xml", "group_roles");
  }

  @Test
  public void should_count_component_permissions() {
    db.prepareDbUnit(getClass(), "should_count_component_permissions.xml");

    assertThat(underTest.countComponentPermissions(db.getSession(), 123L)).isEqualTo(2);
  }

  @Test
  public void should_remove_group_permissions_on_project() {
    db.prepareDbUnit(getClass(), "should_remove_all_permissions.xml");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "devs", 123L)).hasSize(1);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "other", 123L)).isEmpty();

    underTest.deleteGroupRolesByResourceId(db.getSession(), 123L);
    db.getSession().commit();

    db.assertDbUnitTable(getClass(), "should_remove_all_permissions-result.xml", "group_roles", "group_id", "resource_id", "role");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "devs", 123L)).isEmpty();
  }

  @Test
  public void countUserPermissions() {
    db.users().insertProjectPermissionOnUser(user1, GlobalPermissions.SYSTEM_ADMIN, project1);
    db.users().insertPermissionOnUser(user1, GlobalPermissions.SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(user1, GlobalPermissions.SCAN_EXECUTION);

    int result = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, null);

    assertThat(result).isEqualTo(1);
  }

  @Test
  public void countUserPermissions_counts_users_with_one_permission_when_the_last_one_is_in_a_group() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    db.users().insertMember(group1, user1);
    db.users().insertPermissionOnGroup(group1, GlobalPermissions.SYSTEM_ADMIN);

    int resultWithoutExcludingGroup = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, null);
    assertThat(resultWithoutExcludingGroup).isEqualTo(1);

    int resultWithGroupExclusion = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, group1.getId());
    assertThat(resultWithGroupExclusion).isEqualTo(0);
  }

  @Test
  public void countUserPermissions_counts_user_twice_when_both_user_and_group_permission() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    db.users().insertMember(group1, user1);
    db.users().insertPermissionOnGroup(group1, GlobalPermissions.SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(user1, GlobalPermissions.SYSTEM_ADMIN);

    int result = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, null);

    assertThat(result).isEqualTo(2);
  }
}
