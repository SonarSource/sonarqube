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
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserPermissionNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserId;
import org.sonar.db.user.UserIdDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class UserPermissionDao implements Dao {
  private final AuditPersister auditPersister;

  public UserPermissionDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  /**
   * List of user permissions ordered by alphabetical order of user names.
   * Pagination is NOT applied.
   * No sort is done.
   *
   * @param query non-null query including optional filters.
   * @param userUuids Filter on user ids, including disabled users. Must not be empty and maximum size is {@link DatabaseUtils#PARTITION_SIZE_FOR_ORACLE}.
   */
  public List<UserPermissionDto> selectUserPermissionsByQuery(DbSession dbSession, PermissionQuery query, Collection<String> userUuids) {
    if (userUuids.isEmpty()) {
      return emptyList();
    }
    checkArgument(userUuids.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE, "Maximum 1'000 users are accepted");
    return mapper(dbSession).selectUserPermissionsByQueryAndUserUuids(query, userUuids);
  }

  public List<String> selectUserUuidsByQuery(DbSession dbSession, PermissionQuery query) {
    return paginate(mapper(dbSession).selectUserUuidsByQuery(query), query);
  }

  public List<String> selectUserUuidsByQueryAndScope(DbSession dbSession, PermissionQuery query) {
    return paginate(mapper(dbSession).selectUserUuidsByQueryAndScope(query), query);
  }

  private static List<String> paginate(List<String> results, PermissionQuery query) {
    return results
      .stream()
      // Pagination is done in Java because it's too complex to use SQL pagination in Oracle and MsSQL with the distinct
      .skip(query.getPageOffset())
      .limit(query.getPageSize())
      .collect(MoreCollectors.toArrayList());
  }

  public int countUsersByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countUsersByQuery(query);
  }

  /**
   * Count the number of users per permission for a given list of projects
   *
   * @param projectUuids a non-null list of project uuids to filter on. If empty then an empty list is returned.
   */
  public List<CountPerProjectPermission> countUsersByProjectPermission(DbSession dbSession, Collection<String> projectUuids) {
    return executeLargeInputs(projectUuids, mapper(dbSession)::countUsersByProjectPermission);
  }

  /**
   * Gets all the global permissions granted to user
   *
   * @return the global permissions. An empty list is returned if user do not exist.
   */
  public List<String> selectGlobalPermissionsOfUser(DbSession dbSession, String userUuid) {
    return mapper(dbSession).selectGlobalPermissionsOfUser(userUuid);
  }

  /**
   * Gets all the project permissions granted to user for the specified project.
   *
   * @return the project permissions. An empty list is returned if project or user do not exist.
   */
  public List<String> selectProjectPermissionsOfUser(DbSession dbSession, String userUuid, String projectUuid) {
    return mapper(dbSession).selectProjectPermissionsOfUser(userUuid, projectUuid);
  }

  public Set<UserIdDto> selectUserIdsWithPermissionOnProjectBut(DbSession session, String projectUuid, String permission) {
    return mapper(session).selectUserIdsWithPermissionOnProjectBut(projectUuid, permission);
  }

  public void insert(DbSession dbSession, UserPermissionDto dto, @Nullable ComponentDto componentDto,
    @Nullable UserId userId, @Nullable PermissionTemplateDto templateDto) {
    mapper(dbSession).insert(dto);

    String componentName = (componentDto != null) ? componentDto.name() : null;
    String componentKey = (componentDto != null) ? componentDto.getKey() : null;
    String qualifier = (componentDto != null) ? componentDto.qualifier() : null;
    auditPersister.addUserPermission(dbSession, new UserPermissionNewValue(dto, componentKey, componentName, userId, qualifier,
      templateDto));
  }

  /**
   * Removes a single global permission from user
   */
  public void deleteGlobalPermission(DbSession dbSession, UserId user, String permission) {
    int deletedRows = mapper(dbSession).deleteGlobalPermission(user.getUuid(), permission);

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, new UserPermissionNewValue(permission, null, null, null, user, null));
    }
  }

  /**
   * Removes a single project permission from user
   */
  public void deleteProjectPermission(DbSession dbSession, UserId user, String permission, ComponentDto component) {
    int deletedRows = mapper(dbSession).deleteProjectPermission(user.getUuid(), permission, component.uuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, new UserPermissionNewValue(permission, component.uuid(), component.getKey(), component.name(), user, component.qualifier()));
    }
  }

  /**
   * Deletes all the permissions defined on a project
   */
  public void deleteProjectPermissions(DbSession dbSession, ComponentDto component) {
    int deletedRows = mapper(dbSession).deleteProjectPermissions(component.uuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, new UserPermissionNewValue(null, component.uuid(), component.getKey(),
        component.name(), null, component.qualifier()));
    }
  }

  /**
   * Deletes the specified permission on the specified project for any user.
   */
  public int deleteProjectPermissionOfAnyUser(DbSession dbSession, String permission, ComponentDto project) {
    int deletedRows = mapper(dbSession).deleteProjectPermissionOfAnyUser(project.uuid(), permission);

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, new UserPermissionNewValue(permission, project.uuid(), project.getKey(), project.name(), null, project.qualifier()));
    }

    return deletedRows;
  }

  public void deleteByUserUuid(DbSession dbSession, UserId userId) {
    int deletedRows = mapper(dbSession).deleteByUserUuid(userId.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, new UserPermissionNewValue(userId, null));
    }
  }

  private static UserPermissionMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserPermissionMapper.class);
  }
}
