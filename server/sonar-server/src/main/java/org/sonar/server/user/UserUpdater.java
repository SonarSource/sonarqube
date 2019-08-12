/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.user;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserPropertyDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.util.Validation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.core.util.Slug.slugify;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.process.ProcessProperties.Property.ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class UserUpdater {

  public static final String NOTIFICATIONS_READ_DATE = "notifications.readDate";

  private static final String SQ_AUTHORITY = "sonarqube";

  private static final String LOGIN_PARAM = "Login";
  private static final String PASSWORD_PARAM = "Password";
  private static final String NAME_PARAM = "Name";
  private static final String EMAIL_PARAM = "Email";

  public static final int LOGIN_MIN_LENGTH = 2;
  public static final int LOGIN_MAX_LENGTH = 255;
  public static final int EMAIL_MAX_LENGTH = 100;
  public static final int NAME_MAX_LENGTH = 200;

  private final NewUserNotifier newUserNotifier;
  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;
  private final DefaultGroupFinder defaultGroupFinder;
  private final Configuration config;
  private final CredentialsLocalAuthentication localAuthentication;
  private final System2 system2;

  public UserUpdater(System2 system2, NewUserNotifier newUserNotifier, DbClient dbClient, UserIndexer userIndexer, OrganizationFlags organizationFlags,
    DefaultOrganizationProvider defaultOrganizationProvider, DefaultGroupFinder defaultGroupFinder, Configuration config,
    CredentialsLocalAuthentication localAuthentication) {
    this.system2 = system2;
    this.newUserNotifier = newUserNotifier;
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.organizationFlags = organizationFlags;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.defaultGroupFinder = defaultGroupFinder;
    this.config = config;
    this.localAuthentication = localAuthentication;
  }

  public UserDto createAndCommit(DbSession dbSession, NewUser newUser, Consumer<UserDto> beforeCommit, UserDto... otherUsersToIndex) {
    UserDto userDto = saveUser(dbSession, createDto(dbSession, newUser));
    return commitUser(dbSession, userDto, beforeCommit, otherUsersToIndex);
  }

  public UserDto reactivateAndCommit(DbSession dbSession, UserDto disabledUser, NewUser newUser, Consumer<UserDto> beforeCommit, UserDto... otherUsersToIndex) {
    checkArgument(!disabledUser.isActive(), "An active user with login '%s' already exists", disabledUser.getLogin());
    reactivateUser(dbSession, disabledUser, newUser);
    return commitUser(dbSession, disabledUser, beforeCommit, otherUsersToIndex);
  }

  private void reactivateUser(DbSession dbSession, UserDto reactivatedUser, NewUser newUser) {
    UpdateUser updateUser = new UpdateUser()
      .setName(newUser.name())
      .setEmail(newUser.email())
      .setScmAccounts(newUser.scmAccounts())
      .setExternalIdentity(newUser.externalIdentity());
    String login = newUser.login();
    if (login != null) {
      updateUser.setLogin(login);
    }
    String password = newUser.password();
    if (password != null) {
      updateUser.setPassword(password);
    }
    setOnboarded(reactivatedUser);
    updateDto(dbSession, updateUser, reactivatedUser);
    updateUser(dbSession, reactivatedUser);
    boolean isOrganizationEnabled = organizationFlags.isEnabled(dbSession);
    if (isOrganizationEnabled) {
      setNotificationsReadDate(dbSession, reactivatedUser);
    } else {
      addUserToDefaultOrganizationAndDefaultGroup(dbSession, reactivatedUser);
    }
  }

  public void updateAndCommit(DbSession dbSession, UserDto dto, UpdateUser updateUser, Consumer<UserDto> beforeCommit, UserDto... otherUsersToIndex) {
    boolean isUserUpdated = updateDto(dbSession, updateUser, dto);
    if (isUserUpdated) {
      // at least one change. Database must be updated and Elasticsearch re-indexed
      updateUser(dbSession, dto);
      commitUser(dbSession, dto, beforeCommit, otherUsersToIndex);
    } else {
      // no changes but still execute the consumer
      beforeCommit.accept(dto);
      dbSession.commit();
    }
  }

  private UserDto commitUser(DbSession dbSession, UserDto userDto, Consumer<UserDto> beforeCommit, UserDto... otherUsersToIndex) {
    beforeCommit.accept(userDto);
    userIndexer.commitAndIndex(dbSession, concat(Stream.of(userDto), stream(otherUsersToIndex)).collect(toList()));
    notifyNewUser(userDto.getLogin(), userDto.getName(), userDto.getEmail());
    return userDto;
  }

  private UserDto createDto(DbSession dbSession, NewUser newUser) {
    UserDto userDto = new UserDto();
    List<String> messages = new ArrayList<>();

    String login = newUser.login();
    if (isNullOrEmpty(login)) {
      userDto.setLogin(generateUniqueLogin(dbSession, newUser.name()));
    } else if (validateLoginFormat(login, messages)) {
      checkLoginUniqueness(dbSession, login);
      userDto.setLogin(login);
    }

    String name = newUser.name();
    if (validateNameFormat(name, messages)) {
      userDto.setName(name);
    }

    String email = newUser.email();
    if (email != null && validateEmailFormat(email, messages)) {
      userDto.setEmail(email);
    }

    String password = newUser.password();
    if (password != null && validatePasswords(password, messages)) {
      localAuthentication.storeHashPassword(userDto, password);
    }

    List<String> scmAccounts = sanitizeScmAccounts(newUser.scmAccounts());
    if (scmAccounts != null && !scmAccounts.isEmpty() && validateScmAccounts(dbSession, scmAccounts, login, email, null, messages)) {
      userDto.setScmAccounts(scmAccounts);
    }

    setExternalIdentity(dbSession, userDto, newUser.externalIdentity());
    setOnboarded(userDto);

    checkRequest(messages.isEmpty(), messages);
    return userDto;
  }

  private String generateUniqueLogin(DbSession dbSession, String userName) {
    String slugName = slugify(userName);
    for (int i = 0; i < 10; i++) {
      String login = slugName + RandomUtils.nextInt(100_000);
      UserDto existingUser = dbClient.userDao().selectByLogin(dbSession, login);
      if (existingUser == null) {
        return login;
      }
    }
    throw new IllegalStateException("Cannot create unique login for user name " + userName);
  }

  private boolean updateDto(DbSession dbSession, UpdateUser update, UserDto dto) {
    List<String> messages = newArrayList();
    boolean changed = updateLogin(dbSession, update, dto, messages);
    changed |= updateName(update, dto, messages);
    changed |= updateEmail(update, dto, messages);
    changed |= updateExternalIdentity(dbSession, update, dto);
    changed |= updatePassword(update, dto, messages);
    changed |= updateScmAccounts(dbSession, update, dto, messages);
    checkRequest(messages.isEmpty(), messages);
    return changed;
  }

  private boolean updateLogin(DbSession dbSession, UpdateUser updateUser, UserDto userDto, List<String> messages) {
    String newLogin = updateUser.login();
    if (!updateUser.isLoginChanged() || !validateLoginFormat(newLogin, messages) || Objects.equals(userDto.getLogin(), newLogin)) {
      return false;
    }
    checkLoginUniqueness(dbSession, newLogin);
    dbClient.propertiesDao().selectByKeyAndMatchingValue(dbSession, DEFAULT_ISSUE_ASSIGNEE, userDto.getLogin())
      .forEach(p -> dbClient.propertiesDao().saveProperty(p.setValue(newLogin)));
    userDto.setLogin(newLogin);
    if (userDto.isLocal()) {
      userDto.setExternalLogin(newLogin);
      userDto.setExternalId(newLogin);
    }
    return true;
  }

  private static boolean updateName(UpdateUser updateUser, UserDto userDto, List<String> messages) {
    String name = updateUser.name();
    if (updateUser.isNameChanged() && validateNameFormat(name, messages) && !Objects.equals(userDto.getName(), name)) {
      userDto.setName(name);
      return true;
    }
    return false;
  }

  private static boolean updateEmail(UpdateUser updateUser, UserDto userDto, List<String> messages) {
    String email = updateUser.email();
    if (updateUser.isEmailChanged() && validateEmailFormat(email, messages) && !Objects.equals(userDto.getEmail(), email)) {
      userDto.setEmail(email);
      return true;
    }
    return false;
  }

  private boolean updateExternalIdentity(DbSession dbSession, UpdateUser updateUser, UserDto userDto) {
    ExternalIdentity externalIdentity = updateUser.externalIdentity();
    if (updateUser.isExternalIdentityChanged() && !isSameExternalIdentity(userDto, externalIdentity)) {
      setExternalIdentity(dbSession, userDto, externalIdentity);
      return true;
    }
    return false;
  }

  private boolean updatePassword(UpdateUser updateUser, UserDto userDto, List<String> messages) {
    String password = updateUser.password();
    if (updateUser.isPasswordChanged() && validatePasswords(password, messages) && checkPasswordChangeAllowed(userDto, messages)) {
      localAuthentication.storeHashPassword(userDto, password);
      return true;
    }
    return false;
  }

  private boolean updateScmAccounts(DbSession dbSession, UpdateUser updateUser, UserDto userDto, List<String> messages) {
    String email = updateUser.email();
    List<String> scmAccounts = sanitizeScmAccounts(updateUser.scmAccounts());
    List<String> existingScmAccounts = userDto.getScmAccountsAsList();
    if (updateUser.isScmAccountsChanged() && !(existingScmAccounts.containsAll(scmAccounts) && scmAccounts.containsAll(existingScmAccounts))) {
      if (!scmAccounts.isEmpty()) {
        String newOrOldEmail = email != null ? email : userDto.getEmail();
        if (validateScmAccounts(dbSession, scmAccounts, userDto.getLogin(), newOrOldEmail, userDto, messages)) {
          userDto.setScmAccounts(scmAccounts);
        }
      } else {
        userDto.setScmAccounts((String) null);
      }
      return true;
    }
    return false;
  }

  private static boolean isSameExternalIdentity(UserDto dto, @Nullable ExternalIdentity externalIdentity) {
    return externalIdentity != null
      && !dto.isLocal()
      && Objects.equals(dto.getExternalId(), externalIdentity.getId())
      && Objects.equals(dto.getExternalLogin(), externalIdentity.getLogin())
      && Objects.equals(dto.getExternalIdentityProvider(), externalIdentity.getProvider());
  }

  private void setExternalIdentity(DbSession dbSession, UserDto dto, @Nullable ExternalIdentity externalIdentity) {
    if (externalIdentity == null) {
      dto.setExternalLogin(dto.getLogin());
      dto.setExternalIdentityProvider(SQ_AUTHORITY);
      dto.setExternalId(dto.getLogin());
      dto.setLocal(true);
    } else {
      dto.setExternalLogin(externalIdentity.getLogin());
      dto.setExternalIdentityProvider(externalIdentity.getProvider());
      dto.setExternalId(externalIdentity.getId());
      dto.setLocal(false);
      dto.setSalt(null);
      dto.setCryptedPassword(null);
    }
    UserDto existingUser = dbClient.userDao().selectByExternalIdAndIdentityProvider(dbSession, dto.getExternalId(), dto.getExternalIdentityProvider());
    checkArgument(existingUser == null || Objects.equals(dto.getUuid(), existingUser.getUuid()),
      "A user with provider id '%s' and identity provider '%s' already exists", dto.getExternalId(), dto.getExternalIdentityProvider());
  }

  private void setOnboarded(UserDto userDto) {
    boolean showOnboarding = config.getBoolean(ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS.getKey()).orElse(false);
    userDto.setOnboarded(!showOnboarding);
  }

  private static boolean checkNotEmptyParam(@Nullable String value, String param, List<String> messages) {
    if (isNullOrEmpty(value)) {
      messages.add(format(Validation.CANT_BE_EMPTY_MESSAGE, param));
      return false;
    }
    return true;
  }

  private static boolean validateLoginFormat(@Nullable String login, List<String> messages) {
    boolean isValid = checkNotEmptyParam(login, LOGIN_PARAM, messages);
    if (!isNullOrEmpty(login)) {
      if (login.length() < LOGIN_MIN_LENGTH) {
        messages.add(format(Validation.IS_TOO_SHORT_MESSAGE, LOGIN_PARAM, LOGIN_MIN_LENGTH));
        return false;
      } else if (login.length() > LOGIN_MAX_LENGTH) {
        messages.add(format(Validation.IS_TOO_LONG_MESSAGE, LOGIN_PARAM, LOGIN_MAX_LENGTH));
        return false;
      } else if (!login.matches("\\A\\w[\\w\\.\\-_@]+\\z")) {
        messages.add("Use only letters, numbers, and .-_@ please.");
        return false;
      }
    }
    return isValid;
  }

  private static boolean validateNameFormat(@Nullable String name, List<String> messages) {
    boolean isValid = checkNotEmptyParam(name, NAME_PARAM, messages);
    if (name != null && name.length() > NAME_MAX_LENGTH) {
      messages.add(format(Validation.IS_TOO_LONG_MESSAGE, NAME_PARAM, 200));
      return false;
    }
    return isValid;
  }

  private static boolean validateEmailFormat(@Nullable String email, List<String> messages) {
    if (email != null && email.length() > EMAIL_MAX_LENGTH) {
      messages.add(format(Validation.IS_TOO_LONG_MESSAGE, EMAIL_PARAM, 100));
      return false;
    }
    return true;
  }

  private static boolean checkPasswordChangeAllowed(UserDto userDto, List<String> messages) {
    if (!userDto.isLocal()) {
      messages.add("Password cannot be changed when external authentication is used");
      return false;
    }
    return true;
  }

  private static boolean validatePasswords(@Nullable String password, List<String> messages) {
    if (password == null || password.length() == 0) {
      messages.add(format(Validation.CANT_BE_EMPTY_MESSAGE, PASSWORD_PARAM));
      return false;
    }
    return true;
  }

  private boolean validateScmAccounts(DbSession dbSession, List<String> scmAccounts, @Nullable String login, @Nullable String email, @Nullable UserDto existingUser,
    List<String> messages) {
    boolean isValid = true;
    for (String scmAccount : scmAccounts) {
      if (scmAccount.equals(login) || scmAccount.equals(email)) {
        messages.add("Login and email are automatically considered as SCM accounts");
        isValid = false;
      } else {
        List<UserDto> matchingUsers = dbClient.userDao().selectByScmAccountOrLoginOrEmail(dbSession, scmAccount);
        List<String> matchingUsersWithoutExistingUser = newArrayList();
        for (UserDto matchingUser : matchingUsers) {
          if (existingUser != null && matchingUser.getId().equals(existingUser.getId())) {
            continue;
          }
          matchingUsersWithoutExistingUser.add(getNameOrLogin(matchingUser) + " (" + matchingUser.getLogin() + ")");
        }
        if (!matchingUsersWithoutExistingUser.isEmpty()) {
          messages.add(format("The scm account '%s' is already used by user(s) : '%s'", scmAccount, Joiner.on(", ").join(matchingUsersWithoutExistingUser)));
          isValid = false;
        }
      }
    }
    return isValid;
  }

  private static String getNameOrLogin(UserDto user) {
    String name = user.getName();
    return name != null ? name : user.getLogin();
  }

  private static List<String> sanitizeScmAccounts(@Nullable List<String> scmAccounts) {
    if (scmAccounts != null) {
      return new HashSet<>(scmAccounts).stream()
        .map(Strings::emptyToNull)
        .filter(Objects::nonNull)
        .sorted(String::compareToIgnoreCase)
        .collect(toList(scmAccounts.size()));
    }
    return Collections.emptyList();
  }

  private void checkLoginUniqueness(DbSession dbSession, String login) {
    UserDto existingUser = dbClient.userDao().selectByLogin(dbSession, login);
    checkArgument(existingUser == null, "A user with login '%s' already exists", login);
  }

  private UserDto saveUser(DbSession dbSession, UserDto userDto) {
    userDto.setActive(true);
    UserDto res = dbClient.userDao().insert(dbSession, userDto);
    boolean isOrganizationEnabled = organizationFlags.isEnabled(dbSession);
    if (isOrganizationEnabled) {
      setNotificationsReadDate(dbSession, userDto);
    } else {
      addUserToDefaultOrganizationAndDefaultGroup(dbSession, userDto);
    }

    return res;
  }

  private void updateUser(DbSession dbSession, UserDto dto) {
    dto.setActive(true);
    dbClient.userDao().update(dbSession, dto);
  }

  private void notifyNewUser(String login, String name, @Nullable String email) {
    newUserNotifier.onNewUser(NewUserHandler.Context.builder()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .build());
  }

  private static boolean isUserAlreadyMemberOfDefaultGroup(GroupDto defaultGroup, List<GroupDto> userGroups) {
    return userGroups.stream().anyMatch(group -> defaultGroup.getId().equals(group.getId()));
  }

  private void addUserToDefaultOrganizationAndDefaultGroup(DbSession dbSession, UserDto userDto) {
    addUserToDefaultOrganization(dbSession, userDto);
    addDefaultGroup(dbSession, userDto);
  }

  private void addUserToDefaultOrganization(DbSession dbSession, UserDto userDto) {
    String defOrgUuid = defaultOrganizationProvider.get().getUuid();
    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto().setOrganizationUuid(defOrgUuid).setUserId(userDto.getId()));
  }

  private void addDefaultGroup(DbSession dbSession, UserDto userDto) {
    String defOrgUuid = defaultOrganizationProvider.get().getUuid();
    List<GroupDto> userGroups = dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin());
    GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession, defOrgUuid);
    if (isUserAlreadyMemberOfDefaultGroup(defaultGroup, userGroups)) {
      return;
    }
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(userDto.getId()).setGroupId(defaultGroup.getId()));
  }

  private void setNotificationsReadDate(DbSession dbSession, UserDto user) {
    dbClient.userPropertiesDao().insertOrUpdate(dbSession, new UserPropertyDto()
      .setUserUuid(user.getUuid())
      .setKey(NOTIFICATIONS_READ_DATE)
      .setValue(Long.toString(system2.now())));
  }
}
