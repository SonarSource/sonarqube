/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.authentication;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.authentication.UserIdentityAuthenticatorParameters.ExistingEmailStrategy;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.authentication.exception.EmailAlreadyExistsRedirectionException;
import org.sonar.server.authentication.exception.UpdateLoginRedirectionException;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.OrganizationUpdater;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.authentication.UserIdentityAuthenticatorParameters.UpdateLoginStrategy;

public class UserIdentityAuthenticatorImpl implements UserIdentityAuthenticator {

  private static final Logger LOGGER = Loggers.get(UserIdentityAuthenticatorImpl.class);

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;
  private final OrganizationUpdater organizationUpdater;
  private final DefaultGroupFinder defaultGroupFinder;

  public UserIdentityAuthenticatorImpl(DbClient dbClient, UserUpdater userUpdater, DefaultOrganizationProvider defaultOrganizationProvider, OrganizationFlags organizationFlags,
    OrganizationUpdater organizationUpdater, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationFlags = organizationFlags;
    this.organizationUpdater = organizationUpdater;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  @Override
  public UserDto authenticate(UserIdentityAuthenticatorParameters authenticatorParameters) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = getUser(dbSession, authenticatorParameters.getUserIdentity(), authenticatorParameters.getProvider());
      if (userDto == null) {
        return registerNewUser(dbSession, null, authenticatorParameters);
      }
      if (!userDto.isActive()) {
        return registerNewUser(dbSession, userDto, authenticatorParameters);
      }
      return registerExistingUser(dbSession, userDto, authenticatorParameters);
    }
  }

  @CheckForNull
  private UserDto getUser(DbSession dbSession, UserIdentity userIdentity, IdentityProvider provider) {
    String externalId = userIdentity.getProviderId();
    UserDto user = dbClient.userDao().selectByExternalIdAndIdentityProvider(dbSession, externalId == null ? userIdentity.getProviderLogin() : externalId, provider.getKey());
    // We need to search by login because :
    // 1. external id may have not been set before,
    // 2. user may have been provisioned,
    // 3. user may have been disabled.
    return user != null ? user : dbClient.userDao().selectByLogin(dbSession, userIdentity.getLogin());
  }

  private UserDto registerExistingUser(DbSession dbSession, UserDto userDto, UserIdentityAuthenticatorParameters authenticatorParameters) {
    UpdateUser update = new UpdateUser()
      .setLogin(authenticatorParameters.getUserIdentity().getLogin())
      .setEmail(authenticatorParameters.getUserIdentity().getEmail())
      .setName(authenticatorParameters.getUserIdentity().getName())
      .setExternalIdentity(new ExternalIdentity(
        authenticatorParameters.getProvider().getKey(),
        authenticatorParameters.getUserIdentity().getProviderLogin(),
        authenticatorParameters.getUserIdentity().getProviderId()));
    detectLoginUpdate(dbSession, userDto, update, authenticatorParameters);
    Optional<UserDto> otherUserToIndex = detectEmailUpdate(dbSession, authenticatorParameters);
    userUpdater.updateAndCommit(dbSession, userDto, update, u -> syncGroups(dbSession, authenticatorParameters.getUserIdentity(), u), toArray(otherUserToIndex));
    return userDto;
  }

  private UserDto registerNewUser(DbSession dbSession, @Nullable UserDto disabledUser, UserIdentityAuthenticatorParameters authenticatorParameters) {
    Optional<UserDto> otherUserToIndex = detectEmailUpdate(dbSession, authenticatorParameters);
    NewUser newUser = createNewUser(authenticatorParameters);
    if (disabledUser == null) {
      return userUpdater.createAndCommit(dbSession, newUser, u -> syncGroups(dbSession, authenticatorParameters.getUserIdentity(), u), toArray(otherUserToIndex));
    }
    return userUpdater.reactivateAndCommit(dbSession, disabledUser, newUser, u -> syncGroups(dbSession, authenticatorParameters.getUserIdentity(), u), toArray(otherUserToIndex));
  }

  private Optional<UserDto> detectEmailUpdate(DbSession dbSession, UserIdentityAuthenticatorParameters authenticatorParameters) {
    String email = authenticatorParameters.getUserIdentity().getEmail();
    if (email == null) {
      return Optional.empty();
    }
    List<UserDto> existingUsers = dbClient.userDao().selectByEmail(dbSession, email);
    if (existingUsers.isEmpty()) {
      return Optional.empty();
    }
    if (existingUsers.size() > 1) {
      throw generateExistingEmailError(authenticatorParameters, email);
    }

    UserDto existingUser = existingUsers.get(0);
    if (existingUser == null
      || Objects.equals(existingUser.getLogin(), authenticatorParameters.getUserIdentity().getLogin())
      || (Objects.equals(existingUser.getExternalId(), authenticatorParameters.getUserIdentity().getProviderId())
        && Objects.equals(existingUser.getExternalIdentityProvider(), authenticatorParameters.getProvider().getKey()))) {
      return Optional.empty();
    }
    ExistingEmailStrategy existingEmailStrategy = authenticatorParameters.getExistingEmailStrategy();
    switch (existingEmailStrategy) {
      case ALLOW:
        existingUser.setEmail(null);
        dbClient.userDao().update(dbSession, existingUser);
        return Optional.of(existingUser);
      case WARN:
        throw new EmailAlreadyExistsRedirectionException(email, existingUser, authenticatorParameters.getUserIdentity(), authenticatorParameters.getProvider());
      case FORBID:
        throw generateExistingEmailError(authenticatorParameters, email);
      default:
        throw new IllegalStateException(format("Unknown strategy %s", existingEmailStrategy));
    }
  }

  private void detectLoginUpdate(DbSession dbSession, UserDto user, UpdateUser update, UserIdentityAuthenticatorParameters authenticatorParameters) {
    String newLogin = update.login();
    if (!update.isLoginChanged() || user.getLogin().equals(newLogin)) {
      return;
    }
    if (!organizationFlags.isEnabled(dbSession)) {
      return;
    }
    String personalOrganizationUuid = user.getOrganizationUuid();
    if (personalOrganizationUuid == null) {
      return;
    }
    Optional<OrganizationDto> personalOrganization = dbClient.organizationDao().selectByUuid(dbSession, personalOrganizationUuid);
    checkState(personalOrganization.isPresent(),
      "Cannot find personal organization uuid '%s' for user '%s'", personalOrganizationUuid, user.getLogin());
    UpdateLoginStrategy updateLoginStrategy = authenticatorParameters.getUpdateLoginStrategy();
    switch (updateLoginStrategy) {
      case ALLOW:
        organizationUpdater.updateOrganizationKey(dbSession, personalOrganization.get(), requireNonNull(newLogin, "new login cannot be null"));
        return;
      case WARN:
        throw new UpdateLoginRedirectionException(authenticatorParameters.getUserIdentity(), authenticatorParameters.getProvider(), user, personalOrganization.get());
      default:
        throw new IllegalStateException(format("Unknown strategy %s", updateLoginStrategy));
    }
  }

  private void syncGroups(DbSession dbSession, UserIdentity userIdentity, UserDto userDto) {
    if (!userIdentity.shouldSyncGroups()) {
      return;
    }
    String userLogin = userIdentity.getLogin();
    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userLogin)).get(userLogin));
    Set<String> identityGroups = userIdentity.getGroups();
    LOGGER.debug("List of groups returned by the identity provider '{}'", identityGroups);

    Collection<String> groupsToAdd = Sets.difference(identityGroups, userGroups);
    Collection<String> groupsToRemove = Sets.difference(userGroups, identityGroups);
    Collection<String> allGroups = new ArrayList<>(groupsToAdd);
    allGroups.addAll(groupsToRemove);
    DefaultOrganization defaultOrganization = defaultOrganizationProvider.get();
    Map<String, GroupDto> groupsByName = dbClient.groupDao().selectByNames(dbSession, defaultOrganization.getUuid(), allGroups)
      .stream()
      .collect(uniqueIndex(GroupDto::getName));

    addGroups(dbSession, userDto, groupsToAdd, groupsByName);
    removeGroups(dbSession, userDto, groupsToRemove, groupsByName);
  }

  private void addGroups(DbSession dbSession, UserDto userDto, Collection<String> groupsToAdd, Map<String, GroupDto> groupsByName) {
    groupsToAdd.stream().map(groupsByName::get).filter(Objects::nonNull).forEach(
      groupDto -> {
        LOGGER.debug("Adding group '{}' to user '{}'", groupDto.getName(), userDto.getLogin());
        dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupId(groupDto.getId()).setUserId(userDto.getId()));
      });
  }

  private void removeGroups(DbSession dbSession, UserDto userDto, Collection<String> groupsToRemove, Map<String, GroupDto> groupsByName) {
    Optional<GroupDto> defaultGroup = getDefaultGroup(dbSession);
    groupsToRemove.stream().map(groupsByName::get)
      .filter(Objects::nonNull)
      // user should be member of default group only when organizations are disabled, as the IdentityProvider API doesn't handle yet
      // organizations
      .filter(group -> !defaultGroup.isPresent() || !group.getId().equals(defaultGroup.get().getId()))
      .forEach(groupDto -> {
        LOGGER.debug("Removing group '{}' from user '{}'", groupDto.getName(), userDto.getLogin());
        dbClient.userGroupDao().delete(dbSession, groupDto.getId(), userDto.getId());
      });
  }

  private Optional<GroupDto> getDefaultGroup(DbSession dbSession) {
    return organizationFlags.isEnabled(dbSession) ? Optional.empty() : Optional.of(defaultGroupFinder.findDefaultGroup(dbSession, defaultOrganizationProvider.get().getUuid()));
  }

  private static NewUser createNewUser(UserIdentityAuthenticatorParameters authenticatorParameters) {
    String identityProviderKey = authenticatorParameters.getProvider().getKey();
    if (!authenticatorParameters.getProvider().allowsUsersToSignUp()) {
      throw AuthenticationException.newBuilder()
        .setSource(authenticatorParameters.getSource())
        .setLogin(authenticatorParameters.getUserIdentity().getLogin())
        .setMessage(format("User signup disabled for provider '%s'", identityProviderKey))
        .setPublicMessage(format("'%s' users are not allowed to sign up", identityProviderKey))
        .build();
    }
    return NewUser.builder()
      .setLogin(authenticatorParameters.getUserIdentity().getLogin())
      .setEmail(authenticatorParameters.getUserIdentity().getEmail())
      .setName(authenticatorParameters.getUserIdentity().getName())
      .setExternalIdentity(
        new ExternalIdentity(
          identityProviderKey,
          authenticatorParameters.getUserIdentity().getProviderLogin(),
          authenticatorParameters.getUserIdentity().getProviderId()))
      .build();
  }

  private static UserDto[] toArray(Optional<UserDto> userDto) {
    return userDto.map(u -> new UserDto[] {u}).orElse(new UserDto[] {});
  }

  private static AuthenticationException generateExistingEmailError(UserIdentityAuthenticatorParameters authenticatorParameters, String email) {
    return AuthenticationException.newBuilder()
      .setSource(authenticatorParameters.getSource())
      .setLogin(authenticatorParameters.getUserIdentity().getLogin())
      .setMessage(format("Email '%s' is already used", email))
      .setPublicMessage(format(
        "You can't sign up because email '%s' is already used by an existing user. This means that you probably already registered with another account.",
        email))
      .build();
  }

}
