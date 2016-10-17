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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class UserPermissionDao implements Dao {

  /**
   * @see UserPermissionMapper#selectByQuery(PermissionQuery, Collection, RowBounds)
   */
  public List<ExtendedUserPermissionDto> select(DbSession dbSession, PermissionQuery query, @Nullable Collection<String> userLogins) {
    if (userLogins != null) {
      if (userLogins.isEmpty()) {
        return emptyList();
      }
      checkArgument(userLogins.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE, "Maximum 1'000 users are accepted");
    }

    RowBounds rowBounds = new RowBounds(query.getPageOffset(), query.getPageSize());
    return mapper(dbSession).selectByQuery(query, userLogins, rowBounds);
  }

  /**
   * Shortcut over {@link #select(DbSession, PermissionQuery, Collection)} to return only logins, in the same order.
   */
  public List<String> selectLogins(DbSession dbSession, PermissionQuery query) {
    return select(dbSession, query, null).stream()
      .map(ExtendedUserPermissionDto::getUserLogin)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * @see UserPermissionMapper#countUsersByQuery(PermissionQuery, Collection)
   */
  public int countUsers(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countUsersByQuery(query, null);
  }

  /**
   * Count the number of users per permission for a given list of projects
   *
   * @param projectIds a non-null list of project ids to filter on. If empty then an empty list is returned.
   */
  public List<CountPerProjectPermission> countUsersByProjectPermission(DbSession dbSession, Collection<Long> projectIds) {
    return executeLargeInputs(projectIds, mapper(dbSession)::countUsersByProjectPermission);
  }

  /**
   * @return {@code true} if the project has at least one user permission defined, else returns {@code false}
   */
  public boolean hasRootComponentPermissions(DbSession dbSession, long rootComponentId) {
    return mapper(dbSession).countRowsByRootComponentId(rootComponentId) > 0;
  }

  /**
   * Gets all the global permissions granted to user for the specified organization.
   *
   * @return the global permissions. An empty list is returned if user or organization do not exist.
   */
  public List<String> selectGlobalPermissionsOfUser(DbSession dbSession, long userId, String organizationUuid) {
    return mapper(dbSession).selectGlobalPermissionsOfUser(userId, organizationUuid);
  }

  /**
   * Gets all the project permissions granted to user for the specified project.
   *
   * @return the project permissions. An empty list is returned if project or user do not exist.
   */
  public List<String> selectProjectPermissionsOfUser(DbSession dbSession, long userId, long projectId) {
    return mapper(dbSession).selectProjectPermissionsOfUser(userId, projectId);
  }

  public void insert(DbSession dbSession, UserPermissionDto dto) {
    mapper(dbSession).insert(dto);
  }

  /**
   * Removes a single global permission from user
   */
  public void deleteGlobalPermission(DbSession dbSession, long userId, String permission, String organizationUuid) {
    mapper(dbSession).deleteGlobalPermission(userId, permission, organizationUuid);
  }

  /**
   * Removes a single project permission from user
   */
  public void deleteProjectPermission(DbSession dbSession, long userId, String permission, long projectId) {
    mapper(dbSession).deleteProjectPermission(userId, permission, projectId);
  }

  /**
   * Deletes all the permissions defined on a project
   */
  public void deleteProjectPermissions(DbSession dbSession, long projectId) {
    mapper(dbSession).deleteProjectPermissions(projectId);
  }

  private static UserPermissionMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserPermissionMapper.class);
  }
}
