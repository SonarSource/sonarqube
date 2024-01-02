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
package org.sonar.server.user.ws;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class UserDeactivator {
  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final UserSession userSession;
  private final UserAnonymizer userAnonymizer;

  public UserDeactivator(DbClient dbClient, UserIndexer userIndexer, UserSession userSession, UserAnonymizer userAnonymizer) {
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.userSession = userSession;
    this.userAnonymizer = userAnonymizer;
  }

  public UserDto deactivateUser(DbSession dbSession, String login) {
    UserDto user = doBeforeDeactivation(dbSession, login);
    deactivateUser(dbSession, user);
    return user;
  }

  public UserDto deactivateUserWithAnonymization(DbSession dbSession, String login) {
    UserDto user = doBeforeDeactivation(dbSession, login);
    anonymizeUser(dbSession, user);
    deactivateUser(dbSession, user);
    return user;
  }

  private UserDto doBeforeDeactivation(DbSession dbSession, String login) {
    checkRequest(!login.equals(userSession.getLogin()), "Self-deactivation is not possible");
    UserDto user = getUserOrThrow(dbSession, login);
    ensureNotLastAdministrator(dbSession, user);
    deleteRelatedData(dbSession, user);
    return user;
  }

  private UserDto getUserOrThrow(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    return checkFound(user, "User '%s' doesn't exist", login);
  }

  private void ensureNotLastAdministrator(DbSession dbSession, UserDto user) {
    boolean isLastAdmin = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUser(dbSession, ADMINISTER.getKey(), user.getUuid()) == 0;
    checkRequest(!isLastAdmin, "User is last administrator, and cannot be deactivated");
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

  private void deactivateUser(DbSession dbSession, UserDto user) {
    dbClient.userDao().deactivateUser(dbSession, user);
    userIndexer.commitAndIndex(dbSession, user);
  }
}
