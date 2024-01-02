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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.template.PermissionTemplateDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class GroupPermissionDao implements Dao {

  private static final String ANYONE_GROUP_PARAMETER = "anyoneGroup";

  private final AuditPersister auditPersister;

  public GroupPermissionDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  /**
   * Returns the names of the groups that match the given query.
   * The virtual group "Anyone" may be returned as the value {@link DefaultGroups#ANYONE}.
   *
   * @return group names, sorted in alphabetical order
   */
  public List<String> selectGroupNamesByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).selectGroupNamesByQuery(query, Pagination.forPage(query.getPageIndex()).andSize(query.getPageSize()));
  }

  /**
   * Count the number of groups returned by {@link #selectGroupNamesByQuery(DbSession, PermissionQuery)},
   * without applying pagination.
   */
  public int countGroupsByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countGroupsByQuery(query);
  }

  /**
   * Select global or entity permission of given groups. Anyone virtual group is supported
   * through the value "zero" (0L) in {@code groupUuids}.
   */
  public List<GroupPermissionDto> selectByGroupUuids(DbSession dbSession, List<String> groupUuids, @Nullable String entityUuid) {
    return executeLargeInputs(groupUuids, groups -> mapper(dbSession).selectByGroupUuids(groups, entityUuid));
  }

  public List<String> selectProjectKeysWithAnyonePermissions(DbSession dbSession, int max) {
    return mapper(dbSession).selectProjectKeysWithAnyonePermissions(max);
  }

  public int countEntitiesWithAnyonePermissions(DbSession dbSession) {
    return mapper(dbSession).countEntitiesWithAnyonePermissions();
  }

  /**
   * Each row returns a {@link CountPerEntityPermission}
   */
  public void groupsCountByComponentUuidAndPermission(DbSession dbSession, List<String> entityUuids, ResultHandler<CountPerEntityPermission> resultHandler) {
    Map<String, Object> parameters = new HashMap<>(2);
    parameters.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);

    executeLargeInputsWithoutOutput(
      entityUuids,
      partitionedComponentUuids -> {
        parameters.put("entityUuids", partitionedComponentUuids);
        mapper(dbSession).groupsCountByEntityUuidAndPermission(parameters, resultHandler);
      });
  }

  /**
   * Selects the global permissions granted to group. An empty list is returned if the
   * group does not exist.
   */
  public List<String> selectGlobalPermissionsOfGroup(DbSession session, @Nullable String groupUuid) {
    return mapper(session).selectGlobalPermissionsOfGroup(groupUuid);
  }

  /**
   * Selects the permissions granted to group and entity. An empty list is returned if the
   * group or entity do not exist.
   */
  public List<String> selectEntityPermissionsOfGroup(DbSession session, @Nullable String groupUuid, String entityUuid) {
    return mapper(session).selectEntityPermissionsOfGroup(groupUuid, entityUuid);
  }

  /**
   * Lists uuid of groups with at least one permission on the specified entity but which do not have the specified
   * permission, <strong>excluding group "AnyOne"</strong> (which implies the returned {@code Sett} can't contain
   * {@code null}).
   */
  public Set<String> selectGroupUuidsWithPermissionOnEntityBut(DbSession session, String entityUuid, String permission) {
    return mapper(session).selectGroupUuidsWithPermissionOnEntityBut(entityUuid, permission);
  }

  /**
   * Lists group uuids that have the selected permission on the specified entity.
   */
  public Set<String> selectGroupUuidsWithPermissionOnEntity(DbSession session, String entityUuid, String permission) {
    return mapper(session).selectGroupUuidsWithPermissionOnEntity(entityUuid, permission);
  }

  public List<GroupPermissionDto> selectGroupPermissionsOnEntity(DbSession session, String entityUuid) {
    return mapper(session).selectGroupPermissionsOnEntity(entityUuid);
  }

  public void insert(DbSession dbSession, GroupPermissionDto groupPermissionDto, @Nullable EntityDto entityDto, @Nullable PermissionTemplateDto permissionTemplateDto) {
    mapper(dbSession).insert(groupPermissionDto);

    String componentKey = (entityDto != null) ? entityDto.getKey() : null;
    String qualifier = (entityDto != null) ? entityDto.getQualifier() : null;
    auditPersister.addGroupPermission(dbSession, new GroupPermissionNewValue(groupPermissionDto, componentKey, qualifier, permissionTemplateDto));
  }

  /**
   * Delete all the permissions associated to a entity
   */
  public void deleteByEntityUuid(DbSession dbSession, EntityDto entityDto) {
    int deletedRecords = mapper(dbSession).deleteByEntityUuid(entityDto.getUuid());

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(entityDto.getUuid(),
        entityDto.getKey(), entityDto.getName(), null, null, null, entityDto.getQualifier()));
    }
  }

  /**
   * Delete all permissions of the specified group (group "AnyOne" if {@code groupUuid} is {@code null}) for the specified
   * entity.
   */
  public int deleteByEntityAndGroupUuid(DbSession dbSession, @Nullable String groupUuid, EntityDto entityDto) {
    int deletedRecords = mapper(dbSession).deleteByEntityUuidAndGroupUuid(entityDto.getUuid(), groupUuid);

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(entityDto.getUuid(),
        entityDto.getKey(), entityDto.getName(), null, groupUuid, "", entityDto.getQualifier()));
    }
    return deletedRecords;
  }

  public int deleteByEntityUuidForAnyOne(DbSession dbSession, EntityDto entity) {
    int deletedRecords = mapper(dbSession).deleteByEntityUuidAndGroupUuid(entity.getUuid(), null);

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(entity.getUuid(),
        entity.getKey(), entity.getName(), null, null, null, entity.getQualifier()));
    }

    return deletedRecords;
  }

  /**
   * Delete the specified permission for the specified entity for any group (including group AnyOne).
   */
  public int deleteByEntityAndPermission(DbSession dbSession, String permission, EntityDto entity) {
    int deletedRecords = mapper(dbSession).deleteByEntityUuidAndPermission(entity.getUuid(), permission);

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(entity.getUuid(),
        entity.getKey(), entity.getName(), permission, null, null, entity.getQualifier()));
    }

    return deletedRecords;
  }

  /**
   * Delete a single permission. It can be:
   * <ul>
   *   <li>a global permission granted to a group</li>
   *   <li>a global permission granted to anyone</li>
   *   <li>a permission granted to a group for a entity</li>
   *   <li>a permission granted to anyone for a entity</li>
   * </ul>
   *
   * @param dbSession
   * @param permission        the kind of permission
   * @param groupUuid         if null, then anyone, else uuid of group
   * @param entityDto         if null, then global permission, otherwise the uuid of entity
   */
  public void delete(DbSession dbSession, String permission, @Nullable String groupUuid,
    @Nullable String groupName, @Nullable EntityDto entityDto) {

    int deletedRecords = mapper(dbSession).delete(permission, groupUuid, entityDto != null ? entityDto.getUuid() : null);

    if (deletedRecords > 0) {
      String entityUuid = (entityDto != null) ? entityDto.getUuid() : null;
      String qualifier = (entityDto != null) ? entityDto.getQualifier() : null;
      String componentKey = (entityDto != null) ? entityDto.getKey() : null;
      String componentName = (entityDto != null) ? entityDto.getName() : null;
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(entityUuid,
        componentKey, componentName, permission, groupUuid, groupName, qualifier));
    }
  }

  private static GroupPermissionMapper mapper(DbSession session) {
    return session.getMapper(GroupPermissionMapper.class);
  }
}
