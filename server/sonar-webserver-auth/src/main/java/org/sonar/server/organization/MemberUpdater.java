/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.Map;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.ALM;
import org.sonar.db.alm.OrganizationAlmBindingDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
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

  public void addMember(DbSession dbSession, OrganizationDto organization, UserDto user) {
    addMembers(dbSession, organization, singletonList(user));
  }

  public void addMembers(DbSession dbSession, OrganizationDto organization, List<UserDto> users) {
    Set<String> currentMemberUuids = new HashSet<>(dbClient.organizationMemberDao().selectUserUuidsByOrganizationUuid(dbSession, organization.getUuid()));
    List<UserDto> usersToAdd = users.stream()
      .filter(UserDto::isActive)
      .filter(u -> !currentMemberUuids.contains(u.getUuid()))
      .collect(toList());
    if (usersToAdd.isEmpty()) {
      return;
    }
    usersToAdd.forEach(u -> addMemberInDb(dbSession, organization, u));
    userIndexer.commitAndIndex(dbSession, usersToAdd);
  }

  private void addMemberInDb(DbSession dbSession, OrganizationDto organization, UserDto user) {
    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto()
      .setOrganizationUuid(organization.getUuid())
      .setUserUuid(user.getUuid()));
    dbClient.userGroupDao().insert(dbSession,
      new UserGroupDto().setGroupUuid(defaultGroupFinder.findDefaultGroup(dbSession).getUuid()).setUserUuid(user.getUuid()));
  }

  public void removeMember(DbSession dbSession, OrganizationDto organization, UserDto user) {
    removeMembers(dbSession, organization, singletonList(user));
  }

  public void removeMembers(DbSession dbSession, OrganizationDto organization, List<UserDto> users) {
    Set<String> currentMemberIds = new HashSet<>(dbClient.organizationMemberDao().selectUserUuidsByOrganizationUuid(dbSession, organization.getUuid()));
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

    usersToRemove.forEach(u -> removeMemberInDb(dbSession, organization, u));
    userIndexer.commitAndIndex(dbSession, usersToRemove);
  }

  /**
   * Synchronize organization membership of a user from a list of ALM organization specific ids
   * Please note that no commit will not be executed.
   */
  public void synchronizeUserOrganizationMembership(DbSession dbSession, UserDto user, ALM alm, Set<String> organizationAlmIds) {
    Set<String> userOrganizationUuids = dbClient.organizationMemberDao().selectOrganizationUuidsByUser(dbSession, user.getUuid());
    Set<String> userOrganizationUuidsWithMembersSyncEnabled = dbClient.organizationAlmBindingDao().selectByOrganizationUuids(dbSession, userOrganizationUuids).stream()
      .filter(OrganizationAlmBindingDto::isMembersSyncEnable)
      .map(OrganizationAlmBindingDto::getOrganizationUuid)
      .collect(toSet());
    Set<String> almOrganizationUuidsWithMembersSyncEnabled = dbClient.organizationAlmBindingDao().selectByOrganizationAlmIds(dbSession, alm, organizationAlmIds).stream()
      .filter(OrganizationAlmBindingDto::isMembersSyncEnable)
      .map(OrganizationAlmBindingDto::getOrganizationUuid)
      .collect(toSet());

    Set<String> organizationUuidsToBeAdded = difference(almOrganizationUuidsWithMembersSyncEnabled, userOrganizationUuidsWithMembersSyncEnabled);
    Set<String> organizationUuidsToBeRemoved = difference(userOrganizationUuidsWithMembersSyncEnabled, almOrganizationUuidsWithMembersSyncEnabled);
    Map<String, OrganizationDto> allOrganizationsByUuid = dbClient.organizationDao().selectByUuids(dbSession, union(organizationUuidsToBeAdded, organizationUuidsToBeRemoved))
      .stream()
      .collect(uniqueIndex(OrganizationDto::getUuid));

    allOrganizationsByUuid.entrySet().stream()
      .filter(entry -> organizationUuidsToBeAdded.contains(entry.getKey()))
      .forEach(entry -> addMemberInDb(dbSession, entry.getValue(), user));
    allOrganizationsByUuid.entrySet().stream()
      .filter(entry -> organizationUuidsToBeRemoved.contains(entry.getKey()))
      .forEach(entry -> removeMemberInDb(dbSession, entry.getValue(), user));
  }

  private void removeMemberInDb(DbSession dbSession, OrganizationDto organization, UserDto user) {
    String userUuid = user.getUuid();
    String organizationUuid = organization.getUuid();
    dbClient.propertiesDao().deleteByOrganizationAndUser(dbSession, organizationUuid, userUuid);
    dbClient.propertiesDao().deleteByOrganizationAndMatchingLogin(dbSession, organizationUuid, user.getLogin(), singletonList(DEFAULT_ISSUE_ASSIGNEE));

    dbClient.organizationMemberDao().delete(dbSession, organizationUuid, userUuid);
  }

}
