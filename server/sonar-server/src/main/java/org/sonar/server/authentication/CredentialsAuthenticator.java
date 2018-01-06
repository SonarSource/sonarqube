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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.sonar.db.user.UserDto.encryptPassword;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class CredentialsAuthenticator {

  private final DbClient dbClient;
  private final RealmAuthenticator externalAuthenticator;
  private final AuthenticationEvent authenticationEvent;

  public CredentialsAuthenticator(DbClient dbClient, RealmAuthenticator externalAuthenticator, AuthenticationEvent authenticationEvent) {
    this.dbClient = dbClient;
    this.externalAuthenticator = externalAuthenticator;
    this.authenticationEvent = authenticationEvent;
  }

  public UserDto authenticate(String userLogin, String userPassword, HttpServletRequest request, Method method) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return authenticate(dbSession, userLogin, userPassword, request, method);
    }
  }

  private UserDto authenticate(DbSession dbSession, String userLogin, String userPassword, HttpServletRequest request, Method method) {
    UserDto localUser = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
    if (localUser != null && localUser.isLocal()) {
      UserDto userDto = authenticateFromDb(localUser, userPassword, method);
      authenticationEvent.loginSuccess(request, userLogin, Source.local(method));
      return userDto;
    }
    Optional<UserDto> externalUser = externalAuthenticator.authenticate(userLogin, userPassword, request, method);
    if (externalUser.isPresent()) {
      return externalUser.get();
    }
    throw AuthenticationException.newBuilder()
      .setSource(Source.local(method))
      .setLogin(userLogin)
      .setMessage(localUser != null && !localUser.isLocal() ? "User is not local" : "No active user for login")
      .build();
  }

  private static UserDto authenticateFromDb(UserDto userDto, String userPassword, Method method) {
    String cryptedPassword = userDto.getCryptedPassword();
    String salt = userDto.getSalt();
    String failureCause = checkPassword(cryptedPassword, salt, userPassword);
    if (failureCause == null) {
      return userDto;
    }
    throw AuthenticationException.newBuilder()
      .setSource(Source.local(method))
      .setLogin(userDto.getLogin())
      .setMessage(failureCause)
      .build();
  }

  @CheckForNull
  private static String checkPassword(@Nullable String cryptedPassword, @Nullable String salt, String userPassword) {
    if (cryptedPassword == null) {
      return "null password in DB";
    } else if (salt == null) {
      return "null salt";
    } else if (!cryptedPassword.equals(encryptPassword(userPassword, salt))) {
      return "wrong password";
    }
    return null;
  }
}
