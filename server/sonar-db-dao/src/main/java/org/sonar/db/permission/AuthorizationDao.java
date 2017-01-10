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

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsIntoSet;

/**
 * The SQL requests used to verify authorization (the permissions
 * granted to users)
 *
 * @see GroupPermissionDao for CRUD of table group_roles
 * @see UserPermissionDao for CRUD of table user_roles
 */
public class AuthorizationDao implements Dao {

  private static final String USER_ID_PARAM = "userId";

  private final MyBatis mybatis;

  public AuthorizationDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * Loads all the permissions granted to logged-in user for the specified organization
   */
  public Set<String> selectOrganizationPermissions(DbSession dbSession, String organizationUuid, long userId) {
    return mapper(dbSession).selectOrganizationPermissions(organizationUuid, userId);
  }

  /**
   * Loads all the permissions granted to anonymous user for the specified organization
   */
  public Set<String> selectOrganizationPermissionsOfAnonymous(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectOrganizationPermissionsOfAnonymous(organizationUuid);
  }

  /**
   * Loads all the permissions granted to logged-in user for the specified root component (project)
   */
  public Set<String> selectRootComponentPermissions(DbSession dbSession, long rootComponentId, long userId) {
    return mapper(dbSession).selectRootComponentPermissions(rootComponentId, userId);
  }

  /**
   * Loads all the permissions granted to anonymous user for the specified root component (project)
   */
  public Set<String> selectRootComponentPermissionsOfAnonymous(DbSession dbSession, long rootComponentId) {
    return mapper(dbSession).selectRootComponentPermissionsOfAnonymous(rootComponentId);
  }

  /**
   * The number of users who will still have the permission if the group {@code excludedGroupId}
   * is deleted. The anyone virtual group is not taken into account.
   */
  public int countUsersWithGlobalPermissionExcludingGroup(DbSession dbSession, String organizationUuid,
    String permission, long excludedGroupId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingGroup(organizationUuid, permission, excludedGroupId);
  }

  /**
   * The number of users who will still have the permission if the user {@code excludedUserId}
   * is deleted. The anyone virtual group is not taken into account.
   */
  public int countUsersWithGlobalPermissionExcludingUser(DbSession dbSession, String organizationUuid,
    String permission, long excludedUSerId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingUser(organizationUuid, permission, excludedUSerId);
  }

  /**
   * The number of users who will still have the permission if the user {@code userId}
   * is removed from group {@code groupId}. The anyone virtual group is not taken into account.
   * Contrary to {@link #countUsersWithGlobalPermissionExcludingUser(DbSession, String, String, long)}, user
   * still exists and may have the permission directly or through other groups.
   */
  public int countUsersWithGlobalPermissionExcludingGroupMember(DbSession dbSession, String organizationUuid,
    String permission, long groupId, long userId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingGroupMember(organizationUuid, permission, groupId, userId);
  }

  /**
   * The number of users who will still have the permission if the permission {@code permission}
   * is removed from user {@code userId}. The anyone virtual group is not taken into account.
   * Contrary to {@link #countUsersWithGlobalPermissionExcludingUser(DbSession, String, String, long)}, user
   * still exists and may have the permission through groups.
   */
  public int countUsersWithGlobalPermissionExcludingUserPermission(DbSession dbSession, String organizationUuid,
    String permission, long userId) {
    return mapper(dbSession).countUsersWithGlobalPermissionExcludingUserPermission(organizationUuid, permission, userId);
  }

  /**
   * The UUIDs of all the organizations in which the specified user has the specified global permission. An empty
   * set is returned if user or permission do not exist. An empty set is also returned if the user is not involved
   * in any organization.
   * <br/>
   * Group membership is taken into account. Anonymous privileges are ignored.
   */
  public Set<String> selectOrganizationUuidsOfUserWithGlobalPermission(DbSession dbSession, long userId, String permission) {
    return mapper(dbSession).selectOrganizationUuidsOfUserWithGlobalPermission(userId, permission);
  }

  public Set<Long> keepAuthorizedProjectIds(DbSession dbSession, Collection<Long> componentIds, @Nullable Integer userId, String role) {
    return executeLargeInputsIntoSet(
      componentIds,
      partition -> {
        if (userId == null) {
          return mapper(dbSession).keepAuthorizedProjectIdsForAnonymous(role, componentIds);
        }
        return mapper(dbSession).keepAuthorizedProjectIdsForUser(userId, role, componentIds);
      });
  }

  public Collection<String> selectAuthorizedRootProjectsKeys(DbSession dbSession, @Nullable Integer userId, String role) {
    String sql;
    Map<String, Object> params = new HashMap<>(2);
    sql = "selectAuthorizedRootProjectsKeys";
    params.put(USER_ID_PARAM, userId);
    params.put("role", role);

    return dbSession.selectList(sql, params);
  }

  public Collection<String> selectAuthorizedRootProjectsUuids(DbSession dbSession, @Nullable Integer userId, String role) {
    String sql;
    Map<String, Object> params = new HashMap<>(2);
    sql = "selectAuthorizedRootProjectsUuids";
    params.put(USER_ID_PARAM, userId);
    params.put("role", role);

    return dbSession.selectList(sql, params);
  }

  public List<String> selectGlobalPermissions(@Nullable String userLogin) {
    DbSession session = mybatis.openSession(false);
    try {
      Map<String, Object> params = new HashMap<>(1);
      params.put("userLogin", userLogin);
      return session.selectList("selectGlobalPermissions", params);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Keep only authorized user that have the given permission on a given project.
   * Please Note that if the permission is 'Anyone' is NOT taking into account by thie method.
   */
  public Collection<Long> keepAuthorizedUsersForRoleAndProject(DbSession dbSession, Collection<Long> userIds, String role, long projectId) {
    return executeLargeInputs(
      userIds,
      partitionOfIds -> mapper(dbSession).keepAuthorizedUsersForRoleAndProject(role, projectId, partitionOfIds));
  }

  public boolean isAuthorizedComponentKey(String componentKey, @Nullable Integer userId, String role) {
    DbSession session = mybatis.openSession(false);
    try {
      return keepAuthorizedComponentKeys(session, componentKey, userId, role).size() == 1;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static List<String> keepAuthorizedComponentKeys(DbSession dbSession, String componentKey, @Nullable Integer userId, String role) {
    if (userId == null) {
      return mapper(dbSession).keepAuthorizedComponentKeysForAnonymous(role, Sets.newHashSet(componentKey));
    } else {
      return mapper(dbSession).keepAuthorizedComponentKeysForUser(userId, role, Sets.newHashSet(componentKey));
    }
  }

  private static AuthorizationMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(AuthorizationMapper.class);
  }
}
