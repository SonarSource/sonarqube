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
package org.sonar.server.common.user.service;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.user.UserDeactivator;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.sonar.auth.ldap.LdapRealm.LDAP_SECURITY_REALM;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.user.ExternalIdentity.SQ_AUTHORITY;

public class UserService {

  private static final String USER_NOT_FOUND_MESSAGE = "User '%s' not found";
  private final DbClient dbClient;
  private final AvatarResolver avatarResolver;
  private final ManagedInstanceService managedInstanceService;
  private final ManagedInstanceChecker managedInstanceChecker;
  private final UserDeactivator userDeactivator;
  private final UserUpdater userUpdater;
  private final IdentityProviderRepository identityProviderRepository;

  public UserService(
    DbClient dbClient,
    AvatarResolver avatarResolver,
    ManagedInstanceService managedInstanceService,
    ManagedInstanceChecker managedInstanceChecker,
    UserDeactivator userDeactivator,
    UserUpdater userUpdater,
    IdentityProviderRepository identityProviderRepository) {
    this.dbClient = dbClient;
    this.avatarResolver = avatarResolver;
    this.managedInstanceService = managedInstanceService;
    this.managedInstanceChecker = managedInstanceChecker;
    this.userDeactivator = userDeactivator;
    this.userUpdater = userUpdater;
    this.identityProviderRepository = identityProviderRepository;
  }

  public SearchResults<UserInformation> findUsers(UsersSearchRequest request) {
    UserQuery userQuery = buildUserQuery(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      int totalUsers = dbClient.userDao().countUsers(dbSession, userQuery);
      if (request.getPageSize() == 0) {
        return new SearchResults<>(List.of(), totalUsers);
      }
      List<UserInformation> searchResults = performSearch(dbSession, userQuery, request.getPage(), request.getPageSize());
      return new SearchResults<>(searchResults, totalUsers);
    }
  }

  private UserQuery buildUserQuery(UsersSearchRequest request) {
    UserQuery.UserQueryBuilder builder = UserQuery.builder();
    request.getLastConnectionDateFrom().ifPresent(builder::lastConnectionDateFrom);
    request.getLastConnectionDateTo().ifPresent(builder::lastConnectionDateTo);
    request.getSonarLintLastConnectionDateFrom().ifPresent(builder::sonarLintLastConnectionDateFrom);
    request.getSonarLintLastConnectionDateTo().ifPresent(builder::sonarLintLastConnectionDateTo);
    request.getExternalLogin().ifPresent(builder::externalLogin);
    request.getGroupUuid().ifPresent(builder::groupUuid);
    request.getExcludedGroupUuid().ifPresent(builder::excludedGroupUuid);
    if (managedInstanceService.isInstanceExternallyManaged()) {
      String managedInstanceSql = Optional.ofNullable(request.isManaged())
        .map(managedInstanceService::getManagedUsersSqlFilter)
        .orElse(null);
      builder.isManagedClause(managedInstanceSql);
    } else if (request.isManaged() != null) {
      throw BadRequestException.create("The 'managed' parameter is only available for managed instances.");
    }
    if (request.getOrganizationUuids() != null) {
      builder.addOrganizationUuids(request.getOrganizationUuids());
    }
    return builder
      .isActive(!request.isDeactivated())
      .searchText(request.getQuery())
      .build();
  }

  private List<UserInformation> performSearch(DbSession dbSession, UserQuery userQuery, int pageIndex, int pageSize) {
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
    return Optional.ofNullable(emptyToNull(userDto.getEmail())).map(email -> avatarResolver.create(userDto));
  }

  private static Set<String> getUserUuids(List<UserDto> users) {
    return users.stream().map(UserDto::getUuid).collect(Collectors.toSet());
  }

