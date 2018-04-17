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
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class CredentialsAuthenticator {

  private final DbClient dbClient;
  private final RealmAuthenticator externalAuthenticator;
  private final AuthenticationEvent authenticationEvent;
  private final LocalAuthentication localAuthentication;

  public CredentialsAuthenticator(DbClient dbClient, RealmAuthenticator externalAuthenticator, AuthenticationEvent authenticationEvent,
    LocalAuthentication localAuthentication) {
    this.dbClient = dbClient;
    this.externalAuthenticator = externalAuthenticator;
    this.authenticationEvent = authenticationEvent;
    this.localAuthentication = localAuthentication;
  }

  public UserDto authenticate(String userLogin, String userPassword, HttpServletRequest request, Method method) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return authenticate(dbSession, userLogin, userPassword, request, method);
    }
  }

  private UserDto authenticate(DbSession dbSession, String userLogin, String userPassword, HttpServletRequest request, Method method) {
    UserDto localUser = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
    if (localUser != null && localUser.isLocal()) {
      localAuthentication.authenticate(dbSession, localUser, userPassword, method);
      dbSession.commit();
      authenticationEvent.loginSuccess(request, userLogin, Source.local(method));
      return localUser;
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
}
