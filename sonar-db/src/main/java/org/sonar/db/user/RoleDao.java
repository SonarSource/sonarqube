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
import org.sonar.db.permission.GroupPermissionDto;

public class RoleDao implements Dao {

  /**
   * All the projects on which the user has {@code permission}, directly or through
   * groups.
   */
  public List<Long> selectComponentIdsByPermissionAndUserId(DbSession dbSession, String permission, long userId) {
    return mapper(dbSession).selectComponentIdsByPermissionAndUserId(permission, userId);
  }

  /**
   * @deprecated replaced by {@link org.sonar.db.permission.GroupPermissionDao#selectGroupPermissions(DbSession, long, Long)}
   * and {@link org.sonar.db.permission.GroupPermissionDao#selectAnyonePermissions(DbSession, Long)}
   */
  @Deprecated
  public List<String> selectGroupPermissions(DbSession session, String groupName, @Nullable Long resourceId) {
    return session.getMapper(RoleMapper.class).selectGroupPermissions(groupName, resourceId, DefaultGroups.isAnyone(groupName));
  }

  /**
   * @deprecated does not support organizations on anyone groups
   */
  @Deprecated
  public void deleteGroupRole(GroupPermissionDto groupRole, DbSession session) {
    mapper(session).deleteGroupRole(groupRole);
  }

  public void deleteGroupRolesByResourceId(DbSession session, long projectId) {
    mapper(session).deleteGroupRolesByResourceId(projectId);
  }

  private static int countResourceGroupRoles(DbSession session, Long resourceId) {
    return mapper(session).countResourceGroupRoles(resourceId);
  }

  private static int countResourceUserRoles(DbSession session, long resourceId) {
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

  private static RoleMapper mapper(DbSession session) {
    return session.getMapper(RoleMapper.class);
  }
}
