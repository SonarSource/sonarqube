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
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.component.ComponentDto;
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
    return mapper(dbSession).selectGroupNamesByQuery(query, new RowBounds(query.getPageOffset(), query.getPageSize()));
  }

  /**
   * Count the number of groups returned by {@link #selectGroupNamesByQuery(DbSession, PermissionQuery)},
   * without applying pagination.
   */
  public int countGroupsByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countGroupsByQuery(query);
  }

  /**
   * Select global or project permission of given groups. Anyone virtual group is supported
   * through the value "zero" (0L) in {@code groupUuids}.
   */
  public List<GroupPermissionDto> selectByGroupUuids(DbSession dbSession, List<String> groupUuids, @Nullable String projectUuid) {
    return executeLargeInputs(groupUuids, groups -> mapper(dbSession).selectByGroupUuids(groups, projectUuid));
  }

  public List<String> selectProjectKeysWithAnyonePermissions(DbSession dbSession, int max) {
    return mapper(dbSession).selectProjectKeysWithAnyonePermissions(max);
  }

  public int countProjectsWithAnyonePermissions(DbSession dbSession) {
    return mapper(dbSession).countProjectsWithAnyonePermissions();
  }

  /**
   * Select global and project permissions of a given group (Anyone group is NOT supported)
   * Each row returns a {@link GroupPermissionDto}
   */
  public void selectAllPermissionsByGroupUuid(DbSession dbSession, String groupUuid, ResultHandler<GroupPermissionDto> resultHandler) {
    mapper(dbSession).selectAllPermissionsByGroupUuid(groupUuid, resultHandler);
  }

  /**
   * Each row returns a {@link CountPerProjectPermission}
   */
  public void groupsCountByComponentUuidAndPermission(DbSession dbSession, List<String> componentUuids, ResultHandler<CountPerProjectPermission> resultHandler) {
    Map<String, Object> parameters = new HashMap<>(2);
    parameters.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);

    executeLargeInputsWithoutOutput(
      componentUuids,
      partitionedComponentUuids -> {
        parameters.put("componentUuids", partitionedComponentUuids);
        mapper(dbSession).groupsCountByProjectUuidAndPermission(parameters, resultHandler);
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
   * Selects the permissions granted to group and project. An empty list is returned if the
   * group or project do not exist.
   */
  public List<String> selectProjectPermissionsOfGroup(DbSession session, @Nullable String groupUuid, String projectUuid) {
    return mapper(session).selectProjectPermissionsOfGroup(groupUuid, projectUuid);
  }

  /**
   * Lists uuid of groups with at least one permission on the specified root component but which do not have the specified
   * permission, <strong>excluding group "AnyOne"</strong> (which implies the returned {@code Sett} can't contain
   * {@code null}).
   */
  public Set<String> selectGroupUuidsWithPermissionOnProjectBut(DbSession session, String projectUuid, String permission) {
    return mapper(session).selectGroupUuidsWithPermissionOnProjectBut(projectUuid, permission);
  }

  public void insert(DbSession dbSession, GroupPermissionDto groupPermissionDto, @Nullable ComponentDto componentDto, @Nullable PermissionTemplateDto permissionTemplateDto) {
    mapper(dbSession).insert(groupPermissionDto);

    String componentKey = (componentDto != null) ? componentDto.getKey() : null;
    String qualifier = (componentDto != null) ? componentDto.qualifier() : null;
    auditPersister.addGroupPermission(dbSession, new GroupPermissionNewValue(groupPermissionDto, componentKey, qualifier, permissionTemplateDto));
  }

  /**
   * Delete all the permissions associated to a root component (project)
   */
  public void deleteByRootComponentUuid(DbSession dbSession, ComponentDto component) {
    int deletedRecords = mapper(dbSession).deleteByRootComponentUuid(component.uuid());

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(component.uuid(),
        component.getKey(), component.name(), null, null, null, component.qualifier()));
    }
  }

  /**
   * Delete all permissions of the specified group (group "AnyOne" if {@code groupUuid} is {@code null}) for the specified
   * component.
   */
  public int deleteByRootComponentUuidAndGroupUuid(DbSession dbSession, @Nullable String groupUuid, ComponentDto component) {
    int deletedRecords = mapper(dbSession).deleteByRootComponentUuidAndGroupUuid(component.uuid(), groupUuid);

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(component.uuid(),
        component.getKey(), component.name(), null, groupUuid, "", component.qualifier()));
    }
    return deletedRecords;
  }

  public int deleteByRootComponentUuidForAnyOne(DbSession dbSession, ComponentDto component) {
    int deletedRecords = mapper(dbSession).deleteByRootComponentUuidAndGroupUuid(component.uuid(), null);

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(component.uuid(),
        component.getKey(), component.name(), null, null, null, component.qualifier()));
    }

    return deletedRecords;
  }

  /**
   * Delete the specified permission for the specified component for any group (including group AnyOne).
   */
  public int deleteByRootComponentUuidAndPermission(DbSession dbSession, String permission, ComponentDto component) {
    int deletedRecords = mapper(dbSession).deleteByRootComponentUuidAndPermission(component.uuid(), permission);

    if (deletedRecords > 0) {
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(component.uuid(),
        component.getKey(), component.name(), permission, null, null, component.qualifier()));
    }

    return deletedRecords;
  }

  /**
   * Delete a single permission. It can be:
   * <ul>
   *   <li>a global permission granted to a group</li>
   *   <li>a global permission granted to anyone</li>
   *   <li>a permission granted to a group for a project</li>
   *   <li>a permission granted to anyone for a project</li>
   * </ul>
   *
   * @param dbSession
   * @param permission        the kind of permission
   * @param groupUuid         if null, then anyone, else uuid of group
   * @param rootComponentUuid if null, then global permission, otherwise the uuid of root component (project)
   */
  public void delete(DbSession dbSession, String permission, @Nullable String groupUuid,
    @Nullable String groupName, @Nullable String rootComponentUuid, @Nullable ComponentDto componentDto) {

    int deletedRecords = mapper(dbSession).delete(permission, groupUuid, rootComponentUuid);

    if (deletedRecords > 0) {
      String qualifier = (componentDto != null) ? componentDto.qualifier() : null;
      String componentKey = (componentDto != null) ? componentDto.getKey() : null;
      String componentName = (componentDto != null) ? componentDto.name() : null;
      auditPersister.deleteGroupPermission(dbSession, new GroupPermissionNewValue(rootComponentUuid,
        componentKey, componentName, permission, groupUuid, groupName, qualifier));
    }
  }

  private static GroupPermissionMapper mapper(DbSession session) {
    return session.getMapper(GroupPermissionMapper.class);
  }
}
