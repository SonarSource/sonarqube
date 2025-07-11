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
package org.sonar.server.organization.ws;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.difference;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.usergroups.DefaultGroupFinder;

public class MemberUpdater {

  private final DbClient dbClient;
  private final DefaultGroupFinder defaultGroupFinder;
  private final BillingValidationsProxy billingValidations;

  public MemberUpdater(DbClient dbClient, DefaultGroupFinder defaultGroupFinder, BillingValidationsProxy billingValidations) {
    this.dbClient = dbClient;
    this.defaultGroupFinder = defaultGroupFinder;
    this.billingValidations = billingValidations;
  }

  public enum MemberType {
    STANDARD,
    PLATFORM;
  }

  public void addMember(DbSession dbSession, OrganizationDto organization, UserDto user, MemberType type) {
    addMembers(dbSession, organization, singletonList(user), type);
  }

  public void addMembers(DbSession dbSession, OrganizationDto organization, List<UserDto> users, MemberType type) {
    Set<String> currentMemberUuids = new HashSet<>(dbClient.organizationMemberDao().selectUserUuidsByOrganizationUuid(dbSession, organization.getUuid()));
    List<UserDto> usersToAdd = users.stream()
        .filter(UserDto::isActive)
        .filter(u -> !currentMemberUuids.contains(u.getUuid()))
        .filter(u -> canAddMember(organization, u))
        .toList();
    if (usersToAdd.isEmpty()) {
      return;
    }
    usersToAdd.forEach(u -> addMemberInDb(dbSession, organization, u, type));
  }

  private boolean canAddMember(OrganizationDto organization, UserDto user) {
    try {
      billingValidations.checkBeforeAddMember(new BillingValidations.Organization(organization.getKey(), organization.getUuid(), organization.getName()),
              new BillingValidations.User(user.getUuid(), user.getLogin(), user.getEmail()));
      return true;
    } catch (BillingValidations.BillingValidationsException e) {
      return false;
    }
  }

  private void addMemberInDb(DbSession dbSession, OrganizationDto organization, UserDto user, MemberType type) {
    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto()
      .setOrganizationUuid(organization.getUuid())
      .setUserUuid(user.getUuid())
      .setType(type.name()));
    GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession, organization.getUuid());
    UserGroupDto userGroup = new UserGroupDto()
            .setGroupUuid(defaultGroup.getUuid())
            .setUserUuid(user.getUuid());
    dbClient.userGroupDao().insert(dbSession, userGroup, defaultGroup.getName(), user.getLogin(), organization.getUuid());
  }

  public void removeMember(DbSession dbSession, OrganizationDto organization, UserDto user) {
    removeMembers(dbSession, organization, singletonList(user));
  }

  public void removeMembers(DbSession dbSession, OrganizationDto organization, List<UserDto> users) {
    Set<String> currentMemberIds = new HashSet<>(dbClient.organizationMemberDao().selectUserUuidsByOrganizationUuid(dbSession, organization.getUuid()));
    List<UserDto> usersToRemove = users.stream()
            .filter(UserDto::isActive)
            .filter(u -> currentMemberIds.contains(u.getUuid()))
            .toList();
    if (usersToRemove.isEmpty()) {
      return;
    }

    Set<String> userUuidsToRemove = usersToRemove.stream().map(UserDto::getUuid).collect(toSet());
    Set<String> adminUuids = new HashSet<>(dbClient.authorizationDao().selectUserUuidsWithGlobalPermission(dbSession, organization.getUuid(), ADMINISTER.getKey()));
    checkArgument(!difference(adminUuids, userUuidsToRemove).isEmpty(), "The last administrator member cannot be removed");

    usersToRemove.forEach(u -> removeMemberInDb(dbSession, organization, u));
    dbSession.commit();
  }

  private void removeMemberInDb(DbSession dbSession, OrganizationDto organization, UserDto user) {
    String userUuid = user.getUuid();
    String organizationUuid = organization.getUuid();
    dbClient.userPermissionDao().deleteOrganizationMemberPermissions(dbSession, organizationUuid, userUuid);
    dbClient.permissionTemplateDao().deleteUserPermissionsByOrganization(dbSession, organizationUuid, userUuid);
    dbClient.qProfileEditUsersDao().deleteByOrganizationAndUser(dbSession, organization, user);
    dbClient.userGroupDao().deleteByOrganizationAndUser(dbSession, organizationUuid, userUuid);
    dbClient.propertiesDao().deleteByOrganizationAndUser(dbSession, organizationUuid, userUuid);
    dbClient.propertiesDao().deleteByOrganizationAndMatchingLogin(dbSession, organizationUuid, user.getLogin(), singletonList(DEFAULT_ISSUE_ASSIGNEE));
    dbClient.ideUsageDao().deleteByOrganizationAndUser(dbSession, organizationUuid, userUuid);

    dbClient.organizationMemberDao().delete(dbSession, organizationUuid, userUuid);
  }
}
