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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  GroupDbTester groupDb = new GroupDbTester(db);
  DbSession dbSession = db.getSession();

  RoleDao underTest = db.getDbClient().roleDao();

  @Test
  public void retrieve_global_user_permissions() {
    db.prepareDbUnit(getClass(), "globalUserPermissions.xml");

    assertThat(underTest.selectUserPermissions(db.getSession(), "admin_user", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    assertThat(underTest.selectUserPermissions(db.getSession(), "profile_admin_user", null)).containsOnly(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void retrieve_resource_user_permissions() {
    db.prepareDbUnit(getClass(), "resourceUserPermissions.xml");

    assertThat(underTest.selectUserPermissions(db.getSession(), "admin_user", 1L)).containsOnly(UserRole.ADMIN, UserRole.USER);
    assertThat(underTest.selectUserPermissions(db.getSession(), "browse_admin_user", 1L)).containsOnly(UserRole.USER);
  }

  @Test
  public void select_user_permissions_by_permission_and_user_id() {
    underTest.insertUserRole(dbSession, new UserPermissionDto().setPermission(UserRole.ADMIN).setUserId(1L).setComponentId(2L));
    underTest.insertUserRole(dbSession, new UserPermissionDto().setPermission(UserRole.ADMIN).setUserId(1L).setComponentId(3L));
    // global permission - not returned
    underTest.insertUserRole(dbSession, new UserPermissionDto().setPermission(UserRole.ADMIN).setUserId(1L).setComponentId(null));
    // project permission on another user id - not returned
    underTest.insertUserRole(dbSession, new UserPermissionDto().setPermission(UserRole.ADMIN).setUserId(42L).setComponentId(2L));
    // project permission on another permission - not returned
    underTest.insertUserRole(dbSession, new UserPermissionDto().setPermission(GlobalPermissions.SCAN_EXECUTION).setUserId(1L).setComponentId(2L));
    db.commit();

    List<Long> result = underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.ADMIN, 1L);

    assertThat(result).hasSize(2).containsExactly(2L, 3L);
  }

  @Test
  public void select_group_permissions_by_permission_and_user_id() {
    long userId = 11L;

    underTest.insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.ADMIN).setGroupId(1L).setResourceId(2L));
    groupDb.addUserToGroup(userId, 1L);
    underTest.insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.ADMIN).setGroupId(2L).setResourceId(3L));
    groupDb.addUserToGroup(userId, 2L);
    // global permission - not returned
    groupDb.addUserToGroup(userId, 3L);
    underTest.insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.ADMIN).setGroupId(3L).setResourceId(null));
    // project permission on another user id - not returned
    underTest.insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.ADMIN).setGroupId(4L).setResourceId(4L));
    groupDb.addUserToGroup(12L, 4L);
    // project permission on another permission - not returned
    underTest.insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.USER).setGroupId(5L).setResourceId(5L));
    groupDb.addUserToGroup(userId, 5L);
    // duplicates on resource id - should be returned once
    underTest.insertUserRole(dbSession, new UserPermissionDto().setPermission(UserRole.ADMIN).setUserId(userId).setComponentId(2L));
    underTest.insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.ADMIN).setGroupId(3L).setResourceId(3L));
    db.commit();

    List<Long> result = underTest.selectComponentIdsByPermissionAndUserId(dbSession, UserRole.ADMIN, userId);

    assertThat(result).hasSize(2).containsExactly(2L, 3L);
  }

  @Test
  public void retrieve_global_group_permissions() {
    db.prepareDbUnit(getClass(), "globalGroupPermissions.xml");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "sonar-administrators", null)).containsOnly(GlobalPermissions.SYSTEM_ADMIN,
      GlobalPermissions.QUALITY_PROFILE_ADMIN,
      GlobalPermissions.DASHBOARD_SHARING);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "sonar-users", null)).containsOnly(GlobalPermissions.DASHBOARD_SHARING);
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
  public void delete_global_user_permission() {
    db.prepareDbUnit(getClass(), "globalUserPermissions.xml");

    UserPermissionDto userRoleToDelete = new UserPermissionDto().setUserId(200L).setPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    underTest.deleteUserRole(userRoleToDelete, db.getSession());
    db.getSession().commit();

    db.assertDbUnit(getClass(), "globalUserPermissions-result.xml", "user_roles");
  }

  @Test
  public void delete_resource_user_permission() {
    db.prepareDbUnit(getClass(), "resourceUserPermissions.xml");

    UserPermissionDto userRoleToDelete = new UserPermissionDto().setUserId(200L).setPermission(UserRole.USER).setComponentId(1L);

    underTest.deleteUserRole(userRoleToDelete, db.getSession());
    db.getSession().commit();

    db.assertDbUnit(getClass(), "resourceUserPermissions-result.xml", "user_roles");
  }

  @Test
  public void delete_global_group_permission() {
    db.prepareDbUnit(getClass(), "globalGroupPermissions.xml");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    underTest.deleteGroupRole(groupRoleToDelete, db.getSession());
    db.getSession().commit();

    db.assertDbUnit(getClass(), "globalGroupPermissions-result.xml", "group_roles");
  }

  @Test
  public void delete_resource_group_permission() {
    db.prepareDbUnit(getClass(), "resourceGroupPermissions.xml");

    GroupRoleDto groupRoleToDelete = new GroupRoleDto().setGroupId(100L).setRole(UserRole.CODEVIEWER).setResourceId(1L);

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
  public void should_remove_all_permissions() {
    db.prepareDbUnit(getClass(), "should_remove_all_permissions.xml");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "devs", 123L)).hasSize(1);
    assertThat(underTest.selectGroupPermissions(db.getSession(), "other", 123L)).isEmpty();
    assertThat(underTest.selectUserPermissions(db.getSession(), "dave.loper", 123L)).hasSize(1);
    assertThat(underTest.selectUserPermissions(db.getSession(), "other.user", 123L)).isEmpty();

    underTest.removeAllPermissions(db.getSession(), 123L);
    db.getSession().commit();

    db.assertDbUnitTable(getClass(), "should_remove_all_permissions-result.xml", "group_roles", "group_id", "resource_id", "role");
    db.assertDbUnitTable(getClass(), "should_remove_all_permissions-result.xml", "user_roles", "user_id", "resource_id", "role");

    assertThat(underTest.selectGroupPermissions(db.getSession(), "devs", 123L)).isEmpty();
    assertThat(underTest.selectUserPermissions(db.getSession(), "dave.loper", 123L)).isEmpty();
  }

  @Test
  public void count_users_with_one_specific_permission() {
    DbClient dbClient = db.getDbClient();
    UserDto user = dbClient.userDao().insert(db.getSession(), new UserDto().setActive(true));
    dbClient.roleDao().insertUserRole(db.getSession(), new UserPermissionDto()
      .setUserId(user.getId())
      .setComponentId(123L)
      .setPermission(GlobalPermissions.SYSTEM_ADMIN));
    dbClient.roleDao().insertUserRole(db.getSession(), new UserPermissionDto()
      .setUserId(user.getId())
      .setPermission(GlobalPermissions.SYSTEM_ADMIN));
    dbClient.roleDao().insertUserRole(db.getSession(), new UserPermissionDto()
      .setUserId(user.getId())
      .setPermission(GlobalPermissions.SCAN_EXECUTION));

    int result = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, null);

    assertThat(result).isEqualTo(1);
  }

  @Test
  public void count_users_with_one_permission_when_the_last_one_is_in_a_group() {
    DbClient dbClient = db.getDbClient();

    UserDto user = dbClient.userDao().insert(db.getSession(), new UserDto().setActive(true));
    GroupDto group = dbClient.groupDao().insert(db.getSession(), new GroupDto());
    dbClient.userGroupDao().insert(db.getSession(), new UserGroupDto()
      .setGroupId(group.getId())
      .setUserId(user.getId()));
    dbClient.roleDao().insertGroupRole(db.getSession(), new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(GlobalPermissions.SYSTEM_ADMIN));

    int resultWithoutExcludingGroup = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, null);
    int resultWithGroupExclusion = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, group.getId());

    assertThat(resultWithoutExcludingGroup).isEqualTo(1);
    assertThat(resultWithGroupExclusion).isEqualTo(0);
  }

  @Test
  public void count_user_twice_when_user_and_group_permission() {
    DbClient dbClient = db.getDbClient();

    UserDto user = dbClient.userDao().insert(db.getSession(), new UserDto().setActive(true));
    GroupDto group = dbClient.groupDao().insert(db.getSession(), new GroupDto());
    dbClient.userGroupDao().insert(db.getSession(), new UserGroupDto()
      .setGroupId(group.getId())
      .setUserId(user.getId()));
    dbClient.roleDao().insertGroupRole(db.getSession(), new GroupRoleDto()
      .setGroupId(group.getId())
      .setRole(GlobalPermissions.SYSTEM_ADMIN));
    dbClient.roleDao().insertUserRole(db.getSession(), new UserPermissionDto()
      .setUserId(user.getId())
      .setPermission(GlobalPermissions.SYSTEM_ADMIN));

    int result = underTest.countUserPermissions(db.getSession(), GlobalPermissions.SYSTEM_ADMIN, null);

    assertThat(result).isEqualTo(2);
  }
}
