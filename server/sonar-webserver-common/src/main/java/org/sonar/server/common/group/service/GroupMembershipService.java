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

import java.util.List;
import java.util.Optional;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserGroupQuery;
import org.sonar.server.common.SearchResults;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

@ServerSide
public class GroupMembershipService {
  private final DbClient dbClient;
  private final UserGroupDao userGroupDao;
  private final UserDao userDao;
  private final GroupDao groupDao;

  public GroupMembershipService(DbClient dbClient, UserGroupDao userGroupDao, UserDao userDao, GroupDao groupDao) {
    this.dbClient = dbClient;
    this.userGroupDao = userGroupDao;
    this.userDao = userDao;
    this.groupDao = groupDao;
  }

  public SearchResults<UserGroupDto> searchMembers(GroupMembershipSearchRequest groupMembershipSearchRequest, int pageIndex, int pageSize) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserGroupQuery query = new UserGroupQuery(null, groupMembershipSearchRequest.groupUuid(), groupMembershipSearchRequest.userUuid());
      int total = userGroupDao.countByQuery(dbSession, query);
      if (pageSize == 0) {
        return new SearchResults<>(List.of(), total);
      }
      List<UserGroupDto> userGroupDtos = userGroupDao.selectByQuery(dbSession, query, pageIndex, pageSize);
      return new SearchResults<>(userGroupDtos, total);
    }
  }

  public UserGroupDto addMembership(String groupUuid, String userUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = findUserOrThrow(userUuid, dbSession);
      GroupDto groupDto = findNonDefaultGroupOrThrow(groupUuid, dbSession);
      UserGroupDto userGroupDto = new UserGroupDto().setGroupUuid(groupUuid).setUserUuid(userUuid);
      checkArgument(isNotInGroup(dbSession, groupUuid, userUuid), "User '%s' is already a member of group '%s'", userDto.getLogin(), groupDto.getName());
      userGroupDao.insert(dbSession, userGroupDto, groupDto.getName(), userDto.getLogin());
      dbSession.commit();
      return userGroupDto;
    }
  }

  private boolean isNotInGroup(DbSession dbSession, String groupUuid, String userUuid) {
    return userGroupDao.selectByQuery(dbSession, new UserGroupQuery(null, groupUuid, userUuid), 1, 1).isEmpty();
  }

  public void removeMembership(String groupMembershipUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserGroupDto userGroupDto = findMembershipOrThrow(groupMembershipUuid, dbSession);
      removeMembership(userGroupDto.getGroupUuid(), userGroupDto.getUserUuid());
    }
  }

  private UserGroupDto findMembershipOrThrow(String groupMembershipUuid, DbSession dbSession) {
    return userGroupDao.selectByQuery(dbSession, new UserGroupQuery(groupMembershipUuid, null, null), 1, 1).stream()
      .findFirst()
      .orElseThrow(() -> new NotFoundException(format("Group membership '%s' not found", groupMembershipUuid)));
  }

  public void removeMembership(String groupUuid, String userUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = findUserOrThrow(userUuid, dbSession);
      GroupDto groupDto = findNonDefaultGroupOrThrow(groupUuid, dbSession);
      ensureLastAdminIsNotRemoved(dbSession, groupUuid, userUuid);
      userGroupDao.delete(dbSession, groupDto, userDto);
      dbSession.commit();
    }
  }

  private GroupDto findNonDefaultGroupOrThrow(String groupUuid, DbSession dbSession) {
    GroupDto groupDto = groupDao.selectByUuid(dbSession, groupUuid);
    checkFound(groupDto, "Group '%s' not found", groupUuid);
    checkArgument(!groupDto.getName().equals(DefaultGroups.USERS), "Default group '%s' cannot be used to perform this action", groupDto.getName());
    return groupDto;
  }

  private UserDto findUserOrThrow(String userUuid, DbSession dbSession) {
    return Optional.ofNullable(userDao.selectByUuid(dbSession, userUuid))
      .filter(UserDto::isActive)
      .orElseThrow(() -> new NotFoundException(format("User '%s' not found", userUuid)));
  }

  private void ensureLastAdminIsNotRemoved(DbSession dbSession, String groupUuids, String userUuid) {
    int remainingAdmins = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroupMember(dbSession,
      GlobalPermission.ADMINISTER.getKey(), groupUuids, userUuid);
    checkRequest(remainingAdmins > 0, "The last administrator user cannot be removed");
  }

}
