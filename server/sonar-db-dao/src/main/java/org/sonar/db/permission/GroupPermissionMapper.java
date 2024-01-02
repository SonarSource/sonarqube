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
import org.sonar.db.Pagination;

public interface GroupPermissionMapper {

  List<String> selectGroupNamesByQuery(@Param("query") PermissionQuery query, @Param("pagination") Pagination pagination);

  int countGroupsByQuery(@Param("query") PermissionQuery query);

  List<GroupPermissionDto> selectByGroupUuids(@Param("groupUuids") List<String> groupUuids, @Nullable @Param("entityUuid") String entityUuid);

  void groupsCountByEntityUuidAndPermission(Map<String, Object> parameters, ResultHandler<CountPerEntityPermission> resultHandler);

  List<String> selectProjectKeysWithAnyonePermissions(int max);

  int countEntitiesWithAnyonePermissions();

  void insert(GroupPermissionDto dto);

  int delete(@Param("permission") String permission, @Nullable @Param("groupUuid") String groupUuid, @Nullable @Param("entityUuid") String entityUuid);

  List<String> selectGlobalPermissionsOfGroup(@Nullable @Param("groupUuid") String groupUuid);

  List<String> selectEntityPermissionsOfGroup(@Nullable @Param("groupUuid") String groupUuid, @Param("entityUuid") String entityUuid);

  /**
   * Lists uuid of groups with at least one permission on the specified entity but which do not have the specified
   * permission, <strong>excluding group "AnyOne"</strong> (which implies the returned {@code Set} can't contain
   * {@code null}).
   */
  Set<String> selectGroupUuidsWithPermissionOnEntityBut(@Param("entityUuid") String entityUuid, @Param("role") String permission);

  Set<String> selectGroupUuidsWithPermissionOnEntity(@Param("entityUuid") String entityUuid, @Param("role") String permission);

  List<GroupPermissionDto> selectGroupPermissionsOnEntity(@Param("entityUuid") String entityUuid);

  int deleteByEntityUuid(@Param("entityUuid") String entityUuid);

  int deleteByEntityUuidAndGroupUuid(@Param("entityUuid") String entityUuid, @Nullable @Param("groupUuid") String groupUuid);

  int deleteByEntityUuidAndPermission(@Param("entityUuid") String entityUuid, @Param("permission") String permission);
}
