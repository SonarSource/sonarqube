/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public interface GroupPermissionMapper {

  List<String> selectGroupNamesByQuery(@Param("query") PermissionQuery query, RowBounds rowBounds);

  int countGroupsByQuery(@Param("query") PermissionQuery query);

  List<GroupPermissionDto> selectByGroupIds(@Param("organizationUuid") String organizationUuid,
    @Param("groupIds") List<Integer> groupIds, @Nullable @Param("projectUuid") String projectUuid);

  void groupsCountByProjectUuidAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  void insert(GroupPermissionDto dto);

  void delete(@Param("permission") String permission, @Param("organizationUuid") String organizationUuid,
    @Nullable @Param("groupId") Integer groupId, @Nullable @Param("rootComponentUuid") String rootComponentUuid);

  List<String> selectGlobalPermissionsOfGroup(@Param("organizationUuid") String organizationUuid,
    @Nullable @Param("groupId") Integer groupId);

  List<String> selectProjectPermissionsOfGroup(@Param("organizationUuid") String organizationUuid,
    @Nullable @Param("groupId") Integer groupId, @Param("projectUuid") String projectUuid);

  void selectAllPermissionsByGroupId(@Param("organizationUuid") String organizationUuid,
    @Param("groupId") Integer groupId, ResultHandler resultHandler);

  /**
   * Lists id of groups with at least one permission on the specified root component but which do not have the specified
   * permission, <strong>excluding group "AnyOne"</strong> (which implies the returned {@code Set} can't contain
   * {@code null}).
   */
  Set<Integer> selectGroupIdsWithPermissionOnProjectBut(@Param("projectUuid") String projectUuid, @Param("role") String permission);

  void deleteByOrganization(@Param("organizationUuid") String organizationUuid);

  void deleteByRootComponentUuid(@Param("rootComponentUuid") String rootComponentUuid);

  int deleteByRootComponentUuidAndGroupId(@Param("rootComponentUuid") String rootComponentUuid, @Nullable @Param("groupId") Integer groupId);

  int deleteByRootComponentUuidAndPermission(@Param("rootComponentUuid") String rootComponentUuid, @Param("permission") String permission);
}
