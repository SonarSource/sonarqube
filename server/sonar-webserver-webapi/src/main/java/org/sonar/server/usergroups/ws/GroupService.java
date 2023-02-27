/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import java.util.Set;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

@ServerSide
public class GroupService {

  private final DbClient dbClient;

  public GroupService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public GroupDto findGroupDtoOrThrow(DbSession dbSession, String groupName) {
    return dbClient.groupDao()
      .selectByName(dbSession, groupName)
      .orElseThrow(() -> new NotFoundException(format("No group with name '%s'", groupName)));
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
    removeGroup(dbSession, group);
  }

  void checkGroupIsNotDefault(DbSession dbSession, GroupDto groupDto) {
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

  private void removeGroup(DbSession dbSession, GroupDto group) {
    dbClient.groupDao().deleteByUuid(dbSession, group.getUuid(), group.getName());
  }

  public void deleteScimMembersByGroup(DbSession dbSession, GroupDto groupDto) {
    Set<UserDto> scimUsers = dbClient.userGroupDao().selectScimMembersByGroupUuid(dbSession, groupDto);
    dbClient.userGroupDao().deleteFromGroupByUserUuids(dbSession, groupDto, scimUsers);
  }
}
