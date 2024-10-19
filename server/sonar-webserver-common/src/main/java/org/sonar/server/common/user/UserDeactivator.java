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
package org.sonar.server.common.user;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;

import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class UserDeactivator {
  private final DbClient dbClient;
  private final UserAnonymizer userAnonymizer;

  public UserDeactivator(DbClient dbClient, UserAnonymizer userAnonymizer) {
    this.dbClient = dbClient;
    this.userAnonymizer = userAnonymizer;
  }

  public UserDto deactivateUser(DbSession dbSession, UserDto userDto) {
    UserDto user = doBeforeDeactivation(dbSession, userDto);
    return deactivateUserInternal(dbSession, user);
  }

  public UserDto deactivateUserWithAnonymization(DbSession dbSession, UserDto userDto) {
    UserDto user = doBeforeDeactivation(dbSession, userDto);
    anonymizeUser(dbSession, user);
    return deactivateUserInternal(dbSession, user);
  }

  private UserDto doBeforeDeactivation(DbSession dbSession, UserDto userDto) {
    ensureNotLastAdministrator(dbSession, userDto);
    deleteRelatedData(dbSession, userDto);
    return userDto;
  }

  private void ensureNotLastAdministrator(DbSession dbSession, UserDto user) {
    boolean isLastAdmin = !selectOrganizationsWithLastAdmin(dbSession, user.getUuid()).isEmpty();
    checkRequest(!isLastAdmin, "User is last administrator, and cannot be deactivated");
  }

  public List<OrganizationDto> selectOrganizationsWithLastAdmin(DbSession dbSession, String userUuid) {
    return dbClient.organizationDao().selectByPermission(dbSession, userUuid, ADMINISTER.getKey()).stream()
            .filter(org -> isLastAdmin(dbSession, org, userUuid))
            .toList();
  }

  private boolean isLastAdmin(DbSession dbSession, OrganizationDto org, String userUuid) {
    return dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUser(dbSession, org.getUuid(), ADMINISTER.getKey(), userUuid) == 0;
  }

  private void deleteRelatedData(DbSession dbSession, UserDto user) {
    String userUuid = user.getUuid();
    dbClient.userTokenDao().deleteByUser(dbSession, user);
    dbClient.propertiesDao().deleteByKeyAndValue(dbSession, DEFAULT_ISSUE_ASSIGNEE, user.getLogin());
    dbClient.propertiesDao().deleteByQuery(dbSession, PropertyQuery.builder().setUserUuid(userUuid).build());
    dbClient.userGroupDao().deleteByUserUuid(dbSession, user);
    dbClient.userPermissionDao().deleteByUserUuid(dbSession, user);
    dbClient.permissionTemplateDao().deleteUserPermissionsByUserUuid(dbSession, userUuid, user.getLogin());
    dbClient.qProfileEditUsersDao().deleteByUser(dbSession, user);
    dbClient.almPatDao().deleteByUser(dbSession, user);
    dbClient.sessionTokensDao().deleteByUser(dbSession, user);
    dbClient.userDismissedMessagesDao().deleteByUser(dbSession, user);
    dbClient.qualityGateUserPermissionDao().deleteByUser(dbSession, user);
  }

  private void anonymizeUser(DbSession dbSession, UserDto user) {
    userAnonymizer.anonymize(dbSession, user);
    dbClient.userDao().update(dbSession, user);
    dbClient.scimUserDao().deleteByUserUuid(dbSession, user.getUuid());
  }

  private UserDto deactivateUserInternal(DbSession dbSession, UserDto user) {
    dbClient.userDao().deactivateUser(dbSession, user);
    dbSession.commit();
    return getUserOrThrow(dbSession, user.getLogin());
  }

  private UserDto getUserOrThrow(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    return checkFound(user, "User '%s' doesn't exist", login);
  }
}
