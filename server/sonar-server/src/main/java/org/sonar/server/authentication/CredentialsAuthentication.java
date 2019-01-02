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

/**
 * Authentication based on the tuple {login, password}. Validation can be
 * delegated to an external system, e.g. LDAP.
 */
public class CredentialsAuthentication {

  private final DbClient dbClient;
  private final AuthenticationEvent authenticationEvent;
  private final CredentialsExternalAuthentication externalAuthentication;
  private final CredentialsLocalAuthentication localAuthentication;

  public CredentialsAuthentication(DbClient dbClient, AuthenticationEvent authenticationEvent,
    CredentialsExternalAuthentication externalAuthentication, CredentialsLocalAuthentication localAuthentication) {
    this.dbClient = dbClient;
    this.authenticationEvent = authenticationEvent;
    this.externalAuthentication = externalAuthentication;
    this.localAuthentication = localAuthentication;
  }

  public UserDto authenticate(Credentials credentials, HttpServletRequest request, Method method) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return authenticate(dbSession, credentials, request, method);
    }
  }

  private UserDto authenticate(DbSession dbSession, Credentials credentials, HttpServletRequest request, Method method) {
    UserDto localUser = dbClient.userDao().selectActiveUserByLogin(dbSession, credentials.getLogin());
    if (localUser != null && localUser.isLocal()) {
      localAuthentication.authenticate(dbSession, localUser, credentials.getPassword().orElse(null), method);
      dbSession.commit();
      authenticationEvent.loginSuccess(request, localUser.getLogin(), Source.local(method));
      return localUser;
    }
    Optional<UserDto> externalUser = externalAuthentication.authenticate(credentials, request, method);
    if (externalUser.isPresent()) {
      return externalUser.get();
    }
    throw AuthenticationException.newBuilder()
      .setSource(Source.local(method))
      .setLogin(credentials.getLogin())
      .setMessage(localUser != null && !localUser.isLocal() ? "User is not local" : "No active user for login")
      .build();
  }
}
