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
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentMapper;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class UserPermissionDao implements Dao {

  /**
   * List of user permissions ordered by alphabetical order of user names.
   * Pagination is NOT applied.
   * No sort is done.
   *
   * @param query non-null query including optional filters.
   * @param userIds Filter on user ids, including disabled users. Must not be empty and maximum size is {@link DatabaseUtils#PARTITION_SIZE_FOR_ORACLE}.
   */
  public List<UserPermissionDto> selectUserPermissionsByQuery(DbSession dbSession, PermissionQuery query, Collection<Integer> userIds) {
    if (userIds.isEmpty()) {
      return emptyList();
    }
    checkArgument(userIds.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE, "Maximum 1'000 users are accepted");
    return mapper(dbSession).selectUserPermissionsByQueryAndUserIds(query, userIds);
  }

  public List<Integer> selectUserIdsByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).selectUserIdsByQuery(query)
      .stream()
      // Pagination is done in Java because it's too complex to use SQL pagination in Oracle and MsSQL with the distinct
      .skip(query.getPageOffset())
      .limit(query.getPageSize())
      .collect(MoreCollectors.toList());
  }

  public int countUsersByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countUsersByQuery(query);
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
   * Gets all the global permissions granted to user for the specified organization.
   *
   * @return the global permissions. An empty list is returned if user or organization do not exist.
   */
  public List<String> selectGlobalPermissionsOfUser(DbSession dbSession, int userId, String organizationUuid) {
    return mapper(dbSession).selectGlobalPermissionsOfUser(userId, organizationUuid);
  }

  /**
   * Gets all the project permissions granted to user for the specified project.
   *
   * @return the project permissions. An empty list is returned if project or user do not exist.
   */
  public List<String> selectProjectPermissionsOfUser(DbSession dbSession, int userId, long projectId) {
    return mapper(dbSession).selectProjectPermissionsOfUser(userId, projectId);
  }

  public Set<Integer> selectUserIdsWithPermissionOnProjectBut(DbSession session, long projectId, String permission) {
    return mapper(session).selectUserIdsWithPermissionOnProjectBut(projectId, permission);
  }

  public void insert(DbSession dbSession, UserPermissionDto dto) {
    ensureComponentPermissionConsistency(dbSession, dto);
    mapper(dbSession).insert(dto);
  }

  private static void ensureComponentPermissionConsistency(DbSession dbSession, UserPermissionDto dto) {
    if (dto.getComponentId() == null) {
      return;
    }
    ComponentMapper componentMapper = dbSession.getMapper(ComponentMapper.class);
    checkArgument(
      componentMapper.countComponentByOrganizationAndId(dto.getOrganizationUuid(), dto.getComponentId()) == 1,
      "Can't insert permission '%s' for component with id '%s' in organization with uuid '%s' because this component does not belong to organization with uuid '%s'",
      dto.getPermission(), dto.getComponentId(), dto.getOrganizationUuid(), dto.getOrganizationUuid());
  }

  /**
   * Removes a single global permission from user
   */
  public void deleteGlobalPermission(DbSession dbSession, int userId, String permission, String organizationUuid) {
    mapper(dbSession).deleteGlobalPermission(userId, permission, organizationUuid);
  }

  /**
   * Removes a single project permission from user
   */
  public void deleteProjectPermission(DbSession dbSession, int userId, String permission, long projectId) {
    mapper(dbSession).deleteProjectPermission(userId, permission, projectId);
  }

  /**
   * Deletes all the permissions defined on a project
   */
  public void deleteProjectPermissions(DbSession dbSession, long projectId) {
    mapper(dbSession).deleteProjectPermissions(projectId);
  }

  /**
   * Deletes the specified permission on the specified project for any user.
   */
  public int deleteProjectPermissionOfAnyUser(DbSession dbSession, long projectId, String permission) {
    return mapper(dbSession).deleteProjectPermissionOfAnyUser(projectId, permission);
  }

  public void deleteByOrganization(DbSession dbSession, String organizationUuid) {
    mapper(dbSession).deleteByOrganization(organizationUuid);
  }

  public void deleteOrganizationMemberPermissions(DbSession dbSession, String organizationUuid, int userId) {
    mapper(dbSession).deleteOrganizationMemberPermissions(organizationUuid, userId);
  }

  public void deleteByUserId(DbSession dbSession, int userId) {
    mapper(dbSession).deleteByUserId(userId);
  }

  private static UserPermissionMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserPermissionMapper.class);
  }
}
