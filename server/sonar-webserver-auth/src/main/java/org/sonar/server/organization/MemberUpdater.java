/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.organization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.difference;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;

public class MemberUpdater {

  private final DbClient dbClient;
  private final DefaultGroupFinder defaultGroupFinder;
  private final UserIndexer userIndexer;

  public MemberUpdater(DbClient dbClient, DefaultGroupFinder defaultGroupFinder, UserIndexer userIndexer) {
    this.dbClient = dbClient;
    this.defaultGroupFinder = defaultGroupFinder;
    this.userIndexer = userIndexer;
  }

  public void addMember(DbSession dbSession, UserDto user) {
    addMembers(dbSession, singletonList(user));
  }

  public void addMembers(DbSession dbSession, List<UserDto> users) {
    Set<String> currentMemberUuids = dbClient.userGroupDao().selectUserUuidsInGroup(dbSession, defaultGroupFinder.findDefaultGroup(dbSession).getUuid());
    List<UserDto> usersToAdd = users.stream()
      .filter(UserDto::isActive)
      .filter(u -> !currentMemberUuids.contains(u.getUuid()))
      .collect(toList());
    if (usersToAdd.isEmpty()) {
      return;
    }
    usersToAdd.forEach(u -> addMemberInDb(dbSession, u));
    userIndexer.commitAndIndex(dbSession, usersToAdd);
  }

  private void addMemberInDb(DbSession dbSession, UserDto user) {
    String defaultGroupUuid = defaultGroupFinder.findDefaultGroup(dbSession).getUuid();
    dbClient.userGroupDao().insert(dbSession,
      new UserGroupDto().setGroupUuid(defaultGroupUuid).setUserUuid(user.getUuid()));
  }

  public void removeMember(DbSession dbSession, UserDto user) {
    removeMembers(dbSession, singletonList(user));
  }

  public void removeMembers(DbSession dbSession, List<UserDto> users) {
    String defaultGroupUuid = defaultGroupFinder.findDefaultGroup(dbSession).getUuid();
    Set<String> currentMemberIds = dbClient.userGroupDao().selectUserUuidsInGroup(dbSession, defaultGroupUuid);
    List<UserDto> usersToRemove = users.stream()
      .filter(UserDto::isActive)
      .filter(u -> currentMemberIds.contains(u.getUuid()))
      .collect(toList());
    if (usersToRemove.isEmpty()) {
      return;
    }

    Set<String> userUuidsToRemove = usersToRemove.stream().map(UserDto::getUuid).collect(toSet());
    Set<String> adminUuids = new HashSet<>(dbClient.authorizationDao().selectUserUuidsWithGlobalPermission(dbSession, ADMINISTER.getKey()));
    checkArgument(!difference(adminUuids, userUuidsToRemove).isEmpty(), "The last administrator member cannot be removed");

    usersToRemove.forEach(u -> removeMemberInDb(dbSession, u));
    userIndexer.commitAndIndex(dbSession, usersToRemove);
  }

  private void removeMemberInDb(DbSession dbSession, UserDto user) {
    String userUuid = user.getUuid();
    dbClient.propertiesDao().deleteByUser(dbSession, userUuid);
    dbClient.propertiesDao().deleteByMatchingLogin(dbSession, user.getLogin(), singletonList(DEFAULT_ISSUE_ASSIGNEE));
  }

}
