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
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsIntoSet;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

/**
 * The SQL requests used to verify authorization (the permissions
 * granted to users)
 *
 * @see GroupPermissionDao for CRUD of table group_roles
 * @see UserPermissionDao for CRUD of table user_roles
 */
public class AuthorizationDao implements Dao {

  /**
   * Loads all the global permissions granted to user
   */
  public Set<String> selectGlobalPermissions(DbSession dbSession, String userUuid) {
    return mapper(dbSession).selectGlobalPermissions(userUuid);
  }

  /**
   * Loads all the permissions granted to anonymous user
   */
  public Set<String> selectGlobalPermissionsOfAnonymous(DbSession dbSession) {
    return mapper(dbSession).selectGlobalPermissionsOfAnonymous();
  }

  /**
   * Loads all the permissions granted to logged-in user for the specified entity <strong>stored in *_ROLES
   * tables</strong>.
   * An empty Set is returned if user has no permissions on the entity.
   *
   * <strong>This method does not support public components</strong>
   */
  public Set<String> selectEntityPermissions(DbSession dbSession, String entityUuid, String userUuid) {
    return mapper(dbSession).selectEntityPermissions(entityUuid, userUuid);
  }

  public Set<UserAndPermissionDto> selectEntityPermissionsObtainedViaManagedGroup(DbSession dbSession, String entityUuid, String managedInstanceProvider) {
    return mapper(dbSession).selectEntityPermissionsObtainedViaManagedGroup(entityUuid, managedInstanceProvider);
  }

  /**
   * Loads all the permissions granted to anonymous for the specified entity <strong>stored in *_ROLES
   * tables</strong>.
   * An empty Set is returned if anonymous user has no permissions on the entity.
   *
   * <strong>This method does not support public components</strong>
   */
  public Set<String> selectEntityPermissionsOfAnonymous(DbSession dbSession, String entityUuid) {
    return mapper(dbSession).selectEntityPermissionsOfAnonymous(entityUuid);
  }

  /**
   * The number of users who will still have the permission if the group {@code excludedGroupUuid}
   * is deleted. The anyone virtual group is not taken into account.
   */
  public int countUsersWithGlobalPermissionExcludingGroup(DbSession dbSession, String permission, String excludedGroupUuid) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingGroup(permission, excludedGroupUuid);
  }

  /**
   * The number of users who will still have the permission if the user {@code excludedUserId}
   * is deleted. The anyone virtual group is not taken into account.
   */
  public int countUsersWithGlobalPermissionExcludingUser(DbSession dbSession, String permission, String excludedUserUuid) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingUser(permission, excludedUserUuid);
  }

  /**
   * The list of users who have the global permission.
   * The anyone virtual group is not taken into account.
   */
  public List<String> selectUserUuidsWithGlobalPermission(DbSession dbSession, String permission) {
    return mapper(dbSession).selectUserUuidsWithGlobalPermission(permission);
  }

  /**
   * The number of users who will still have the permission if the user {@code userId}
   * is removed from group {@code groupUuid}. The anyone virtual group is not taken into account.
   * Contrary to {@link #countUsersWithGlobalPermissionExcludingUser(DbSession, String, String)}, user
   * still exists and may have the permission directly or through other groups.
   */
  public int countUsersWithGlobalPermissionExcludingGroupMember(DbSession dbSession, String permission, String groupUuid, String userUuid) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingGroupMember(permission, groupUuid, userUuid);
  }

  /**
   * The number of users who will still have the permission if the permission {@code permission}
   * is removed from user {@code userId}. The anyone virtual group is not taken into account.
   * Contrary to {@link #countUsersWithGlobalPermissionExcludingUser(DbSession, String, String)}, user
   * still exists and may have the permission through groups.
   */
  public int countUsersWithGlobalPermissionExcludingUserPermission(DbSession dbSession, String permission, String userUuid) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingUserPermission(permission, userUuid);
  }

  public Set<String> keepAuthorizedEntityUuids(DbSession dbSession, Collection<String> entityUuids, @Nullable String userUuid, String permission) {
    return executeLargeInputsIntoSet(
      entityUuids,
      partition -> {
        if (userUuid == null) {
          return mapper(dbSession).keepAuthorizedEntityUuidsForAnonymous(permission, partition);
        }
        return mapper(dbSession).keepAuthorizedEntityUuidsForUser(userUuid, permission, partition);
      },
      partitionSize -> partitionSize / 2);
  }

  /**
   * Keep only authorized user that have the given permission on a given entity.
   * Please Note that if the permission is 'Anyone' is NOT taking into account by this method.
   */
  public Collection<String> keepAuthorizedUsersForRoleAndEntity(DbSession dbSession, Collection<String> userUuids, String role, String entityUuid) {
    return executeLargeInputs(
      userUuids,
      partitionOfIds -> mapper(dbSession).keepAuthorizedUsersForRoleAndEntity(role, entityUuid, partitionOfIds),
      partitionSize -> partitionSize / 3);
  }

  public Set<EmailSubscriberDto> selectQualityProfileAdministratorLogins(DbSession dbSession) {
    return mapper(dbSession).selectEmailSubscribersWithGlobalPermission(ADMINISTER_QUALITY_PROFILES.getKey());
  }

  public Set<EmailSubscriberDto> selectGlobalAdministerEmailSubscribers(DbSession dbSession) {
    return mapper(dbSession).selectEmailSubscribersWithGlobalPermission(ADMINISTER.getKey());
  }

  public Set<String> keepAuthorizedLoginsOnEntity(DbSession dbSession, Set<String> logins, String entityKey, String permission) {
    return executeLargeInputsIntoSet(
      logins,
      partitionOfLogins -> mapper(dbSession).keepAuthorizedLoginsOnEntity(partitionOfLogins, entityKey, permission),
      partitionSize -> partitionSize / 3);
  }

  private static AuthorizationMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(AuthorizationMapper.class);
  }
}