  public UserDto deactivate(String uuid, Boolean anonymize) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = findUserOrThrow(uuid, dbSession);
      managedInstanceChecker.throwIfUserIsManaged(dbSession, uuid);
      UserDto deactivatedUser;
      if (Boolean.TRUE.equals(anonymize)) {
        deactivatedUser = userDeactivator.deactivateUserWithAnonymization(dbSession, userDto);
      } else {
        deactivatedUser = userDeactivator.deactivateUser(dbSession, userDto);
      }
      dbSession.commit();
      return deactivatedUser;
    }
  }

  private UserInformation toUserSearchResult(Collection<String> groups, int tokenCount, boolean managed, UserDto userDto) {
    return new UserInformation(
      userDto,
      managed,
      findAvatar(userDto),
      groups,
      tokenCount);
  }

  public UserInformation createUser(UserCreateRequest userCreateRequest) {
    managedInstanceChecker.throwIfInstanceIsManaged();
    List<String> scmAccounts = userCreateRequest.getScmAccounts().orElse(new ArrayList<>());
    validateScmAccounts(scmAccounts);
    try (DbSession dbSession = dbClient.openSession(false)) {
      String login = userCreateRequest.getLogin();
      NewUser.Builder newUserBuilder = NewUser.builder()
        .setLogin(login)
        .setName(userCreateRequest.getName())
        .setEmail(userCreateRequest.getEmail().orElse(null))
        .setScmAccounts(scmAccounts)
        .setPassword(userCreateRequest.getPassword().orElse(null));
      if (Boolean.FALSE.equals(userCreateRequest.isLocal())) {
        newUserBuilder.setExternalIdentity(new ExternalIdentity(SQ_AUTHORITY, login, login));
      }
      return registerUser(dbSession, newUserBuilder.build());
    }
  }

  private UserInformation registerUser(DbSession dbSession, NewUser newUser) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, requireNonNull(newUser.login()));
    if (user == null) {
      user = userUpdater.createAndCommit(dbSession, newUser, u -> {
      });
    } else {
      checkArgument(!user.isActive(), "An active user with login '%s' already exists", user.getLogin());
      user = userUpdater.reactivateAndCommit(dbSession, user, newUser, u -> {
      });
    }
    return fetchUser(user.getUuid());
  }

  public static void validateScmAccounts(List<String> scmAccounts) {
    scmAccounts.forEach(UserService::validateScmAccountFormat);
    validateNoDuplicates(scmAccounts);
  }

  private static void validateScmAccountFormat(String scmAccount) {
    checkArgument(scmAccount.equals(scmAccount.strip()), "SCM account cannot start or end with whitespace: '%s'", scmAccount);
  }

  private static void validateNoDuplicates(List<String> scmAccounts) {
    Set<String> duplicateCheck = new HashSet<>();
    for (String account : scmAccounts) {
      checkArgument(duplicateCheck.add(account), "Duplicate SCM account: '%s'", account);
    }
  }

  public UserInformation updateUser(String uuid, UpdateUser updateUser) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfInvalidChangeOfExternalProvider(updateUser);
      UserDto userDto = findUserOrThrow(uuid, dbSession);
      userUpdater.updateAndCommit(dbSession, userDto, updateUser, u -> {
      });
      return fetchUser(userDto.getUuid());
    }
  }

  public UserInformation fetchUser(String uuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = findUserOrThrow(uuid, dbSession);
      Collection<String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, Set.of(userDto.getLogin())).get(userDto.getLogin());
      int tokenCount = dbClient.userTokenDao().selectByUser(dbSession, userDto).size();
      boolean isManaged = managedInstanceService.isUserManaged(dbSession, uuid);
      return toUserSearchResult(groups, tokenCount, isManaged, userDto);
    }
  }

  private UserDto findUserOrThrow(String uuid, DbSession dbSession) {
    return checkFound(dbClient.userDao().selectByUuid(dbSession, uuid), USER_NOT_FOUND_MESSAGE, uuid);
  }

  private void throwIfInvalidChangeOfExternalProvider(UpdateUser updateUser) {
    Optional.ofNullable(updateUser.externalIdentityProvider()).ifPresent(this::assertProviderIsSupported);
  }

  private void assertProviderIsSupported(String newExternalProvider) {
    List<String> allowedIdentityProviders = getAvailableIdentityProviders();

    boolean isAllowedProvider = allowedIdentityProviders.contains(newExternalProvider) || isLdapIdentityProvider(newExternalProvider);
    checkArgument(isAllowedProvider, "Value of 'externalProvider' (%s) must be one of: [%s] or [%s]", newExternalProvider,
      String.join(", ", allowedIdentityProviders), String.join(", ", "LDAP", "LDAP_{serverKey}"));
  }

  private List<String> getAvailableIdentityProviders() {
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .map(IdentityProvider::getKey)
      .toList();
  }

  private static boolean isLdapIdentityProvider(String identityProviderKey) {
    return identityProviderKey.startsWith(LDAP_SECURITY_REALM);
  }

}
