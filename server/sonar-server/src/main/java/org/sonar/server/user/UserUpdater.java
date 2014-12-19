/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.user;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.CsvWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.core.user.UserGroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Message;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.db.UserGroupDao;
import org.sonar.server.util.Validation;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;

public class UserUpdater implements ServerComponent {

  private static final String LOGIN_PARAM = "Login";
  private static final String PASSWORD_CONFIRMATION_PARAM = "Password confirmation";
  private static final String PASSWORD_PARAM = "Password";
  private static final String NAME_PARAM = "Name";
  private static final String EMAIL_PARAM = "Email";

  private final NewUserNotifier newUserNotifier;
  private final Settings settings;
  private final UserGroupDao userGroupDao;
  private final DbClient dbClient;
  private final System2 system2;

  public UserUpdater(NewUserNotifier newUserNotifier, Settings settings, UserGroupDao userGroupDao, DbClient dbClient, System2 system2) {
    this.newUserNotifier = newUserNotifier;
    this.settings = settings;
    this.userGroupDao = userGroupDao;
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  /**
   * Retuen true if the user has been reactivated
   * @return
   */
  public boolean create(NewUser newUser) {
    UserDto userDto = createNewUserDto(newUser);
    boolean isUserReactivated = false;

    DbSession dbSession = dbClient.openSession(false);
    try {
      String login = userDto.getLogin();
      UserDto existingUser = dbClient.userDao().selectNullableByLogin(dbSession, login);
      if (existingUser == null) {
        saveUser(dbSession, userDto);
      } else {
        if (existingUser.isActive()) {
          throw new IllegalArgumentException(String.format("An active user with login '%s' already exists", login));
        }
        UpdateUser updateUser = UpdateUser.create(login)
          .setName(newUser.name())
          .setEmail(newUser.email())
          .setScmAccounts(newUser.scmAccounts())
          .setPassword(newUser.password())
          .setPasswordConfirmation(newUser.passwordConfirmation());
        updateUserDto(updateUser, existingUser);
        updateUser(dbSession, existingUser);
        isUserReactivated = true;
      }
      dbSession.commit();
      notifyNewUser(userDto.getLogin(), userDto.getName(), newUser.email());
    } finally {
      dbSession.close();
    }
    return isUserReactivated;
  }

  public void update(UpdateUser updateUser) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      UserDto user = dbClient.userDao().selectNullableByLogin(dbSession, updateUser.login());
      if (user != null) {
        updateUserDto(updateUser, user);
        updateUser(dbSession, user);
      } else {
        throw new NotFoundException(String.format("User '%s' does not exists", updateUser.login()));
      }
      dbSession.commit();
      notifyNewUser(user.getLogin(), user.getName(), user.getEmail());
    } finally {
      dbSession.close();
    }
  }

  private static UserDto createNewUserDto(NewUser newUser) {
    UserDto userDto = new UserDto();
    List<Message> messages = newArrayList();

    String login = newUser.login();
    validateLoginFormat(login, messages);
    userDto.setLogin(login);

    String name = newUser.name();
    validateNameFormat(name, messages);
    userDto.setName(name);

    String email = newUser.email();
    if (email != null) {
      validateEmailFormat(email, messages);
      userDto.setEmail(email);
    }

    String password = newUser.password();
    String passwordConfirmation = newUser.passwordConfirmation();
    validatePasswords(password, passwordConfirmation, messages);
    setEncryptedPassWord(password, userDto);

    userDto.setScmAccounts(convertScmAccountsToCsv(newUser.scmAccounts()));

    if (!messages.isEmpty()) {
      throw new BadRequestException(messages);
    }
    return userDto;
  }

  private static void updateUserDto(UpdateUser updateUser, UserDto userDto) {
    List<Message> messages = newArrayList();

    String name = updateUser.name();
    if (updateUser.isNameChanged()) {
      validateNameFormat(name, messages);
      userDto.setName(name);
    }

    String email = updateUser.email();
    if (updateUser.isEmailChanged()) {
      validateEmailFormat(email, messages);
      userDto.setEmail(email);
    }

    String password = updateUser.password();
    String passwordConfirmation = updateUser.passwordConfirmation();
    if (updateUser.isPasswordChanged()) {
      validatePasswords(password, passwordConfirmation, messages);
      setEncryptedPassWord(password, userDto);
    }

    if (updateUser.isScmAccountsChanged()) {
      userDto.setScmAccounts(convertScmAccountsToCsv(updateUser.scmAccounts()));
    }

    if (!messages.isEmpty()) {
      throw new BadRequestException(messages);
    }
  }

  private static void checkNotEmptyParam(@Nullable String value, String param, List<Message> messages) {
    if (Strings.isNullOrEmpty(value)) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, param));
    }
  }

  private static void validateLoginFormat(@Nullable String login, List<Message> messages) {
    checkNotEmptyParam(login, LOGIN_PARAM, messages);
    if (!Strings.isNullOrEmpty(login)) {
      if (login.length() <= 2) {
        messages.add(Message.of(Validation.IS_TOO_SHORT_MESSAGE, LOGIN_PARAM, 2));
      } else if (login.length() >= 255) {
        messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, LOGIN_PARAM, 255));
      } else if (!login.matches("\\A\\w[\\w\\.\\-_@\\s]+\\z")) {
        messages.add(Message.of("user.bad_login"));
      }
    }
  }

  private static void validateNameFormat(@Nullable String name, List<Message> messages) {
    checkNotEmptyParam(name, NAME_PARAM, messages);
    if (name != null && name.length() >= 200) {
      messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, NAME_PARAM, 200));
    }
  }

  private static void validateEmailFormat(@Nullable String email, List<Message> messages) {
    if (email != null) {
      if (email.length() >= 100) {
        messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, EMAIL_PARAM, 100));
      }
    }
  }

  private static void validatePasswords(@Nullable String password, @Nullable String passwordConfirmation, List<Message> messages) {
    checkNotEmptyParam(password, PASSWORD_PARAM, messages);
    checkNotEmptyParam(passwordConfirmation, PASSWORD_CONFIRMATION_PARAM, messages);
    if (!StringUtils.equals(password, passwordConfirmation)) {
      messages.add(Message.of("user.password_doesnt_match_confirmation"));
    }
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
    addDefaultGroup(dbSession, userDto);
  }

  private static void setEncryptedPassWord(String password, UserDto userDto) {
    Random random = new SecureRandom();
    byte[] salt = new byte[32];
    random.nextBytes(salt);
    String saltHex = DigestUtils.sha1Hex(salt);
    userDto.setSalt(saltHex);
    userDto.setCryptedPassword(DigestUtils.sha1Hex("--" + saltHex + "--" + password + "--"));
  }

  @CheckForNull
  private static String convertScmAccountsToCsv(@Nullable List<String> scmAccounts) {
    if (scmAccounts != null) {
      scmAccounts.removeAll(Arrays.asList(null, ""));
      int size = scmAccounts.size();
      StringWriter writer = new StringWriter(size);
      CsvWriter csv = CsvWriter.of(writer);
      csv.values(scmAccounts.toArray(new String[size]));
      csv.close();
      return writer.toString();
    }
    return null;
  }

  private void notifyNewUser(String login, String name, String email) {
    newUserNotifier.onNewUser(NewUserHandler.Context.builder()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .build());
  }

  private void addDefaultGroup(DbSession dbSession, UserDto userDto) {
    final String defaultGroup = settings.getString(CoreProperties.CORE_DEFAULT_GROUP);
    if (defaultGroup == null) {
      throw new IllegalStateException(String.format("The default group property '%s' is null", CoreProperties.CORE_DEFAULT_GROUP));
    }
    List<GroupDto> userGroups = dbClient.groupDao().findByUserLogin(dbSession, userDto.getLogin());
    if (!Iterables.any(userGroups, new Predicate<GroupDto>() {
      @Override
      public boolean apply(@Nullable GroupDto input) {
        return input != null && input.getKey().equals(defaultGroup);
      }
    })) {
      GroupDto groupDto = dbClient.groupDao().getByKey(dbSession, defaultGroup);
      userGroupDao.insert(dbSession, new UserGroupDto().setUserId(userDto.getId()).setGroupId(groupDto.getId()));
    }
  }
}
