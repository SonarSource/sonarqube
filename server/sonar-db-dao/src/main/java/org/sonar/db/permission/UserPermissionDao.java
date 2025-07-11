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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserPermissionNewValue;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserId;
import org.sonar.db.user.UserIdDto;

import com.google.common.annotations.VisibleForTesting;

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
      .toList();
  }

  public int countUsersByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countUsersByQuery(query);
  }

  /**
   * Count the number of users per permission for a given list of entities
   *
   * @param entityUuids a non-null list of entity uuids to filter on. If empty then an empty list is returned.
   */
  @VisibleForTesting
  List<CountPerEntityPermission> countUsersByEntityPermission(DbSession dbSession, Collection<String> entityUuids) {
    return executeLargeInputs(entityUuids, mapper(dbSession)::countUsersByEntityPermission);
  }

  /**
   * Gets all the global permissions granted to user
   *
   * @return the global permissions. An empty list is returned if user do not exist.
   */
  public List<String> selectGlobalPermissionsOfUser(DbSession dbSession, String userUuid, String organizationUuid) {
    return mapper(dbSession).selectGlobalPermissionsOfUser(userUuid, organizationUuid);
  }

  /**
   * Gets all the entity permissions granted to user for the specified entity.
   *
   * @return the entity permissions. An empty list is returned if entity or user do not exist.
   */
  public List<String> selectEntityPermissionsOfUser(DbSession dbSession, String userUuid, String entityUuid) {
    return mapper(dbSession).selectEntityPermissionsOfUser(userUuid, entityUuid);
  }

  public Set<UserIdDto> selectUserIdsWithPermissionOnEntityBut(DbSession session, String entityUuid, String permission) {
    return mapper(session).selectUserIdsWithPermissionOnEntityBut(entityUuid, permission);
  }

  public void insert(DbSession dbSession, UserPermissionDto dto, @Nullable EntityDto entityDto,
    @Nullable UserId userId, @Nullable PermissionTemplateDto templateDto) {
    mapper(dbSession).insert(dto);

    String entityName = (entityDto != null) ? entityDto.getName() : null;
    String entityKey = (entityDto != null) ? entityDto.getKey() : null;
    String entityQualifier = (entityDto != null) ? entityDto.getQualifier() : null;
    String organizationUuid = entityDto != null && entityDto.getOrganizationUuid() != null
        ? entityDto.getOrganizationUuid() : dto.getOrganizationUuid();

    auditPersister.addUserPermission(dbSession, organizationUuid,
        new UserPermissionNewValue(dto, entityKey, entityName, userId, entityQualifier,
            templateDto));
  }

  /**
   * Removes a single global permission from user
   */
  public void deleteGlobalPermission(DbSession dbSession, UserId user, String permission, String organizationUuid) {
    int deletedRows = mapper(dbSession).deleteGlobalPermission(user.getUuid(), permission, organizationUuid);

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, organizationUuid,
          new UserPermissionNewValue(permission, null, null, null, user, null));
    }
  }

  /**
   * Removes a single entity permission from user
   */
  public void deleteEntityPermission(DbSession dbSession, UserId user, String permission, EntityDto entity) {
    int deletedRows = mapper(dbSession).deleteEntityPermission(user.getUuid(), permission, entity.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, entity.getOrganizationUuid(), new UserPermissionNewValue(
          permission, entity.getUuid(), entity.getKey(), entity.getName(), user, entity.getQualifier()));
    }
  }

  /**
   * Deletes all the permissions defined on an entity
   */
  public void deleteEntityPermissions(DbSession dbSession, EntityDto entity) {
    int deletedRows = mapper(dbSession).deleteEntityPermissions(entity.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, entity.getOrganizationUuid(),
          new UserPermissionNewValue(null, entity.getUuid(), entity.getKey(),
              entity.getName(), null, entity.getQualifier()));
    }
  }

  /**
   * Deletes the specified permission on the specified entity for any user.
   */
  public int deleteEntityPermissionOfAnyUser(DbSession dbSession, String permission, EntityDto entity) {
    int deletedRows = mapper(dbSession).deleteEntityPermissionOfAnyUser(entity.getUuid(), permission);

    if (deletedRows > 0) {
      auditPersister.deleteUserPermission(dbSession, entity.getOrganizationUuid(),
          new UserPermissionNewValue(permission, entity.getUuid(), entity.getKey(), entity.getName(), null, entity.getQualifier()));
    }

    return deletedRows;
  }

  public void deleteByOrganization(DbSession dbSession, String organizationUuid) {
    mapper(dbSession).deleteByOrganization(organizationUuid);
  }

  public void deleteOrganizationMemberPermissions(DbSession dbSession, String organizationUuid, String userUuid) {
    mapper(dbSession).deleteOrganizationMemberPermissions(organizationUuid, userUuid);
  }

  public void deleteByUserUuid(DbSession dbSession, UserId userId) {
    List<UserPermissionDto> permissions = mapper(dbSession).selectByUserUuid(userId.getUuid());
    int deletedRows = mapper(dbSession).deleteByUserUuid(userId.getUuid());

    if (deletedRows > 0) {
      for (UserPermissionDto permission : permissions) {
        auditPersister.deleteUserPermission(dbSession, permission.getOrganizationUuid(), new UserPermissionNewValue(userId, null));
      }
    }
  }

  private static UserPermissionMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserPermissionMapper.class);
  }
}
