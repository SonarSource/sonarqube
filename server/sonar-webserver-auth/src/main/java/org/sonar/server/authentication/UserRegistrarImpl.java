/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.server.user.UserSession.IdentityProvider.SONARQUBE;

public class UserRegistrarImpl implements UserRegistrar {

  public static final String SQ_AUTHORITY = "sonarqube";
  public static final String LDAP_PROVIDER_PREFIX = "LDAP_";
  private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistrarImpl.class);
  public static final String GITHUB_PROVIDER = "github";
  public static final String GITLAB_PROVIDER = "gitlab";

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final DefaultGroupFinder defaultGroupFinder;
  private final ManagedInstanceService managedInstanceService;

  public UserRegistrarImpl(DbClient dbClient, UserUpdater userUpdater, DefaultGroupFinder defaultGroupFinder,
    ManagedInstanceService managedInstanceService) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.defaultGroupFinder = defaultGroupFinder;
    this.managedInstanceService = managedInstanceService;
  }

  @Override
  public UserDto register(UserRegistration registration) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = getUser(dbSession, registration.getUserIdentity(), registration.getProvider(), registration.getSource());
      if (userDto == null) {
        return registerNewUser(dbSession, null, registration);
      }
      if (!userDto.isActive()) {
        return registerNewUser(dbSession, userDto, registration);
      }
      return updateExistingUser(dbSession, userDto, registration);
    }
  }

  @CheckForNull
  private UserDto getUser(DbSession dbSession, UserIdentity userIdentity, IdentityProvider provider, Source source) {
    // First, try to authenticate using the external ID
    // Then, try with the external login, for instance when external ID has changed or is not used by the provider
    return retrieveUserByExternalIdAndIdentityProvider(dbSession, userIdentity, provider)
      .or(() -> retrieveUserByExternalLoginAndIdentityProvider(dbSession, userIdentity, provider, source))
      .or(() -> retrieveUserByLogin(dbSession, userIdentity, provider))
      .orElse(null);
  }

  private Optional<UserDto> retrieveUserByExternalIdAndIdentityProvider(DbSession dbSession, UserIdentity userIdentity, IdentityProvider provider) {
    return Optional.ofNullable(dbClient.userDao().selectByExternalIdAndIdentityProvider(dbSession, getProviderIdOrProviderLogin(userIdentity), provider.getKey()));
  }

  private Optional<UserDto> retrieveUserByExternalLoginAndIdentityProvider(DbSession dbSession, UserIdentity userIdentity, IdentityProvider provider, Source source) {
    return Optional.ofNullable(dbClient.userDao().selectByExternalLoginAndIdentityProvider(dbSession, userIdentity.getProviderLogin(), provider.getKey()))
      .filter(user -> validateAlmSpecificData(user, provider.getKey(), userIdentity, source));
  }

  private Optional<UserDto> retrieveUserByLogin(DbSession dbSession, UserIdentity userIdentity, IdentityProvider provider) {
    return Optional.ofNullable(dbClient.userDao().selectByLogin(dbSession, userIdentity.getProviderLogin()))
      .filter(user -> shouldPerformLdapIdentityProviderMigration(user, provider));
  }

  private static boolean shouldPerformLdapIdentityProviderMigration(UserDto user, IdentityProvider identityProvider) {
    boolean isLdapIdentityProvider = identityProvider.getKey().startsWith(LDAP_PROVIDER_PREFIX);
    boolean hasSonarQubeExternalIdentityProvider = SONARQUBE.getKey().equals(user.getExternalIdentityProvider());

    return isLdapIdentityProvider && hasSonarQubeExternalIdentityProvider && !user.isLocal();
  }

  private static boolean validateAlmSpecificData(UserDto user, String key, UserIdentity userIdentity, Source source) {
    // All gitlab users have an external ID, so the other two authentication methods should never be used
    if (GITLAB_PROVIDER.equals(key)) {
      throw failAuthenticationException(userIdentity, source);
    }

    if (GITHUB_PROVIDER.equals(key)) {
      validateEmailToAvoidLoginRecycling(userIdentity, user, source);
    }

    return true;
  }

  private static void validateEmailToAvoidLoginRecycling(UserIdentity userIdentity, UserDto user, Source source) {
    String dbEmail = user.getEmail();

    if (dbEmail == null) {
      return;
    }

    String externalEmail = userIdentity.getEmail();

    if (!dbEmail.equalsIgnoreCase(externalEmail)) {
      LOGGER.warn("User with login '{}' tried to login with email '{}' which doesn't match the email on record '{}'", userIdentity.getProviderLogin(), externalEmail, dbEmail);
      throw failAuthenticationException(userIdentity, source);
    }
  }

  private static AuthenticationException failAuthenticationException(UserIdentity userIdentity, Source source) {
    String message = String.format("Failed to authenticate with login '%s'", userIdentity.getProviderLogin());
    return authException(userIdentity, source, message, message);
  }

  private static AuthenticationException authException(UserIdentity userIdentity, Source source, String message, String publicMessage) {
    return AuthenticationException.newBuilder()
      .setSource(source)
      .setLogin(userIdentity.getProviderLogin())
      .setMessage(message)
      .setPublicMessage(publicMessage)
      .build();
  }

  private UserDto registerNewUser(DbSession dbSession, @Nullable UserDto disabledUser, UserRegistration authenticatorParameters) {
    blockUnmanagedUserCreationOnManagedInstance(authenticatorParameters);
    Optional<UserDto> otherUserToIndex = detectEmailUpdate(dbSession, authenticatorParameters, disabledUser != null ? disabledUser.getUuid() : null);
    NewUser newUser = createNewUser(authenticatorParameters);
    if (disabledUser == null) {
      return userUpdater.createAndCommit(dbSession, newUser, beforeCommit(dbSession, authenticatorParameters), toArray(otherUserToIndex));
    }
    return userUpdater.reactivateAndCommit(dbSession, disabledUser, newUser, beforeCommit(dbSession, authenticatorParameters), toArray(otherUserToIndex));
  }

  private void blockUnmanagedUserCreationOnManagedInstance(UserRegistration userRegistration) {
    if (managedInstanceService.isInstanceExternallyManaged() && !userRegistration.managed()) {
      throw AuthenticationException.newBuilder()
        .setMessage("No account found for this user. As the instance is managed, make sure to provision the user from your IDP.")
        .setPublicMessage("You have no account on SonarQube. Please make sure with your administrator that your account is provisioned.")
        .setLogin(userRegistration.getUserIdentity().getProviderLogin())
        .setSource(userRegistration.getSource())
        .build();
    }
  }

  private UserDto updateExistingUser(DbSession dbSession, UserDto userDto, UserRegistration authenticatorParameters) {
    UpdateUser update = new UpdateUser()
      .setEmail(authenticatorParameters.getUserIdentity().getEmail())
      .setName(authenticatorParameters.getUserIdentity().getName())
      .setExternalIdentityProvider(authenticatorParameters.getProvider().getKey())
      .setExternalIdentityProviderId(authenticatorParameters.getUserIdentity().getProviderId())
      .setExternalIdentityProviderLogin(authenticatorParameters.getUserIdentity().getProviderLogin());
    Optional<UserDto> otherUserToIndex = detectEmailUpdate(dbSession, authenticatorParameters, userDto.getUuid());
    userUpdater.updateAndCommit(dbSession, userDto, update, beforeCommit(dbSession, authenticatorParameters), toArray(otherUserToIndex));
    return userDto;
  }

  private Consumer<UserDto> beforeCommit(DbSession dbSession, UserRegistration authenticatorParameters) {
    return user -> syncGroups(dbSession, authenticatorParameters.getUserIdentity(), user);
  }

  private Optional<UserDto> detectEmailUpdate(DbSession dbSession, UserRegistration authenticatorParameters, @Nullable String authenticatingUserUuid) {
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
    if (existingUser == null || existingUser.getUuid().equals(authenticatingUserUuid)) {
      return Optional.empty();
    }
    throw generateExistingEmailError(authenticatorParameters, email);
  }

  private void syncGroups(DbSession dbSession, UserIdentity userIdentity, UserDto userDto) {
    if (!userIdentity.shouldSyncGroups()) {
      return;
    }
    String userLogin = userDto.getLogin();
    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userLogin)).get(userLogin));
    Set<String> identityGroups = userIdentity.getGroups();
    LOGGER.debug("List of groups returned by the identity provider '{}'", identityGroups);

    Collection<String> groupsToAdd = Sets.difference(identityGroups, userGroups);
    Collection<String> groupsToRemove = Sets.difference(userGroups, identityGroups);
    Collection<String> allGroups = new ArrayList<>(groupsToAdd);
    allGroups.addAll(groupsToRemove);
    Map<String, GroupDto> groupsByName = dbClient.groupDao().selectByNames(dbSession, allGroups)
      .stream()
      .collect(Collectors.toMap(GroupDto::getName, Function.identity()));

    addGroups(dbSession, userDto, groupsToAdd, groupsByName);

    if (shouldRemoveGroups()) {
      removeGroups(dbSession, userDto, groupsToRemove, groupsByName);
    }
  }

  private boolean shouldRemoveGroups() {
    return !managedInstanceService.isInstanceExternallyManaged()
      || !GITHUB_PROVIDER.equals(managedInstanceService.getProviderName());
  }

  private void addGroups(DbSession dbSession, UserDto userDto, Collection<String> groupsToAdd, Map<String, GroupDto> groupsByName) {
    groupsToAdd.stream().map(groupsByName::get).filter(Objects::nonNull).forEach(
      groupDto -> {
        LOGGER.debug("Adding user '{}' to group '{}'", userDto.getLogin(), groupDto.getName());
        dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setGroupUuid(groupDto.getUuid()).setUserUuid(userDto.getUuid()),
          groupDto.getName(), userDto.getLogin());
      });
  }

  private void removeGroups(DbSession dbSession, UserDto userDto, Collection<String> groupsToRemove, Map<String, GroupDto> groupsByName) {
    Optional<GroupDto> defaultGroup = getDefaultGroup(dbSession);
    groupsToRemove.stream().map(groupsByName::get)
      .filter(Objects::nonNull)
      // user should be member of default group only when organizations are disabled, as the IdentityProvider API doesn't handle yet
      // organizations
      .filter(group -> defaultGroup.isEmpty() || !group.getUuid().equals(defaultGroup.get().getUuid()))
      .forEach(groupDto -> {
        LOGGER.debug("Removing group '{}' from user '{}'", groupDto.getName(), userDto.getLogin());
        dbClient.userGroupDao().delete(dbSession, groupDto, userDto);
      });
  }

  private Optional<GroupDto> getDefaultGroup(DbSession dbSession) {
    return Optional.of(defaultGroupFinder.findDefaultGroup(dbSession));
  }

  private NewUser createNewUser(UserRegistration authenticatorParameters) {
    String identityProviderKey = authenticatorParameters.getProvider().getKey();
    if (!managedInstanceService.isInstanceExternallyManaged() && !authenticatorParameters.getProvider().allowsUsersToSignUp()) {
      throw AuthenticationException.newBuilder()
        .setSource(authenticatorParameters.getSource())
        .setLogin(authenticatorParameters.getUserIdentity().getProviderLogin())
        .setMessage(format("User signup disabled for provider '%s'", identityProviderKey))
        .setPublicMessage(format("'%s' users are not allowed to sign up", identityProviderKey))
        .build();
    }
    String providerLogin = authenticatorParameters.getUserIdentity().getProviderLogin();
    return NewUser.builder()
      .setLogin(SQ_AUTHORITY.equals(identityProviderKey) ? providerLogin : null)
      .setEmail(authenticatorParameters.getUserIdentity().getEmail())
      .setName(authenticatorParameters.getUserIdentity().getName())
      .setExternalIdentity(
        new ExternalIdentity(
          identityProviderKey,
          providerLogin,
          authenticatorParameters.getUserIdentity().getProviderId()))
      .build();
  }

  private static UserDto[] toArray(Optional<UserDto> userDto) {
    return userDto.map(u -> new UserDto[]{u}).orElse(new UserDto[]{});
  }

  private static AuthenticationException generateExistingEmailError(UserRegistration authenticatorParameters, String email) {
    return AuthenticationException.newBuilder()
      .setSource(authenticatorParameters.getSource())
      .setLogin(authenticatorParameters.getUserIdentity().getProviderLogin())
      .setMessage(format("Email '%s' is already used", email))
      .setPublicMessage(
        "This account is already associated with another authentication method. "
          + "Sign in using the current authentication method, "
          + "or contact your administrator to transfer your account to a different authentication method.")
      .build();
  }

  private static String getProviderIdOrProviderLogin(UserIdentity userIdentity) {
    String providerId = userIdentity.getProviderId();
    return providerId == null ? userIdentity.getProviderLogin() : providerId;
  }

}
