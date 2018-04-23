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
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.organization.DefaultOrganization;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class UserIdentityAuthenticator {

  /**
   * Strategy to be executed when the email of the user is already used by another user
   */
  enum ExistingEmailStrategy {
    /**
     * Authentication is allowed, the email is moved from other user to current user
     */
    ALLOW,
    /**
     * Authentication process is stopped, the user is redirected to a page explaining that the email is already used
     */
    WARN,
    /**
     * Forbid authentication of the user
     */
    FORBID
  }

  private static final Logger LOGGER = Loggers.get(UserIdentityAuthenticator.class);

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;
  private final DefaultGroupFinder defaultGroupFinder;

  public UserIdentityAuthenticator(DbClient dbClient, UserUpdater userUpdater, DefaultOrganizationProvider defaultOrganizationProvider, OrganizationFlags organizationFlags,
    DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationFlags = organizationFlags;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  public UserDto authenticate(UserIdentity userIdentity, IdentityProvider provider, AuthenticationEvent.Source source, ExistingEmailStrategy existingEmailStrategy) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = getUser(dbSession, userIdentity, provider);
      if (userDto == null) {
        return registerNewUser(dbSession, null, userIdentity, provider, source, existingEmailStrategy);
      }
      if (!userDto.isActive()) {
        return registerNewUser(dbSession, userDto, userIdentity, provider, source, existingEmailStrategy);
      }
      return registerExistingUser(dbSession, userDto, userIdentity, provider, source, existingEmailStrategy);
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

  private UserDto registerExistingUser(DbSession dbSession, UserDto userDto, UserIdentity identity, IdentityProvider provider, AuthenticationEvent.Source source,
    ExistingEmailStrategy existingEmailStrategy) {
    UpdateUser update = new UpdateUser()
      .setLogin(identity.getLogin())
      .setEmail(identity.getEmail())
      .setName(identity.getName())
      .setExternalIdentity(new ExternalIdentity(provider.getKey(), identity.getProviderLogin(), identity.getProviderId()));
    Optional<UserDto> otherUserToIndex = validateEmail(dbSession, identity, provider, source, existingEmailStrategy);
    userUpdater.updateAndCommit(dbSession, userDto, update, u -> syncGroups(dbSession, identity, u), toArray(otherUserToIndex));
    return userDto;
  }

  private UserDto registerNewUser(DbSession dbSession, @Nullable UserDto disabledUser, UserIdentity identity, IdentityProvider provider, AuthenticationEvent.Source source,
    ExistingEmailStrategy existingEmailStrategy) {
    Optional<UserDto> otherUserToIndex = validateEmail(dbSession, identity, provider, source, existingEmailStrategy);
    NewUser newUser = createNewUser(identity, provider, source);
    if (disabledUser == null) {
      return userUpdater.createAndCommit(dbSession, newUser, u -> syncGroups(dbSession, identity, u), toArray(otherUserToIndex));
    }
    return userUpdater.reactivateAndCommit(dbSession, disabledUser, newUser, u -> syncGroups(dbSession, identity, u), toArray(otherUserToIndex));
  }

  private Optional<UserDto> validateEmail(DbSession dbSession, UserIdentity identity, IdentityProvider provider, AuthenticationEvent.Source source,
    ExistingEmailStrategy existingEmailStrategy) {
    String email = identity.getEmail();
    if (email == null) {
      return Optional.empty();
    }
    UserDto existingUser = dbClient.userDao().selectByEmail(dbSession, email);
    if (existingUser == null
      || Objects.equals(existingUser.getLogin(), identity.getLogin())
      || (Objects.equals(existingUser.getExternalId(), identity.getProviderId()) && Objects.equals(existingUser.getExternalIdentityProvider(), provider.getKey()))) {
      return Optional.empty();
    }
    switch (existingEmailStrategy) {
      case ALLOW:
        existingUser.setEmail(null);
        dbClient.userDao().update(dbSession, existingUser);
        return Optional.of(existingUser);
      case WARN:
        throw new EmailAlreadyExistsException(email, existingUser, identity, provider);
      case FORBID:
        throw AuthenticationException.newBuilder()
          .setSource(source)
          .setLogin(identity.getLogin())
          .setMessage(format("Email '%s' is already used", email))
          .setPublicMessage(format(
            "You can't sign up because email '%s' is already used by an existing user. This means that you probably already registered with another account.",
            email))
          .build();
      default:
        throw new IllegalStateException(format("Unknown strategy %s", existingEmailStrategy));
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

  private static NewUser createNewUser(UserIdentity identity, IdentityProvider provider, AuthenticationEvent.Source source) {
    if (!provider.allowsUsersToSignUp()) {
      throw AuthenticationException.newBuilder()
        .setSource(source)
        .setLogin(identity.getLogin())
        .setMessage(format("User signup disabled for provider '%s'", provider.getKey()))
        .setPublicMessage(format("'%s' users are not allowed to sign up", provider.getKey()))
        .build();
    }
    return NewUser.builder()
      .setLogin(identity.getLogin())
      .setEmail(identity.getEmail())
      .setName(identity.getName())
      .setExternalIdentity(new ExternalIdentity(provider.getKey(), identity.getProviderLogin(), identity.getProviderId()))
      .build();
  }

  private static UserDto[] toArray(Optional<UserDto> userDto) {
    return userDto.map(u -> new UserDto[] {u}).orElse(new UserDto[] {});
  }

}
