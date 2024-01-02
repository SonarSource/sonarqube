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

  List<GroupPermissionDto> selectByGroupUuids(@Param("groupUuids") List<String> groupUuids, @Nullable @Param("projectUuid") String projectUuid);

  void groupsCountByProjectUuidAndPermission(Map<String, Object> parameters, ResultHandler<CountPerProjectPermission> resultHandler);

  List<String> selectProjectKeysWithAnyonePermissions(int max);

  int countProjectsWithAnyonePermissions();

  void insert(GroupPermissionDto dto);

  int delete(@Param("permission") String permission, @Nullable @Param("groupUuid") String groupUuid, @Nullable @Param("rootComponentUuid") String rootComponentUuid);

  List<String> selectGlobalPermissionsOfGroup(@Nullable @Param("groupUuid") String groupUuid);

  List<String> selectProjectPermissionsOfGroup(@Nullable @Param("groupUuid") String groupUuid, @Param("projectUuid") String projectUuid);

  void selectAllPermissionsByGroupUuid(@Param("groupUuid") String groupUuid, ResultHandler<GroupPermissionDto> resultHandler);

  /**
   * Lists uuid of groups with at least one permission on the specified root component but which do not have the specified
   * permission, <strong>excluding group "AnyOne"</strong> (which implies the returned {@code Set} can't contain
   * {@code null}).
   */
  Set<String> selectGroupUuidsWithPermissionOnProjectBut(@Param("projectUuid") String projectUuid, @Param("role") String permission);

  int deleteByRootComponentUuid(@Param("rootComponentUuid") String rootComponentUuid);

  int deleteByRootComponentUuidAndGroupUuid(@Param("rootComponentUuid") String rootComponentUuid, @Nullable @Param("groupUuid") String groupUuid);

  int deleteByRootComponentUuidAndPermission(@Param("rootComponentUuid") String rootComponentUuid, @Param("permission") String permission);
}
