/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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

/**
 * @since 3.2
 */
public interface RoleMapper {

  /**
   * @return permissions from a user
   */
  List<String> selectUserPermissions(@Param("userLogin") String userLogin, @Nullable @Param("resourceId") Long resourceId);

  /**
   * @return permissions from to a group
   */
  List<String> selectGroupPermissions(@Param("groupName") String groupName, @Nullable @Param("resourceId") Long resourceId, @Param("isAnyOneGroup") Boolean isAnyOneGroup);

  void insertGroupRole(GroupRoleDto groupRole);

  void insertUserRole(UserRoleDto userRole);

  void deleteUserRole(UserRoleDto userRole);

  void deleteGroupRole(GroupRoleDto groupRole);

  void deleteGroupRolesByResourceId(Long resourceId);

  void deleteUserRolesByResourceId(Long resourceId);

  int countResourceGroupRoles(Long resourceId);

  int countResourceUserRoles(Long resourceId);

  void deleteGroupRolesByGroupId(long groupId);

  int countUsersWithPermission(@Param("permission") String permission, @Nullable @Param("groupId") Long groupId);
}
