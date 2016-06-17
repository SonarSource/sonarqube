/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.db.user.UserDto.encryptPassword;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.util.Validation;

@ServerSide
public class UserUpdater {

  public static final String SQ_AUTHORITY = "sonarqube";

  private static final String LOGIN_PARAM = "Login";
  private static final String PASSWORD_PARAM = "Password";
  private static final String NAME_PARAM = "Name";
  private static final String EMAIL_PARAM = "Email";

  private static final int LOGIN_MIN_LENGTH = 3;
  private static final int LOGIN_MAX_LENGTH = 255;
  private static final int EMAIL_MAX_LENGTH = 100;
  private static final int NAME_MAX_LENGTH = 200;

  private final NewUserNotifier newUserNotifier;
  private final Settings settings;
  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final System2 system2;

  public UserUpdater(NewUserNotifier newUserNotifier, Settings settings, DbClient dbClient, UserIndexer userIndexer, System2 system2) {
    this.newUserNotifier = newUserNotifier;
    this.settings = settings;
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.system2 = system2;
  }

  /**
   * Return true if the user has been reactivated
   */
  public boolean create(NewUser newUser) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return create(dbSession, newUser);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public boolean create(DbSession dbSession, NewUser newUser) {
    boolean isUserReactivated = false;
    UserDto userDto = createNewUserDto(dbSession, newUser);
    String login = userDto.getLogin();
    UserDto existingUser = dbClient.userDao().selectByLogin(dbSession, userDto.getLogin());
    if (existingUser == null) {
      saveUser(dbSession, userDto);
      addDefaultGroup(dbSession, userDto);
    } else {
      isUserReactivated = reactivateUser(dbSession, existingUser, login, newUser);
    }
    dbSession.commit();
    notifyNewUser(userDto.getLogin(), userDto.getName(), newUser.email());
    userIndexer.index();
    return isUserReactivated;
  }

  private boolean reactivateUser(DbSession dbSession, UserDto existingUser, String login, NewUser newUser) {
    if (existingUser.isActive()) {
      throw new IllegalArgumentException(String.format("An active user with login '%s' already exists", login));
    }
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
    return true;
  }

