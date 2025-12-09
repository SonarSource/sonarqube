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

import java.util.Optional;
import org.sonar.api.server.http.HttpRequest;
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
  static final String ERROR_PASSWORD_CANNOT_BE_NULL = "Password cannot be null";
  private final DbClient dbClient;
  private final AuthenticationEvent authenticationEvent;
  private final CredentialsExternalAuthentication externalAuthentication;
  private final CredentialsLocalAuthentication localAuthentication;
  private final LdapCredentialsAuthentication ldapCredentialsAuthentication;

  public CredentialsAuthentication(DbClient dbClient, AuthenticationEvent authenticationEvent,
    CredentialsExternalAuthentication externalAuthentication, CredentialsLocalAuthentication localAuthentication,
    LdapCredentialsAuthentication ldapCredentialsAuthentication) {
    this.dbClient = dbClient;
    this.authenticationEvent = authenticationEvent;
    this.externalAuthentication = externalAuthentication;
    this.localAuthentication = localAuthentication;
    this.ldapCredentialsAuthentication = ldapCredentialsAuthentication;
  }

  public UserDto authenticate(Credentials credentials, HttpRequest request, Method method) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return authenticate(dbSession, credentials, request, method);
    }
  }

  private UserDto authenticate(DbSession dbSession, Credentials credentials, HttpRequest request, Method method) {
    UserDto localUser = dbClient.userDao().selectActiveUserByLogin(dbSession, credentials.getLogin());
    if (localUser != null && localUser.isLocal()) {
      String password = getNonNullPassword(credentials);
      localAuthentication.authenticate(dbSession, localUser, password, method);
      dbSession.commit();
      authenticationEvent.loginSuccess(request, localUser.getLogin(), Source.local(method));
      return localUser;
    }
    Optional<UserDto> externalUser = externalAuthentication.authenticate(credentials, request, method)
      .or(() -> ldapCredentialsAuthentication.authenticate(credentials, request, method));
    if (externalUser.isPresent()) {
      return externalUser.get();
    }
    localAuthentication.generateHashToAvoidEnumerationAttack();
    throw AuthenticationException.newBuilder()
      .setSource(Source.local(method))
      .setLogin(credentials.getLogin())
      .setMessage(localUser != null && !localUser.isLocal() ? "User is not local" : "No active user for login")
      .build();
  }

  private static String getNonNullPassword(Credentials credentials) {
    return credentials.getPassword().orElseThrow(() -> new IllegalArgumentException(ERROR_PASSWORD_CANNOT_BE_NULL));
  }

}
