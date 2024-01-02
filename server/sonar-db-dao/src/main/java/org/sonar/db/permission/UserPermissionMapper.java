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
package org.sonar.db.permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.user.UserIdDto;

public interface UserPermissionMapper {

  List<UserPermissionDto> selectUserPermissionsByQueryAndUserUuids(@Param("query") PermissionQuery query, @Param("userUuids") Collection<String> userUuids);

  List<String> selectUserUuidsByQuery(@Param("query") PermissionQuery query);

  /**
   * Fetch user ids based on permission query and only in a specific scope (global permissions only or project permissions only)
   */
  List<String> selectUserUuidsByQueryAndScope(@Param("query") PermissionQuery query);

  /**
   * Count the number of distinct users returned by {@link #selectUserUuidsByQuery(PermissionQuery)}
   * {@link PermissionQuery#getPageOffset()} and {@link PermissionQuery#getPageSize()} are ignored.
   */
  int countUsersByQuery(@Param("query") PermissionQuery query);

  /**
   * Count the number of users per permission for a given list of projects.
   * @param projectUuids a non-null and non-empty list of project ids
   */
  List<CountPerProjectPermission> countUsersByProjectPermission(@Param("projectUuids") List<String> projectUuids);

  /**
   * select id of users with at least one permission on the specified project but which do not have the specified permission.
   */
  Set<UserIdDto> selectUserIdsWithPermissionOnProjectBut(@Param("projectUuid") String projectUuid, @Param("permission") String permission);

  void insert(@Param("dto")UserPermissionDto dto);

  int deleteGlobalPermission(@Param("userUuid") String userUuid, @Param("permission") String permission);

  int deleteProjectPermission(@Param("userUuid") String userUuid, @Param("permission") String permission,
    @Param("projectUuid") String projectUuid);

  int deleteProjectPermissions(@Param("projectUuid") String projectUuid);

  int deleteProjectPermissionOfAnyUser(@Param("projectUuid") String projectUuid, @Param("permission") String permission);

  List<String> selectGlobalPermissionsOfUser(@Param("userUuid") String userUuid);

  List<String> selectProjectPermissionsOfUser(@Param("userUuid") String userUuid, @Param("projectUuid") String projectUuid);

  int deleteByUserUuid(@Param("userUuid") String userUuid);
}
