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
import org.sonar.server.user.db.UserGroupDao;
import org.sonar.server.util.Validation;

import javax.annotation.Nullable;

import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;

public class UserCreator {

  private static final String LOGIN = "Login";
  private static final String PASSWORD_CONFIRMATION = "Password confirmation";
  private static final String PASSWORD = "Password";
  private static final String NAME = "Name";
  private static final String EMAIL = "Email";

  private final NewUserNotifier newUserNotifier;
  private final Settings settings;
  private final UserGroupDao userGroupDao;
  private final DbClient dbClient;
  private final System2 system2;

  public UserCreator(NewUserNotifier newUserNotifier, Settings settings, UserGroupDao userGroupDao, DbClient dbClient, System2 system2) {
    this.newUserNotifier = newUserNotifier;
    this.settings = settings;
    this.userGroupDao = userGroupDao;
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  public void create(NewUser newUser) {
    validate(newUser);

    DbSession dbSession = dbClient.openSession(false);
    try {
      String login = newUser.login();
      UserDto existingUser = dbClient.userDao().selectNullableByLogin(dbSession, login);
      if (existingUser != null) {
        updateExistingUser(dbSession, newUser);
      } else {
        createNewUser(dbSession, newUser);
      }
      dbSession.commit();
      notifyNewUser(newUser);
    } finally {
      dbSession.close();
    }
  }

  private static void validate(NewUser newUser) {
    List<Message> messages = newArrayList();

    validateLogin(newUser.login(), messages);
    validateName(newUser.name(), messages);
    if (newUser.email().length() >= 100) {
      messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, EMAIL, 100));
    }
    validatePassword(newUser, messages);

    if (!messages.isEmpty()) {
      throw new BadRequestException(messages);
    }
  }

  private static void validateLogin(@Nullable String login, List<Message> messages) {
    if (Strings.isNullOrEmpty(login)) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, LOGIN));
    } else if (!login.matches("\\A\\w[\\w\\.\\-_@\\s]+\\z")) {
      messages.add(Message.of("user.bad_login"));
    } else if (login.length() <= 2) {
      messages.add(Message.of(Validation.IS_TOO_SHORT_MESSAGE, LOGIN, 2));
    } else if (login.length() >= 255) {
      messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, LOGIN, 255));
    }
  }

  private static void validateName(@Nullable String name, List<Message> messages) {
    if (Strings.isNullOrEmpty(name)) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, NAME));
    } else if (name.length() >= 200) {
      messages.add(Message.of(Validation.IS_TOO_LONG_MESSAGE, NAME, 200));
    }
  }

  private static void validatePassword(NewUser newUser, List<Message> messages) {
    if (Strings.isNullOrEmpty(newUser.password())) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, PASSWORD));
    }
    if (Strings.isNullOrEmpty(newUser.passwordConfirmation())) {
      messages.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, PASSWORD_CONFIRMATION));
    }

    if (!Strings.isNullOrEmpty(newUser.password()) && !Strings.isNullOrEmpty(newUser.passwordConfirmation())
      && !StringUtils.equals(newUser.password(), newUser.passwordConfirmation())) {
      messages.add(Message.of("user.password_doesnt_match_confirmation"));
    }
  }

  private void createNewUser(DbSession dbSession, NewUser newUser) {
    long now = system2.now();
    UserDto userDto = new UserDto()
      .setLogin(newUser.login())
      .setName(newUser.name())
      .setEmail(newUser.email())
      .setActive(true)
      .setScmAccounts(convertScmAccountsToCsv(newUser))
      .setCreatedAt(now)
      .setUpdatedAt(now);
    setEncryptedPassWord(newUser, userDto);
    dbClient.userDao().insert(dbSession, userDto);
    addDefaultGroup(dbSession, userDto);
  }

  private void updateExistingUser(DbSession dbSession, NewUser newUser) {
    String login = newUser.login();
    UserDto existingUser = dbClient.userDao().selectNullableByLogin(dbSession, login);
    if (existingUser != null) {
      if (!existingUser.isActive()) {
        if (newUser.isPreventReactivation()) {
          throw new ReactivationException(String.format("A disabled user with the login '%s' already exists", login), login);
        } else {
          existingUser.setActive(true);
          existingUser.setUpdatedAt(system2.now());
          dbClient.userDao().update(dbSession, existingUser);
          addDefaultGroup(dbSession, existingUser);
        }
      } else {
        throw new IllegalArgumentException(String.format("A user with the login '%s' already exists", login));
      }
    }
  }

  private static void setEncryptedPassWord(NewUser newUser, UserDto userDto) {
    Random random = new SecureRandom();
    byte[] salt = new byte[32];
    random.nextBytes(salt);
    String saltHex = DigestUtils.sha1Hex(salt);
    userDto.setSalt(saltHex);
    userDto.setCryptedPassword(DigestUtils.sha1Hex("--" + saltHex + "--" + newUser.password() + "--"));
  }

  private static String convertScmAccountsToCsv(NewUser newUser) {
    int size = newUser.scmAccounts().size();
    StringWriter writer = new StringWriter(size);
    CsvWriter csv = CsvWriter.of(writer);
    csv.values(newUser.scmAccounts().toArray(new String[size]));
    csv.close();
    return writer.toString();
  }

  private void notifyNewUser(NewUser newUser) {
    newUserNotifier.onNewUser(NewUserHandler.Context.builder()
      .setLogin(newUser.login())
      .setName(newUser.name())
      .setEmail(newUser.email())
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
