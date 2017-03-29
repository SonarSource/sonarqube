/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.util.Validation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.sonar.db.user.UserDto.encryptPassword;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
public class UserUpdater {

  public static final String SONAR_USERS_GROUP_NAME = "sonar-users";

  private static final String SQ_AUTHORITY = "sonarqube";

  private static final String LOGIN_PARAM = "Login";
  private static final String PASSWORD_PARAM = "Password";
  private static final String NAME_PARAM = "Name";
  private static final String EMAIL_PARAM = "Email";

  private static final int LOGIN_MIN_LENGTH = 2;
  private static final int LOGIN_MAX_LENGTH = 255;
  private static final int EMAIL_MAX_LENGTH = 100;
  private static final int NAME_MAX_LENGTH = 200;

  private final NewUserNotifier newUserNotifier;
  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final System2 system2;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationCreation organizationCreation;

  public UserUpdater(NewUserNotifier newUserNotifier, DbClient dbClient, UserIndexer userIndexer, System2 system2,
    DefaultOrganizationProvider defaultOrganizationProvider, OrganizationCreation organizationCreation) {
    this.newUserNotifier = newUserNotifier;
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.system2 = system2;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationCreation = organizationCreation;
  }

  public UserDto create(DbSession dbSession, NewUser newUser) {
    String login = newUser.login();
    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, newUser.login());
    if (userDto == null) {
      userDto = saveUser(dbSession, createNewUserDto(dbSession, newUser));
    } else {
      reactivateUser(dbSession, userDto, login, newUser);
    }
    notifyNewUser(userDto.getLogin(), userDto.getName(), newUser.email());
    return userDto;
  }

  private void reactivateUser(DbSession dbSession, UserDto existingUser, String login, NewUser newUser) {
    checkArgument(!existingUser.isActive(), "An active user with login '%s' already exists", login);
    UpdateUser updateUser = UpdateUser.create(login)
      .setName(newUser.name())
      .setEmail(newUser.email())
      .setScmAccounts(newUser.scmAccounts());
    if (newUser.password() != null) {
      updateUser.setPassword(newUser.password());
    }
    if (newUser.externalIdentity() != null) {
      updateUser.setExternalIdentity(newUser.externalIdentity());
    }
    // Hack to allow to change the password of the user
    existingUser.setLocal(true);
    updateUserDto(dbSession, updateUser, existingUser);
    updateUser(dbSession, existingUser);
    addDefaultGroup(dbSession, existingUser);
    addUserToDefaultOrganization(dbSession, existingUser);
    dbSession.commit();
  }

  public void update(DbSession dbSession, UpdateUser updateUser) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, updateUser.login());
    checkFound(user, "User with login '%s' has not been found", updateUser.login());
    boolean isUserUpdated = updateUserDto(dbSession, updateUser, user);
    if (!isUserUpdated) {
      return;
    }
    updateUser(dbSession, user);
    notifyNewUser(user.getLogin(), user.getName(), user.getEmail());
  }

  private UserDto createNewUserDto(DbSession dbSession, NewUser newUser) {
    UserDto userDto = new UserDto();
    List<String> messages = newArrayList();

    String login = newUser.login();
    if (validateLoginFormat(login, messages)) {
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
      setEncryptedPassWord(password, userDto);
    }

    List<String> scmAccounts = sanitizeScmAccounts(newUser.scmAccounts());
    if (scmAccounts != null && !scmAccounts.isEmpty() && validateScmAccounts(dbSession, scmAccounts, login, email, null, messages)) {
      userDto.setScmAccounts(scmAccounts);
    }

    setExternalIdentity(userDto, newUser.externalIdentity());

    checkRequest(messages.isEmpty(), messages);
    return userDto;
  }

  private boolean updateUserDto(DbSession dbSession, UpdateUser updateUser, UserDto userDto) {
    List<String> messages = newArrayList();
    boolean changed = updateName(updateUser, userDto, messages);
    changed |= updateEmail(updateUser, userDto, messages);
    changed |= updateExternalIdentity(updateUser, userDto);
    changed |= updatePassword(updateUser, userDto, messages);
    changed |= updateScmAccounts(dbSession, updateUser, userDto, messages);
    checkRequest(messages.isEmpty(), messages);
    return changed;
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

  private static boolean updateExternalIdentity(UpdateUser updateUser, UserDto userDto) {
    ExternalIdentity externalIdentity = updateUser.externalIdentity();
    if (updateUser.isExternalIdentityChanged() && !isSameExternalIdentity(userDto, externalIdentity)) {
      setExternalIdentity(userDto, externalIdentity);
      userDto.setSalt(null);
      userDto.setCryptedPassword(null);
      return true;
    }
    return false;
  }

  private static boolean updatePassword(UpdateUser updateUser, UserDto userDto, List<String> messages) {
    String password = updateUser.password();
    if (!updateUser.isExternalIdentityChanged() && updateUser.isPasswordChanged() && validatePasswords(password, messages) && checkPasswordChangeAllowed(userDto, messages)) {
      setEncryptedPassWord(password, userDto);
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
    return (externalIdentity == null && dto.getExternalIdentity() == null) ||
      (externalIdentity != null
        && Objects.equals(dto.getExternalIdentity(), externalIdentity.getId())
        && Objects.equals(dto.getExternalIdentityProvider(), externalIdentity.getProvider()));
  }

  private static void setExternalIdentity(UserDto dto, @Nullable ExternalIdentity externalIdentity) {
    if (externalIdentity == null) {
      dto.setExternalIdentity(dto.getLogin());
      dto.setExternalIdentityProvider(SQ_AUTHORITY);
      dto.setLocal(true);
    } else {
      dto.setExternalIdentity(externalIdentity.getId());
      dto.setExternalIdentityProvider(externalIdentity.getProvider());
      dto.setLocal(false);
    }
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
          matchingUsersWithoutExistingUser.add(matchingUser.getName() + " (" + matchingUser.getLogin() + ")");
        }
        if (!matchingUsersWithoutExistingUser.isEmpty()) {
          messages.add(format("The scm account '%s' is already used by user(s) : '%s'", scmAccount, Joiner.on(", ").join(matchingUsersWithoutExistingUser)));
          isValid = false;
        }
      }
    }
    return isValid;
  }

  private static List<String> sanitizeScmAccounts(@Nullable List<String> scmAccounts) {
    if (scmAccounts != null) {
      return scmAccounts.stream().filter(s -> !Strings.isNullOrEmpty(s)).collect(MoreCollectors.toList());
    }
    return Collections.emptyList();
  }

  private UserDto saveUser(DbSession dbSession, UserDto userDto) {
    long now = system2.now();
    userDto.setActive(true).setCreatedAt(now).setUpdatedAt(now);
    UserDto res = dbClient.userDao().insert(dbSession, userDto);
    addDefaultGroup(dbSession, userDto);
    addUserToDefaultOrganization(dbSession, userDto);
    organizationCreation.createForUser(dbSession, userDto);
    dbSession.commit();
    userIndexer.index(userDto.getLogin());
    return res;
  }

  private void updateUser(DbSession dbSession, UserDto userDto) {
    long now = system2.now();
    userDto.setActive(true).setUpdatedAt(now);
    dbClient.userDao().update(dbSession, userDto);
    dbSession.commit();
    userIndexer.index(userDto.getLogin());
  }

  private static void setEncryptedPassWord(String password, UserDto userDto) {
    Random random = new SecureRandom();
    byte[] salt = new byte[32];
    random.nextBytes(salt);
    String saltHex = DigestUtils.sha1Hex(salt);
    userDto.setSalt(saltHex);
    userDto.setCryptedPassword(encryptPassword(password, saltHex));
  }

  private void notifyNewUser(String login, String name, String email) {
    newUserNotifier.onNewUser(NewUserHandler.Context.builder()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .build());
  }

  private void addDefaultGroup(DbSession dbSession, UserDto userDto) {
    String defOrgUuid = defaultOrganizationProvider.get().getUuid();
    List<GroupDto> userGroups = dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin());
    if (isUserAlreadyMemberOfDefaultGroup(SONAR_USERS_GROUP_NAME, defOrgUuid, userGroups)) {
      return;
    }
    Optional<GroupDto> groupDto = dbClient.groupDao().selectByName(dbSession, defOrgUuid, SONAR_USERS_GROUP_NAME);
    if (!groupDto.isPresent()) {
      throw new ServerException(HttpURLConnection.HTTP_INTERNAL_ERROR, format("The default group '%s' for new users does not exist.", SONAR_USERS_GROUP_NAME));
    }
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(userDto.getId()).setGroupId(groupDto.get().getId()));
  }

  private static boolean isUserAlreadyMemberOfDefaultGroup(String defaultGroupName, String defOrgUuid, List<GroupDto> userGroups) {
    return userGroups.stream().anyMatch(g -> defOrgUuid.equals(g.getOrganizationUuid()) && g.getName().equals(defaultGroupName));
  }

  private void addUserToDefaultOrganization(DbSession dbSession, UserDto userDto) {
    String defOrgUuid = defaultOrganizationProvider.get().getUuid();
    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto().setOrganizationUuid(defOrgUuid).setUserId(userDto.getId()));
  }
}
