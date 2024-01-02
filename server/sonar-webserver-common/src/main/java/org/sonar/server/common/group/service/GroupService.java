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
package org.sonar.server.common.group.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.api.user.UserGroupValidation;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupQuery;
import org.sonar.server.common.SearchResults;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.TRUE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class GroupService {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  private final DefaultGroupFinder defaultGroupFinder;
  private final ManagedInstanceService managedInstanceService;

  public GroupService(DbClient dbClient, UuidFactory uuidFactory, DefaultGroupFinder defaultGroupFinder, ManagedInstanceService managedInstanceService) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.defaultGroupFinder = defaultGroupFinder;
    this.managedInstanceService = managedInstanceService;
  }

  public Optional<GroupDto> findGroup(DbSession dbSession, String groupName) {
    return dbClient.groupDao().selectByName(dbSession, groupName);
  }

  public Optional<GroupInformation> findGroupByUuid(DbSession dbSession, String groupUuid) {

    return Optional.ofNullable(dbClient.groupDao().selectByUuid(dbSession, groupUuid))
      .map(group -> groupDtoToGroupInformation(group, dbSession));
  }

  public SearchResults<GroupInformation> search(DbSession dbSession, GroupSearchRequest groupSearchRequest) {
    GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession);
    GroupQuery query = toGroupQuery(groupSearchRequest);

    int limit = dbClient.groupDao().countByQuery(dbSession, query);
    if (groupSearchRequest.page() == 0) {
      return new SearchResults<>(List.of(), limit);
    }

    List<GroupDto> groups = dbClient.groupDao().selectByQuery(dbSession, query, groupSearchRequest.page(), groupSearchRequest.pageSize());
    List<String> groupUuids = extractGroupUuids(groups);
    Map<String, Boolean> groupUuidToIsManaged = managedInstanceService.getGroupUuidToManaged(dbSession, new HashSet<>(groupUuids));

    List<GroupInformation> results = groups.stream()
      .map(groupDto -> toGroupInformation(groupDto, defaultGroup.getUuid(), groupUuidToIsManaged))
      .toList();

    return new SearchResults<>(results, limit);
  }

  private GroupQuery toGroupQuery(GroupSearchRequest groupSearchRequest) {
    return GroupQuery.builder()
      .searchText(groupSearchRequest.query())
      .isManagedClause(getManagedInstanceSql(groupSearchRequest.managed()))
      .build();
  }

  @CheckForNull
  private String getManagedInstanceSql(@Nullable Boolean isManaged) {
    if (managedInstanceService.isInstanceExternallyManaged()) {
      return Optional.ofNullable(isManaged)
        .map(managedInstanceService::getManagedGroupsSqlFilter)
        .orElse(null);
    } else if (TRUE.equals(isManaged)) {
      throw BadRequestException.create("The 'managed' parameter is only available for managed instances.");
    }
    return null;
  }

  private static List<String> extractGroupUuids(List<GroupDto> groups) {
    return groups.stream().map(GroupDto::getUuid).toList();
  }

  private static GroupInformation toGroupInformation(GroupDto groupDto, String defaultGroupUuid, Map<String, Boolean> groupUuidToIsManaged) {
    return new GroupInformation(groupDto, groupUuidToIsManaged.getOrDefault(groupDto.getUuid(), false), defaultGroupUuid.equals(groupDto.getUuid()));
  }

  public void delete(DbSession dbSession, GroupDto group) {
    checkGroupIsNotDefault(dbSession, group);
    checkNotTryingToDeleteLastAdminGroup(dbSession, group);

    removeGroupPermissions(dbSession, group);
    removeGroupFromPermissionTemplates(dbSession, group);
    removeGroupMembers(dbSession, group);
    removeGroupFromQualityProfileEdit(dbSession, group);
    removeGroupFromQualityGateEdit(dbSession, group);
    removeGroupScimLink(dbSession, group);
    removeExternalGroupMapping(dbSession, group);
    removeGithubOrganizationGroup(dbSession, group);

    removeGroup(dbSession, group);
  }

  public GroupInformation updateGroup(DbSession dbSession, GroupDto group, @Nullable String newName) {
    checkGroupIsNotDefault(dbSession, group);
    return groupDtoToGroupInformation(updateName(dbSession, group, newName), dbSession);
  }

  public GroupInformation updateGroup(DbSession dbSession, GroupDto group, @Nullable String newName, @Nullable String newDescription) {
    checkGroupIsNotDefault(dbSession, group);
    GroupDto withUpdatedName = updateName(dbSession, group, newName);
    return groupDtoToGroupInformation(updateDescription(dbSession, withUpdatedName, newDescription), dbSession);
  }

  public GroupInformation createGroup(DbSession dbSession, String name, @Nullable String description) {
    validateGroupName(name);
    checkNameDoesNotExist(dbSession, name);

    GroupDto group = new GroupDto()
      .setUuid(uuidFactory.create())
      .setName(name)
      .setDescription(description);
    return groupDtoToGroupInformation(dbClient.groupDao().insert(dbSession, group), dbSession);
  }

  private GroupInformation groupDtoToGroupInformation(GroupDto groupDto, DbSession dbSession) {
    return new GroupInformation(groupDto, managedInstanceService.isGroupManaged(dbSession, groupDto.getUuid()),
      defaultGroupFinder.findDefaultGroup(dbSession).getUuid().equals(groupDto.getUuid()));
  }

  private GroupDto updateName(DbSession dbSession, GroupDto group, @Nullable String newName) {
    if (newName != null && !newName.equals(group.getName())) {
      validateGroupName(newName);
      checkNameDoesNotExist(dbSession, newName);
      group.setName(newName);
      return dbClient.groupDao().update(dbSession, group);
    }
    return group;
  }

  private static void validateGroupName(String name) {
    try {
      UserGroupValidation.validateGroupName(name);
    } catch (IllegalArgumentException e) {
      BadRequestException.throwBadRequestException(e.getMessage());
    }
  }

  private void checkNameDoesNotExist(DbSession dbSession, String name) {
    // There is no database constraint on column groups.name
    // because MySQL cannot create a unique index
    // on a UTF-8 VARCHAR larger than 255 characters on InnoDB
    checkRequest(!dbClient.groupDao().selectByName(dbSession, name).isPresent(), "Group '%s' already exists", name);
  }

  private GroupDto updateDescription(DbSession dbSession, GroupDto group, @Nullable String newDescription) {
    if (newDescription != null) {
      group.setDescription(newDescription);
      return dbClient.groupDao().update(dbSession, group);
    }
    return group;
  }

  private void checkGroupIsNotDefault(DbSession dbSession, GroupDto groupDto) {
    GroupDto defaultGroup = findDefaultGroup(dbSession);
    checkArgument(!defaultGroup.getUuid().equals(groupDto.getUuid()), "Default group '%s' cannot be used to perform this action", groupDto.getName());
  }

  private GroupDto findDefaultGroup(DbSession dbSession) {
    return dbClient.groupDao().selectByName(dbSession, DefaultGroups.USERS)
      .orElseThrow(() -> new IllegalStateException("Default group cannot be found"));
  }

  private void checkNotTryingToDeleteLastAdminGroup(DbSession dbSession, GroupDto group) {
    int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
      GlobalPermission.ADMINISTER.getKey(), group.getUuid());

    checkArgument(remaining > 0, "The last system admin group cannot be deleted");
  }

  private void removeGroupPermissions(DbSession dbSession, GroupDto group) {
    dbClient.roleDao().deleteGroupRolesByGroupUuid(dbSession, group.getUuid());
  }

  private void removeGroupFromPermissionTemplates(DbSession dbSession, GroupDto group) {
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, group.getUuid(), group.getName());
  }

  private void removeGroupMembers(DbSession dbSession, GroupDto group) {
    dbClient.userGroupDao().deleteByGroupUuid(dbSession, group.getUuid(), group.getName());
  }

  private void removeGroupFromQualityProfileEdit(DbSession dbSession, GroupDto group) {
    dbClient.qProfileEditGroupsDao().deleteByGroup(dbSession, group);
  }

  private void removeGroupFromQualityGateEdit(DbSession dbSession, GroupDto group) {
    dbClient.qualityGateGroupPermissionsDao().deleteByGroup(dbSession, group);
  }

  private void removeGroupScimLink(DbSession dbSession, GroupDto group) {
    dbClient.scimGroupDao().deleteByGroupUuid(dbSession, group.getUuid());
  }

  private void removeExternalGroupMapping(DbSession dbSession, GroupDto group) {
    dbClient.externalGroupDao().deleteByGroupUuid(dbSession, group.getUuid());
  }

  private void removeGithubOrganizationGroup(DbSession dbSession, GroupDto group) {
    dbClient.githubOrganizationGroupDao().deleteByGroupUuid(dbSession, group.getUuid());
  }

  private void removeGroup(DbSession dbSession, GroupDto group) {
    dbClient.groupDao().deleteByUuid(dbSession, group.getUuid(), group.getName());
  }

}
