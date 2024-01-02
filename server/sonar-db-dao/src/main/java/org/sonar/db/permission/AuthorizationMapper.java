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
import org.sonar.db.EmailSubscriberDto;

/**
 * @see AuthorizationDao
 */
public interface AuthorizationMapper {

  Set<String> selectGlobalPermissions(@Param("userUuid") String userUuid);

  Set<String> selectGlobalPermissionsOfAnonymous();

  int countUsersWithGlobalPermissionExcludingGroup(@Param("permission") String permission, @Param("excludedGroupUuid") String excludedGroupUuid);

  int countUsersWithGlobalPermissionExcludingUser(@Param("permission") String permission,
    @Param("excludedUserUuid") String excludedUserUuid);

  List<String> selectUserUuidsWithGlobalPermission(@Param("permission") String permission);

  int countUsersWithGlobalPermissionExcludingGroupMember(@Param("permission") String permission, @Param("groupUuid") String groupUuid, @Param("userUuid") String userUuid);

  int countUsersWithGlobalPermissionExcludingUserPermission(@Param("permission") String permission, @Param("userUuid") String userUuid);

  List<String> keepAuthorizedUsersForRoleAndEntity(@Param("role") String role, @Param("entityUuid") String entityUuid, @Param("userUuids") List<String> userUuids);

  Set<String> keepAuthorizedEntityUuidsForUser(@Param("userUuid") String userUuid, @Param("role") String role, @Param("entityUuids") Collection<String> entityUuids);

  Set<String> keepAuthorizedEntityUuidsForAnonymous(@Param("role") String role, @Param("entityUuids") Collection<String> entityUuids);

  Set<String> selectEntityPermissions(@Param("entityUuid") String entityUuid, @Param("userUuid") String userUuid);

  Set<UserAndPermissionDto> selectEntityPermissionsObtainedViaManagedGroup(
    @Param("entityUuid") String entityUuid,
    @Param("managedInstanceProvider") String managedInstanceProvider
  );

  Set<String> selectEntityPermissionsOfAnonymous(@Param("entityUuid") String entityUuid);

  Set<String> keepAuthorizedLoginsOnEntity(@Param("logins") List<String> logins, @Param("entityKey") String projectKey, @Param("permission") String permission);

  Set<EmailSubscriberDto> selectEmailSubscribersWithGlobalPermission(@Param("permission") String permission);
}
