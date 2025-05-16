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

import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.user.UserGroupValidation;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupQuery;
import org.sonar.server.common.SearchResults;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class GroupService {

  private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

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

  public Optional<GroupDto> findGroup(DbSession dbSession, OrganizationDto organization, String groupName) {
    return dbClient.groupDao().selectByName(dbSession, organization.getUuid(), groupName);
  }

  public Optional<GroupInformation> findGroupByUuid(DbSession dbSession, String groupUuid) {

    return Optional.ofNullable(dbClient.groupDao().selectByUuid(dbSession, groupUuid))
      .map(group -> groupDtoToGroupInformation(group, dbSession));
  }

  public SearchResults<GroupInformation> search(DbSession dbSession, GroupSearchRequest groupSearchRequest) {
    GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession, groupSearchRequest.organization().getUuid());
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
      .organizationUuid(groupSearchRequest.organization().getUuid())
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

  public void delete(DbSession dbSession, OrganizationDto organization, GroupDto group) {
    logger.info("Delete Group Request :: groupName: {} and organization: {}, orgId: {}", group.getName(),
        organization.getKey(), organization.getUuid());

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
    logger.info("Group deleted :: groupName {} and orgId: {}", group.getName(), group.getOrganizationUuid());
  }

  public GroupInformation updateGroup(DbSession dbSession, OrganizationDto organization, GroupDto group, @Nullable String newName) {
    checkGroupIsNotDefault(dbSession, group);
    return groupDtoToGroupInformation(updateName(dbSession, organization, group, newName), dbSession);
  }

  public GroupInformation updateGroup(DbSession dbSession, OrganizationDto organization, GroupDto group, @Nullable String newName, @Nullable String newDescription) {
    checkGroupIsNotDefault(dbSession, group);
    GroupDto withUpdatedName = updateName(dbSession, organization, group, newName);
    return groupDtoToGroupInformation(updateDescription(dbSession, withUpdatedName, newDescription), dbSession);
  }

  public GroupInformation createGroup(DbSession dbSession, OrganizationDto organization, String name, @Nullable String description) {
    validateGroupName(name);
    checkNameDoesNotExist(dbSession, organization, name);

    logger.info("Create Group Request :: group name: {} and organization: {}", name, organization.getUuid());

    GroupDto group = new GroupDto()
      .setUuid(uuidFactory.create())
      .setOrganizationUuid(organization.getUuid())
      .setName(name)
      .setDescription(description);
    try {
      return groupDtoToGroupInformation(dbClient.groupDao().insert(dbSession, group), dbSession);
    } finally {
      logger.info("Group Created :: groupName: {} and orgId: {}", name, organization.getUuid());
    }
  }

  public GroupDto findDefaultGroup(DbSession dbSession, String organizationUuid) {
    Objects.requireNonNull(organizationUuid);
    String defaultGroupUuid = dbClient.organizationDao().getDefaultGroupUuid(dbSession, organizationUuid)
        .orElseThrow(() -> new IllegalStateException(format("Default group cannot be found on organization '%s'", organizationUuid)));
    return requireNonNull(dbClient.groupDao().selectByUuid(dbSession, defaultGroupUuid), format("Group '%s' cannot be found", defaultGroupUuid));
  }

  private GroupInformation groupDtoToGroupInformation(GroupDto groupDto, DbSession dbSession) {
    return new GroupInformation(groupDto, managedInstanceService.isGroupManaged(dbSession, groupDto.getUuid()), false);
  }

  private GroupDto updateName(DbSession dbSession, OrganizationDto organization, GroupDto group, @Nullable String newName) {
    if (newName != null && !newName.equals(group.getName())) {
      validateGroupName(newName);
      checkNameDoesNotExist(dbSession, organization, newName);
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

  private void checkNameDoesNotExist(DbSession dbSession, OrganizationDto organization, String name) {
    // There is no database constraint on column groups.name
    // because MySQL cannot create a unique index
    // on a UTF-8 VARCHAR larger than 255 characters on InnoDB
    checkRequest(dbClient.groupDao().selectByName(dbSession, organization.getUuid(), name).isEmpty(), "Group '%s' already exists", name);
  }

  private GroupDto updateDescription(DbSession dbSession, GroupDto group, @Nullable String newDescription) {
    if (newDescription != null) {
      group.setDescription(newDescription);
      return dbClient.groupDao().update(dbSession, group);
    }
    return group;
  }

  private void checkGroupIsNotDefault(DbSession dbSession, GroupDto groupDto) {
    GroupDto defaultGroup = findDefaultGroup(dbSession, groupDto.getOrganizationUuid());
    checkArgument(!defaultGroup.getUuid().equals(groupDto.getUuid()), "Default group '%s' cannot be used to perform this action", groupDto.getName());
  }

  private void checkNotTryingToDeleteLastAdminGroup(DbSession dbSession, GroupDto group) {
    int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
      group.getOrganizationUuid(), OrganizationPermission.ADMINISTER.getKey(), group.getUuid());

    checkArgument(remaining > 0, "The last system admin group cannot be deleted");
  }

  private void removeGroupPermissions(DbSession dbSession, GroupDto group) {
    logger.debug("Removing group permissions for group: {}", group.getName());
    dbClient.roleDao().deleteGroupRolesByGroupUuid(dbSession, group.getUuid());
  }

  private void removeGroupFromPermissionTemplates(DbSession dbSession, GroupDto group) {
    logger.debug("Removing group from permission template for group: {}", group.getName());
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, group.getUuid(), group.getName());
  }

  private void removeGroupMembers(DbSession dbSession, GroupDto group) {
    logger.debug("Removing group members for group: {}", group.getName());
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

  public void checkOrgGroupMapping(OrganizationDto organization, String groupId) {
    try (DbSession session = dbClient.openSession(false)){
      Optional<GroupInformation> groupByUuid = findGroupByUuid(session, groupId);
      if(groupByUuid.isEmpty()){
        logger.warn("Group with id does not exist");
        throw new NotFoundException("Group does not exist");
      }
      Optional<GroupDto> group = findGroup(session, organization, groupByUuid.get().groupDto().getName());
      if(group.isEmpty()){
        logger.warn("Group '{}' does not exist in organization '{}'", group.get().getName(), organization.getName());
        throw new NotFoundException("Group does not exist in current organization ");
      }
    }
  }
}
