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
package org.sonar.db.permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface UserPermissionMapper {

  List<UserPermissionDto> selectUserPermissionsByQueryAndUserIds(@Param("query") PermissionQuery query, @Param("userIds") Collection<Integer> userIds);

  List<Integer> selectUserIdsByQuery(@Param("query") PermissionQuery query);

  /**
   * Count the number of distinct users returned by {@link #selectUserIdsByQuery(PermissionQuery)}
   * {@link PermissionQuery#getPageOffset()} and {@link PermissionQuery#getPageSize()} are ignored.
   */
  int countUsersByQuery(@Param("query") PermissionQuery query);

  /**
   * Count the number of users per permission for a given list of projects.
   * @param projectIds a non-null and non-empty list of project ids
   */
  List<CountPerProjectPermission> countUsersByProjectPermission(@Param("projectIds") List<Long> projectIds);

  /**
   * select id of users with at least one permission on the specified project but which do not have the specified permission.
   */
  Set<Integer> selectUserIdsWithPermissionOnProjectBut(@Param("projectId") long projectId, @Param("permission") String permission);

  void insert(UserPermissionDto dto);

  void deleteGlobalPermission(@Param("userId") int userId, @Param("permission") String permission,
    @Param("organizationUuid") String organizationUuid);

  void deleteProjectPermission(@Param("userId") int userId, @Param("permission") String permission,
    @Param("projectId") long projectId);

  void deleteProjectPermissions(@Param("projectId") long projectId);

  int deleteProjectPermissionOfAnyUser(@Param("projectId") long projectId, @Param("permission") String permission);

  List<String> selectGlobalPermissionsOfUser(@Param("userId") int userId, @Param("organizationUuid") String organizationUuid);

  List<String> selectProjectPermissionsOfUser(@Param("userId") int userId, @Param("projectId") long projectId);

  void deleteByOrganization(@Param("organizationUuid") String organizationUuid);

  void deleteOrganizationMemberPermissions(@Param("organizationUuid") String organizationUuid, @Param("userId") int login);

  void deleteByUserId(@Param("userId") int userId);
}
