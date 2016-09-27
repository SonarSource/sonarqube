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
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class RoleDao implements Dao {

  /**
   * @deprecated replaced by {@link org.sonar.db.permission.UserPermissionDao#selectUserPermissions(DbSession, String, String)}
   */
  @Deprecated
  public List<String> selectUserPermissions(DbSession session, String userLogin, @Nullable Long resourceId) {
    return mapper(session).selectUserPermissions(userLogin, resourceId);
  }

  // TODO to be moved to PermissionVerifierDao
  public List<Long> selectComponentIdsByPermissionAndUserId(DbSession dbSession, String permission, long userId) {
    return mapper(dbSession).selectComponentIdsByPermissionAndUserId(permission, userId);
  }

  public List<String> selectGroupPermissions(DbSession session, String groupName, @Nullable Long resourceId) {
    return session.getMapper(RoleMapper.class).selectGroupPermissions(groupName, resourceId, DefaultGroups.isAnyone(groupName));
  }

  public void insertGroupRole(DbSession session, GroupRoleDto groupRole) {
    mapper(session).insertGroupRole(groupRole);
  }

  /**
   * @deprecated replaced by {@link org.sonar.db.permission.UserPermissionDao#insert(DbSession, org.sonar.db.permission.UserPermissionDto)}
   */
  @Deprecated
  public void insertUserRole(DbSession session, UserPermissionDto userRole) {
    mapper(session).insertUserRole(userRole);
  }

  /**
   * @deprecated  replaced by {@link org.sonar.db.permission.UserPermissionDao#delete(DbSession, String, String, String)}
   */
  @Deprecated
  public void deleteUserRole(UserPermissionDto userRole, DbSession session) {
    mapper(session).deleteUserRole(userRole);
  }

  public void deleteGroupRole(GroupRoleDto groupRole, DbSession session) {
    mapper(session).deleteGroupRole(groupRole);
  }

  private void deleteGroupRolesByResourceId(DbSession session, Long resourceId) {
    mapper(session).deleteGroupRolesByResourceId(resourceId);
  }

  private void deleteUserRolesByResourceId(DbSession session, Long resourceId) {
    mapper(session).deleteUserRolesByResourceId(resourceId);
  }

  private int countResourceGroupRoles(DbSession session, Long resourceId) {
    return mapper(session).countResourceGroupRoles(resourceId);
  }

  private int countResourceUserRoles(DbSession session, Long resourceId) {
    return mapper(session).countResourceUserRoles(resourceId);
  }

  public void deleteGroupRolesByGroupId(DbSession session, long groupId) {
    mapper(session).deleteGroupRolesByGroupId(groupId);
  }

  public int countComponentPermissions(DbSession session, Long componentId) {
    return countResourceGroupRoles(session, componentId) + countResourceUserRoles(session, componentId);
  }

  public int countUserPermissions(DbSession session, String permission, @Nullable Long allGroupsExceptThisGroupId) {
    return mapper(session).countUsersWithPermission(permission, allGroupsExceptThisGroupId);
  }

  public void removeAllPermissions(DbSession session, Long resourceId) {
    deleteGroupRolesByResourceId(session, resourceId);
    deleteUserRolesByResourceId(session, resourceId);
  }

  private static RoleMapper mapper(DbSession session) {
    return session.getMapper(RoleMapper.class);
  }
}