  public void update(UpdateUser updateUser) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      update(dbSession, updateUser);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public void update(DbSession dbSession, UpdateUser updateUser) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, updateUser.login());
    if (user == null) {
      throw new NotFoundException(String.format("User with login '%s' has not been found", updateUser.login()));
    }
    updateUserDto(dbSession, updateUser, user);
    updateUser(dbSession, user);
    dbSession.commit();
    notifyNewUser(user.getLogin(), user.getName(), user.getEmail());
    userIndexer.index();
  }

  public void deactivateUserByLogin(String login) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      dbClient.userTokenDao().deleteByLogin(dbSession, login);
      dbClient.userDao().deactivateUserByLogin(dbSession, login);
    } finally {
      dbClient.closeSession(dbSession);
    }
    userIndexer.index();
  }

  public void checkCurrentPassword(String login, String password) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      UserDto user = dbClient.userDao().selectOrFailByLogin(dbSession, login);
      String cryptedPassword = encryptPassword(password, user.getSalt());
      if (!cryptedPassword.equals(user.getCryptedPassword())) {
        throw new IllegalArgumentException("Incorrect password");
      }
    } finally {
      dbSession.close();
    }
  }

  private UserDto createNewUserDto(DbSession dbSession, NewUser newUser) {
    UserDto userDto = new UserDto();
    List<Message> messages = newArrayList();

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
    if (scmAccounts != null && !scmAccounts.isEmpty()) {
      validateScmAccounts(dbSession, scmAccounts, login, email, null, messages);
      userDto.setScmAccounts(scmAccounts);
    }

    setExternalIdentity(userDto, newUser.externalIdentity());

    if (!messages.isEmpty()) {
      throw new BadRequestException(messages);
    }
    return userDto;
  }

  private void updateUserDto(DbSession dbSession, UpdateUser updateUser, UserDto userDto) {
    List<Message> messages = newArrayList();

    String name = updateUser.name();
    if (updateUser.isNameChanged() && validateNameFormat(name, messages)) {
      userDto.setName(name);
    }

    String email = updateUser.email();
    if (updateUser.isEmailChanged() && validateEmailFormat(email, messages)) {
      userDto.setEmail(email);
    }

    if (updateUser.isExternalIdentityChanged()) {
      setExternalIdentity(userDto, updateUser.externalIdentity());
      userDto.setSalt(null);
      userDto.setCryptedPassword(null);
    } else {
      String password = updateUser.password();
      if (updateUser.isPasswordChanged() && validatePasswords(password, messages) && checkPasswordChangeAllowed(userDto, messages)) {
        setEncryptedPassWord(password, userDto);
      }
    }

    if (updateUser.isScmAccountsChanged()) {
      List<String> scmAccounts = sanitizeScmAccounts(updateUser.scmAccounts());
      if (scmAccounts != null && !scmAccounts.isEmpty()) {
        String newOrOldEmail = email != null ? email : userDto.getEmail();
        if (validateScmAccounts(dbSession, scmAccounts, userDto.getLogin(), newOrOldEmail, userDto, messages)) {
          userDto.setScmAccounts(scmAccounts);
        }
      } else {
        userDto.setScmAccounts((String) null);
      }
    }

    if (!messages.isEmpty()) {
      throw new BadRequestException(messages);
    }
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

  private static boolean checkNotEmptyParam(@Nullable String value, String param, List<Message> messages) {
    if (isNullOrEmpty(value)) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, param));
      return false;
    }
    return true;
  }

  private static boolean validateLoginFormat(@Nullable String login, List<Message> messages) {
    boolean isValid = checkNotEmptyParam(login, LOGIN_PARAM, messages);
    if (!isNullOrEmpty(login)) {
      if (login.length() < LOGIN_MIN_LENGTH) {
        messages.add(Message.of(Validation.IS_TOO_SHORT_MESSAGE, LOGIN_PARAM, LOGIN_MIN_LENGTH));
        return false;
      } else if (login.length() > LOGIN_MAX_LENGTH) {
        messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, LOGIN_PARAM, LOGIN_MAX_LENGTH));
        return false;
      } else if (!login.matches("\\A\\w[\\w\\.\\-_@]+\\z")) {
        messages.add(Message.of("user.bad_login"));
        return false;
      }
    }
    return isValid;
  }

  private static boolean validateNameFormat(@Nullable String name, List<Message> messages) {
    boolean isValid = checkNotEmptyParam(name, NAME_PARAM, messages);
    if (name != null && name.length() > NAME_MAX_LENGTH) {
      messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, NAME_PARAM, 200));
      return false;
    }
    return isValid;
  }

  private static boolean validateEmailFormat(@Nullable String email, List<Message> messages) {
    if (email != null && email.length() > EMAIL_MAX_LENGTH) {
      messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, EMAIL_PARAM, 100));
      return false;
    }
    return true;
  }

  private static boolean checkPasswordChangeAllowed(UserDto userDto, List<Message> messages) {
    if (!userDto.isLocal()) {
      messages.add(Message.of("user.password_cant_be_changed_on_external_auth"));
      return false;
    }
    return true;
  }

  private static boolean validatePasswords(@Nullable String password, List<Message> messages) {
    if (password == null || password.length() == 0) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, PASSWORD_PARAM));
      return false;
    }
    return true;
  }

  private boolean validateScmAccounts(DbSession dbSession, List<String> scmAccounts, @Nullable String login, @Nullable String email, @Nullable UserDto existingUser,
    List<Message> messages) {
    boolean isValid = true;
    for (String scmAccount : scmAccounts) {
      if (scmAccount.equals(login) || scmAccount.equals(email)) {
        messages.add(Message.of("user.login_or_email_used_as_scm_account"));
        isValid = false;
      } else {
        List<UserDto> matchingUsers = dbClient.userDao().selectByScmAccountOrLoginOrEmail(dbSession, scmAccount);
        List<String> matchingUsersWithoutExistingUser = newArrayList();
        for (UserDto matchingUser : matchingUsers) {
          if (existingUser == null || !matchingUser.getId().equals(existingUser.getId())) {
            matchingUsersWithoutExistingUser.add(matchingUser.getName() + " (" + matchingUser.getLogin() + ")");
          }
        }
        if (!matchingUsersWithoutExistingUser.isEmpty()) {
          messages.add(Message.of("user.scm_account_already_used", scmAccount, Joiner.on(", ").join(matchingUsersWithoutExistingUser)));
          isValid = false;
        }
      }
    }
    return isValid;
  }

  @CheckForNull
  private static List<String> sanitizeScmAccounts(@Nullable List<String> scmAccounts) {
    if (scmAccounts != null) {
      scmAccounts.removeAll(Arrays.asList(null, ""));
    }
    return scmAccounts;
  }

  private void saveUser(DbSession dbSession, UserDto userDto) {
    long now = system2.now();
    userDto.setActive(true).setCreatedAt(now).setUpdatedAt(now);
    dbClient.userDao().insert(dbSession, userDto);
    addDefaultGroup(dbSession, userDto);
  }

  private void updateUser(DbSession dbSession, UserDto userDto) {
    long now = system2.now();
    userDto.setActive(true).setUpdatedAt(now);
    dbClient.userDao().update(dbSession, userDto);
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
    String defaultGroup = settings.getString(CoreProperties.CORE_DEFAULT_GROUP);
    if (defaultGroup == null) {
      throw new ServerException(HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("The default group property '%s' is null", CoreProperties.CORE_DEFAULT_GROUP));
    }
    List<GroupDto> userGroups = dbClient.groupDao().selectByUserLogin(dbSession, userDto.getLogin());
    if (!Iterables.any(userGroups, new GroupDtoMatchKey(defaultGroup))) {
      GroupDto groupDto = dbClient.groupDao().selectByName(dbSession, defaultGroup);
      if (groupDto == null) {
        throw new ServerException(HttpURLConnection.HTTP_INTERNAL_ERROR,
          String.format("The default group '%s' for new users does not exist. Please update the general security settings to fix this issue.",
            defaultGroup));
      }
      dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(userDto.getId()).setGroupId(groupDto.getId()));
    }
  }

  public void index() {
    userIndexer.index();
  }

  private static class GroupDtoMatchKey implements Predicate<GroupDto> {
    private final String key;

    public GroupDtoMatchKey(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nullable GroupDto input) {
      return input != null && input.getKey().equals(key);
    }
  }
}
