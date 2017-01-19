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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentMapper;
import org.sonar.db.user.GroupMapper;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class GroupPermissionDao implements Dao {

  private static final String ANYONE_GROUP_PARAMETER = "anyoneGroup";

  /**
   * Returns the names of the groups that match the given query, for the given organization.
   * The virtual group "Anyone" may be returned as the value {@link DefaultGroups#ANYONE}.
   * @return group names, sorted in alphabetical order
   */
  public List<String> selectGroupNamesByQuery(DbSession dbSession, String organizationUuid, PermissionQuery query) {
    return mapper(dbSession).selectGroupNamesByQuery(organizationUuid, query, new RowBounds(query.getPageOffset(), query.getPageSize()));
  }

  /**
   * Count the number of groups returned by {@link #selectGroupNamesByQuery(DbSession, String, PermissionQuery)},
   * without applying pagination.
   */
  public int countGroupsByQuery(DbSession dbSession, String organizationUuid, PermissionQuery query) {
    return mapper(dbSession).countGroupsByQuery(organizationUuid, query);
  }

  /**
   * Select global or project permission of given groups and organization. Anyone virtual group is supported
   * through the value "zero" (0L) in {@code groupIds}.
   */
  public List<GroupPermissionDto> selectByGroupIds(DbSession dbSession, String organizationUuid, List<Long> groupIds, @Nullable Long projectId) {
    return executeLargeInputs(groupIds, groups -> mapper(dbSession).selectByGroupIds(organizationUuid, groups, projectId));
  }

  /**
   * Each row returns a {@link CountPerProjectPermission}
   */
  public void groupsCountByComponentIdAndPermission(DbSession dbSession, List<Long> componentIds, ResultHandler resultHandler) {
    Map<String, Object> parameters = new HashMap<>(2);
    parameters.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);

    executeLargeInputsWithoutOutput(
      componentIds,
      partitionedComponentIds -> {
        parameters.put("componentIds", partitionedComponentIds);
        mapper(dbSession).groupsCountByProjectIdAndPermission(parameters, resultHandler);
        return null;
      });
  }

  /**
   * Selects the global permissions granted to group. An empty list is returned if the
   * group does not exist.
   */
  public List<String> selectGlobalPermissionsOfGroup(DbSession session, String organizationUuid, @Nullable Long groupId) {
    return mapper(session).selectGlobalPermissionsOfGroup(organizationUuid, groupId);
  }

  /**
   * Selects the permissions granted to group and project. An empty list is returned if the
   * group or project do not exist.
   */
  public List<String> selectProjectPermissionsOfGroup(DbSession session, String organizationUuid, @Nullable Long groupId, long projectId) {
    return mapper(session).selectProjectPermissionsOfGroup(organizationUuid, groupId, projectId);
  }

  /**
   * @return {@code true} if the project has at least one permission defined, whatever it is
   * on a group or on "anyone", else returns {@code false}
   */
  public boolean hasRootComponentPermissions(DbSession dbSession, long rootComponentId) {
    return mapper(dbSession).countRowsByRootComponentId(rootComponentId) > 0;
  }

  public void insert(DbSession dbSession, GroupPermissionDto dto) {
    ensureComponentPermissionConsistency(dbSession, dto);
    ensureGroupPermissionConsistency(dbSession, dto);
    mapper(dbSession).insert(dto);
  }

  private static void ensureComponentPermissionConsistency(DbSession dbSession, GroupPermissionDto dto) {
    if (dto.getResourceId() == null) {
      return;
    }
    ComponentMapper componentMapper = dbSession.getMapper(ComponentMapper.class);
    checkArgument(
      componentMapper.countComponentByOrganizationAndId(dto.getOrganizationUuid(), dto.getResourceId()) == 1,
      "Can't insert permission '%s' for component with id '%s' in organization with uuid '%s' because this component does not belong to organization with uuid '%s'",
      dto.getRole(), dto.getResourceId(), dto.getOrganizationUuid(), dto.getOrganizationUuid());
  }

  private static void ensureGroupPermissionConsistency(DbSession dbSession, GroupPermissionDto dto) {
    if (dto.getGroupId() == null) {
      return;
    }
    GroupMapper groupMapper = dbSession.getMapper(GroupMapper.class);
    checkArgument(
      groupMapper.countGroupByOrganizationAndId(dto.getOrganizationUuid(), dto.getGroupId()) == 1,
      "Can't insert permission '%s' for group with id '%s' in organization with uuid '%s' because this group does not belong to organization with uuid '%s'",
      dto.getRole(), dto.getGroupId(), dto.getOrganizationUuid(), dto.getOrganizationUuid());
  }

  /**
   * Delete all the permissions associated to a root component (project)
   */
  public void deleteByRootComponentId(DbSession dbSession, long rootComponentId) {
    mapper(dbSession).deleteByRootComponentId(rootComponentId);
  }

  /**
   * Delete a single permission. It can be:
   * <ul>
   *   <li>a global permission granted to a group</li>
   *   <li>a global permission granted to anyone</li>
   *   <li>a permission granted to a group for a project</li>
   *   <li>a permission granted to anyone for a project</li>
   * </ul>
   * @param dbSession
   * @param permission the kind of permission
   * @param organizationUuid UUID of organization, even if parameter {@code groupId} is not null
   * @param groupId if null, then anyone, else id of group
   * @param rootComponentId if null, then global permission, else id of root component (project)
   */
  public void delete(DbSession dbSession, String permission, String organizationUuid, @Nullable Long groupId, @Nullable Long rootComponentId) {
    mapper(dbSession).delete(permission, organizationUuid, groupId, rootComponentId);
  }

  public void deleteByOrganization(DbSession dbSession, String organizationUuid) {
    mapper(dbSession).deleteByOrganization(organizationUuid);
  }

  private static GroupPermissionMapper mapper(DbSession session) {
    return session.getMapper(GroupPermissionMapper.class);
  }
}
