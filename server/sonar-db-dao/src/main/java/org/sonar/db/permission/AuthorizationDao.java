/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

/**
 * The SQL requests used to verify authorization (the permissions
 * granted to users)
 *
 * @see GroupPermissionDao for CRUD of table group_roles
 * @see UserPermissionDao for CRUD of table user_roles
 */
public class AuthorizationDao implements Dao {

  /**
  * Loads all the permissions granted to user for the specified organization
  */
  public Set<String> selectOrganizationPermissions(DbSession dbSession, String organizationUuid, int userId) {
    return mapper(dbSession).selectOrganizationPermissions(organizationUuid, userId);
  }

  /**
   * Loads all the permissions granted to anonymous user for the specified organization
   */
  public Set<String> selectOrganizationPermissionsOfAnonymous(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectOrganizationPermissionsOfAnonymous(organizationUuid);
  }

  /**
   * Loads all the permissions granted to logged-in user for the specified project <strong>stored in *_ROLES
   * tables</strong>.
   * An empty Set is returned if user has no permissions on the project.
   *
   * <strong>This method does not support public components</strong>
   */
  public Set<String> selectProjectPermissions(DbSession dbSession, String projectUuid, long userId) {
    return mapper(dbSession).selectProjectPermissions(projectUuid, userId);
  }

  /**
   * Loads all the permissions granted to anonymous for the specified project <strong>stored in *_ROLES
   * tables</strong>.
   * An empty Set is returned if anonymous user has no permissions on the project.
   *
   * <strong>This method does not support public components</strong>
   */
  public Set<String> selectProjectPermissionsOfAnonymous(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectProjectPermissionsOfAnonymous(projectUuid);
  }

  /**
   * The number of users who will still have the permission if the group {@code excludedGroupId}
   * is deleted. The anyone virtual group is not taken into account.
   */
  public int countUsersWithGlobalPermissionExcludingGroup(DbSession dbSession, String organizationUuid,
    String permission, int excludedGroupId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingGroup(organizationUuid, permission, excludedGroupId);
  }

  /**
   * The number of users who will still have the permission if the user {@code excludedUserId}
   * is deleted. The anyone virtual group is not taken into account.
   */
  public int countUsersWithGlobalPermissionExcludingUser(DbSession dbSession, String organizationUuid,
    String permission, int excludedUserId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingUser(organizationUuid, permission, excludedUserId);
  }

  /**
   * The list of users who have the global permission.
   * The anyone virtual group is not taken into account.
   */
  public List<Integer> selectUserIdsWithGlobalPermission(DbSession dbSession, String organizationUuid, String permission) {
    return mapper(dbSession).selectUserIdsWithGlobalPermission(organizationUuid, permission);
  }

  /**
   * The number of users who will still have the permission if the user {@code userId}
   * is removed from group {@code groupId}. The anyone virtual group is not taken into account.
   * Contrary to {@link #countUsersWithGlobalPermissionExcludingUser(DbSession, String, String, int)}, user
   * still exists and may have the permission directly or through other groups.
   */
  public int countUsersWithGlobalPermissionExcludingGroupMember(DbSession dbSession, String organizationUuid,
    String permission, int groupId, int userId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingGroupMember(organizationUuid, permission, groupId, userId);
  }

  /**
   * The number of users who will still have the permission if the permission {@code permission}
   * is removed from user {@code userId}. The anyone virtual group is not taken into account.
   * Contrary to {@link #countUsersWithGlobalPermissionExcludingUser(DbSession, String, String, int)}, user
   * still exists and may have the permission through groups.
   */
  public int countUsersWithGlobalPermissionExcludingUserPermission(DbSession dbSession, String organizationUuid,
    String permission, int userId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingUserPermission(organizationUuid, permission, userId);
  }

  /**
   * The UUIDs of all the organizations in which the specified user has the specified global permission. An empty
   * set is returned if user or permission do not exist. An empty set is also returned if the user is not involved
   * in any organization.
   * <br/>
   * Group membership is taken into account. Anonymous privileges are ignored.
   */
  public Set<String> selectOrganizationUuidsOfUserWithGlobalPermission(DbSession dbSession, int userId, String permission) {
    return mapper(dbSession).selectOrganizationUuidsOfUserWithGlobalPermission(userId, permission);
  }

  /**
   * @deprecated replaced by {@link #keepAuthorizedProjectUuids(DbSession, Collection, Integer, String)}
   */
  @Deprecated
  public Set<Long> keepAuthorizedProjectIds(DbSession dbSession, Collection<Long> componentIds, @Nullable Integer userId, String permission) {
    return executeLargeInputsIntoSet(
      componentIds,
      partition -> {
        if (userId == null) {
          return mapper(dbSession).keepAuthorizedProjectIdsForAnonymous(permission, partition);
        }
        return mapper(dbSession).keepAuthorizedProjectIdsForUser(userId, permission, partition);
      },
      partitionSize -> partitionSize / 2);
  }

  public Set<String> keepAuthorizedProjectUuids(DbSession dbSession, Collection<String> projectUuids, @Nullable Integer userId, String permission) {
    return executeLargeInputsIntoSet(
      projectUuids,
      partition -> {
        if (userId == null) {
          return mapper(dbSession).keepAuthorizedProjectUuidsForAnonymous(permission, partition);
        }
        return mapper(dbSession).keepAuthorizedProjectUuidsForUser(userId, permission, partition);
      },
      partitionSize -> partitionSize / 2);
  }

  /**
   * Keep only authorized user that have the given permission on a given project.
   * Please Note that if the permission is 'Anyone' is NOT taking into account by this method.
   */
  public Collection<Integer> keepAuthorizedUsersForRoleAndProject(DbSession dbSession, Collection<Integer> userIds, String role, long projectId) {
    return executeLargeInputs(
      userIds,
      partitionOfIds -> mapper(dbSession).keepAuthorizedUsersForRoleAndProject(role, projectId, partitionOfIds),
      partitionSize -> partitionSize / 3);
  }

  public Set<EmailSubscriberDto> selectQualityProfileAdministratorLogins(DbSession dbSession) {
    return mapper(dbSession).selectEmailSubscribersWithGlobalPermission(ADMINISTER_QUALITY_PROFILES.getKey());
  }

  /**
   * Used by license notifications
   */
  public Set<EmailSubscriberDto> selectGlobalAdministerEmailSubscribers(DbSession dbSession) {
    return mapper(dbSession).selectEmailSubscribersWithGlobalPermission(ADMINISTER.getKey());
  }

  public Set<String> keepAuthorizedLoginsOnProject(DbSession dbSession, Set<String> logins, String projectKey, String permission) {
    return executeLargeInputsIntoSet(
      logins,
      partitionOfLogins -> mapper(dbSession).keepAuthorizedLoginsOnProject(partitionOfLogins, projectKey, permission),
      partitionSize -> partitionSize / 3);
  }

  private static AuthorizationMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(AuthorizationMapper.class);
  }
}
