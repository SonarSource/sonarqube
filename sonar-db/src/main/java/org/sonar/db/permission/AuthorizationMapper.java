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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

/**
 * @see AuthorizationDao
 */
public interface AuthorizationMapper {

  Set<String> selectOrganizationPermissions(@Param("organizationUuid") String organizationUuid, @Param("userId") long userId);

  Set<String> selectOrganizationPermissionsOfAnonymous(@Param("organizationUuid") String organizationUuid);

  Set<String> selectRootComponentPermissions(@Param("rootComponentId") long rootComponentId, @Param("userId") long userId);

  Set<String> selectRootComponentPermissionsOfAnonymous(@Param("rootComponentId") long rootComponentId);

  int countUsersWithGlobalPermissionExcludingGroup(@Param("organizationUuid") String organizationUuid,
    @Param("permission") String permission, @Param("excludedGroupId") long excludedGroupId);

  int countUsersWithGlobalPermissionExcludingUser(@Param("organizationUuid") String organizationUuid, @Param("permission") String permission,
    @Param("excludedUserId") long excludedUserId);

  int countUsersWithGlobalPermissionExcludingGroupMember(@Param("organizationUuid") String organizationUuid,
                                                         @Param("permission") String permission, @Param("groupId") long groupId, @Param("userId") long userId);

  int countUsersWithGlobalPermissionExcludingUserPermission(@Param("organizationUuid") String organizationUuid,
                                                            @Param("permission") String permission, @Param("userId") long userId);

  Set<String> selectOrganizationUuidsOfUserWithGlobalPermission(@Param("userId") long userId, @Param("permission") String permission);

  Set<Long> keepAuthorizedProjectIdsForAnonymous(@Param("role") String role, @Param("componentIds") Collection<Long> componentIds);
  
  Set<Long> keepAuthorizedProjectIdsForUser(@Param("userId") long userId, @Param("role") String role, @Param("componentIds") Collection<Long> componentIds);

  List<String> keepAuthorizedComponentKeysForAnonymous(@Param("role") String role, @Param("componentKeys") Collection<String> componentKeys);

  List<String> keepAuthorizedComponentKeysForUser(@Param("userId") Integer userId, @Param("role") String role, @Param("componentKeys") Collection<String> componentKeys);

  List<Long> keepAuthorizedUsersForRoleAndProject(@Param("role") String role, @Param("componentId") long componentId, @Param("userIds") List<Long> userIds);


}
