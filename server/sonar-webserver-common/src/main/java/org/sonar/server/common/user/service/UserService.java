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
package org.sonar.server.common.user.service;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.user.UserDeactivator;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;

import static java.util.Comparator.comparing;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class UserService {

  private final DbClient dbClient;
  private final AvatarResolver avatarResolver;
  private final ManagedInstanceService managedInstanceService;
  private final ManagedInstanceChecker managedInstanceChecker;
  private final UserDeactivator userDeactivator;

  public UserService(
    DbClient dbClient,
    AvatarResolver avatarResolver,
    ManagedInstanceService managedInstanceService,
    ManagedInstanceChecker managedInstanceChecker,
    UserDeactivator userDeactivator) {
    this.dbClient = dbClient;
    this.avatarResolver = avatarResolver;
    this.managedInstanceService = managedInstanceService;
    this.managedInstanceChecker = managedInstanceChecker;
    this.userDeactivator = userDeactivator;
  }

  public SearchResults<UserSearchResult> findUsers(UsersSearchRequest request) {
    UserQuery userQuery = buildUserQuery(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      int totalUsers = dbClient.userDao().countUsers(dbSession, userQuery);

      List<UserSearchResult> searchResults = performSearch(dbSession, userQuery, request.getPage(), request.getPageSize());
      return new SearchResults<>(searchResults, totalUsers);
    }
  }

  private UserQuery buildUserQuery(UsersSearchRequest request) {
    UserQuery.UserQueryBuilder builder = UserQuery.builder();
    request.getLastConnectionDateFrom().ifPresent(builder::lastConnectionDateFrom);
    request.getLastConnectionDateTo().ifPresent(builder::lastConnectionDateTo);
    request.getSonarLintLastConnectionDateFrom().ifPresent(builder::sonarLintLastConnectionDateFrom);
    request.getSonarLintLastConnectionDateTo().ifPresent(builder::sonarLintLastConnectionDateTo);

    if (managedInstanceService.isInstanceExternallyManaged()) {
      String managedInstanceSql = Optional.ofNullable(request.isManaged())
        .map(managedInstanceService::getManagedUsersSqlFilter)
        .orElse(null);
      builder.isManagedClause(managedInstanceSql);
    } else if (request.isManaged() != null) {
      throw BadRequestException.create("The 'managed' parameter is only available for managed instances.");
    }

    return builder
      .isActive(!request.isDeactivated())
      .searchText(request.getQuery())
      .build();
  }

  private List<UserSearchResult> performSearch(DbSession dbSession, UserQuery userQuery, int pageIndex, int pageSize) {
    List<UserDto> userDtos = findUsersAndSortByLogin(dbSession, userQuery, pageIndex, pageSize);
    List<String> logins = userDtos.stream().map(UserDto::getLogin).toList();
    Multimap<String, String> groupsByLogin = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, logins);
    Map<String, Integer> tokenCountsByLogin = dbClient.userTokenDao().countTokensByUsers(dbSession, userDtos);
    Map<String, Boolean> userUuidToIsManaged = managedInstanceService.getUserUuidToManaged(dbSession, getUserUuids(userDtos));
    return userDtos.stream()
      .map(userDto -> toUserSearchResult(
        groupsByLogin.get(userDto.getLogin()),
        tokenCountsByLogin.getOrDefault(userDto.getUuid(), 0),
        userUuidToIsManaged.getOrDefault(userDto.getUuid(), false),
        userDto))
      .toList();
  }

  private List<UserDto> findUsersAndSortByLogin(DbSession dbSession, UserQuery userQuery, int page, int pageSize) {
    return dbClient.userDao().selectUsers(dbSession, userQuery, page, pageSize)
      .stream()
      .sorted(comparing(UserDto::getLogin))
      .toList();
  }

  private Optional<String> findAvatar(UserDto userDto) {
    return Optional.ofNullable(userDto.getEmail()).map(email -> avatarResolver.create(userDto));
  }

  private static Set<String> getUserUuids(List<UserDto> users) {
    return users.stream().map(UserDto::getUuid).collect(Collectors.toSet());
  }

  public UserDto deactivate(String login, Boolean anonymize) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' not found", login);
      managedInstanceChecker.throwIfUserIsManaged(dbSession, userDto.getUuid());
      UserDto deactivatedUser;
      if (Boolean.TRUE.equals(anonymize)) {
        deactivatedUser = userDeactivator.deactivateUserWithAnonymization(dbSession, login);
      } else {
        deactivatedUser = userDeactivator.deactivateUser(dbSession, login);
      }
      dbSession.commit();
      return deactivatedUser;
    }
  }

  public UserSearchResult fetchUser(String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' not found", login);
      Collection<String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, Set.of(login)).get(login);
      int tokenCount = dbClient.userTokenDao().selectByUser(dbSession, userDto).size();
      boolean isManaged = managedInstanceService.isUserManaged(dbSession, userDto.getUuid());
      return toUserSearchResult(groups, tokenCount, isManaged, userDto);
    }
  }

  private UserSearchResult toUserSearchResult(Collection<String> groups, int tokenCount, boolean managed, UserDto userDto) {
    return new UserSearchResult(
      userDto,
      managed,
      findAvatar(userDto),
      groups,
      tokenCount);
  }
}
