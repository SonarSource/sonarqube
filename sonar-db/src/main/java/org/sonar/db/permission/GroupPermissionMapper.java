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
package org.sonar.db.permission;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public interface GroupPermissionMapper {

  /**
   * @deprecated does not support organizations
   */
  @Deprecated
  int countGroups(Map<String, Object> parameters);

  /**
   * @deprecated does not support organizations
   */
  @Deprecated
  List<String> selectGroupNamesByPermissionQuery(@Param("query") PermissionQuery query, RowBounds rowBounds);

  /**
   * @deprecated does not support organizations
   */
  @Deprecated
  int countGroupsByPermissionQuery(@Param("query") PermissionQuery query);

  /**
   * @deprecated does not support organizations
   */
  @Deprecated
  List<GroupPermissionDto> selectGroupPermissionByGroupNames(@Param("groupNames") List<String> groupNames, @Nullable @Param("projectId") Long projectId);

  void groupsCountByProjectIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  void insert(GroupPermissionDto dto);

  void deleteByRootComponentId(@Param("rootComponentId") long componentId);

  void delete(@Param("permission") String permission, @Param("organizationUuid") String organizationUuid,
    @Nullable @Param("groupId") Long groupId, @Nullable @Param("rootComponentId") Long rootComponentId);

  int countRowsByRootComponentId(@Param("rootComponentId") long rootComponentId);

  List<String> selectGlobalPermissionsOfGroup(@Param("organizationUuid") String organizationUuid, @Nullable @Param("groupId") Long groupId);

  List<String> selectProjectPermissionsOfGroup(@Param("organizationUuid") String organizationUuid, @Nullable @Param("groupId") Long groupId, @Param("projectId") long projectId);

  void deleteByOrganization(@Param("organizationUuid") String organizationUuid);

}
