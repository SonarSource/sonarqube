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
import org.apache.ibatis.annotations.Param;
import org.sonar.db.permission.GroupPermissionDto;

/**
 * @since 3.2
 */
public interface RoleMapper {

  List<Long> selectComponentIdsByPermissionAndUserId(@Param("permission") String permission, @Param("userId") long userId);

  /**
   * @return permissions from to a group
   */
  List<String> selectGroupPermissions(@Param("groupName") String groupName, @Nullable @Param("resourceId") Long resourceId, @Param("isAnyOneGroup") Boolean isAnyOneGroup);

  void deleteGroupRole(GroupPermissionDto dto);

  void deleteGroupRolesByResourceId(long projectId);

  int countResourceGroupRoles(Long resourceId);

  int countResourceUserRoles(long resourceId);

  void deleteGroupRolesByGroupId(long groupId);

  int countUsersWithPermission(@Param("permission") String permission, @Nullable @Param("groupId") Long groupId);
}
