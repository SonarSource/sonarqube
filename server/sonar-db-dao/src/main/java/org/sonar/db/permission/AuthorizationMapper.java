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

/**
 * @see AuthorizationDao
 */
public interface AuthorizationMapper {

  Set<String> selectOrganizationPermissions(@Param("organizationUuid") String organizationUuid, @Param("userId") int userId);

  Set<String> selectOrganizationPermissionsOfAnonymous(@Param("organizationUuid") String organizationUuid);

  int countUsersWithGlobalPermissionExcludingGroup(@Param("organizationUuid") String organizationUuid,
    @Param("permission") String permission, @Param("excludedGroupId") int excludedGroupId);

  int countUsersWithGlobalPermissionExcludingUser(@Param("organizationUuid") String organizationUuid, @Param("permission") String permission,
    @Param("excludedUserId") int excludedUserId);

  int countUsersWithGlobalPermissionExcludingGroupMember(@Param("organizationUuid") String organizationUuid,
    @Param("permission") String permission, @Param("groupId") int groupId, @Param("userId") int userId);

  int countUsersWithGlobalPermissionExcludingUserPermission(@Param("organizationUuid") String organizationUuid,
    @Param("permission") String permission, @Param("userId") int userId);

  Set<String> selectOrganizationUuidsOfUserWithGlobalPermission(@Param("userId") int userId, @Param("permission") String permission);

  Set<Long> keepAuthorizedProjectIdsForAnonymous(@Param("role") String role, @Param("componentIds") Collection<Long> componentIds);

  Set<Long> keepAuthorizedProjectIdsForUser(@Param("userId") int userId, @Param("role") String role, @Param("componentIds") Collection<Long> componentIds);

  List<Integer> keepAuthorizedUsersForRoleAndProject(@Param("role") String role, @Param("componentId") long componentId, @Param("userIds") List<Integer> userIds);

  Set<String> keepAuthorizedProjectUuidsForUser(@Param("userId") int userId, @Param("permission") String permission, @Param("projectUuids") Collection<String> projectUuids);

  Set<String> keepAuthorizedProjectUuidsForAnonymous(@Param("permission") String permission, @Param("projectUuids") Collection<String> projectUuids);

  Set<String> selectProjectPermissions(@Param("projectUuid") String projectUuid, @Param("userId") long userId);

  Set<String> selectProjectPermissionsOfAnonymous(@Param("projectUuid") String projectUuid);

  List<String> selectQualityProfileAdministratorLogins(@Param("permission") String permission);

  Set<String> keepAuthorizedLoginsOnProject(@Param("logins") List<String> logins, @Param("projectKey") String projectKey, @Param("permission") String permission);

  List<String> selectLoginsWithGlobalPermission(@Param("permission") String permission);
}
