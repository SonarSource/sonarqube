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
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.UserPermissionDto;

public interface PermissionMapper {

  List<UserWithPermissionDto> selectUsers(Map<String, Object> parameters, RowBounds rowBounds);

  int countUsers(Map<String, Object> parameters);

  List<String> selectLoginsByPermissionQuery(@Param("query") PermissionQuery query, RowBounds rowBounds);

  int countUsersByPermissionQuery(@Param("query") PermissionQuery query);

  List<UserPermissionDto> selectUserPermissionsByLogins(@Param("logins") List<String> logins, @Nullable @Param("projectId") Long projectId);

  List<GroupWithPermissionDto> selectGroups(Map<String, Object> parameters);

  int countGroups(Map<String, Object> parameters);

  List<String> selectGroupNamesByPermissionQuery(@Param("query") PermissionQuery query, RowBounds rowBounds);

  int countGroupsByPermissionQuery(@Param("query") PermissionQuery query);

  List<GroupRoleDto> selectGroupPermissionByGroupNames(@Param("groupNames") List<String> groupNames, @Nullable @Param("projectId") Long projectId);

  void usersCountByProjectIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  void groupsCountByProjectIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);
}
