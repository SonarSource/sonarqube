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

package org.sonar.server.authentication;

import static java.util.Locale.ENGLISH;
import static org.elasticsearch.common.Strings.isEmpty;

import com.google.common.base.Charsets;
import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.usertoken.UserTokenAuthenticator;

public class BasicAuthenticator {

  private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BASIC_AUTHORIZATION = "BASIC";

  private final DbClient dbClient;
  private final CredentialsAuthenticator credentialsAuthenticator;
  private final UserTokenAuthenticator userTokenAuthenticator;

  public BasicAuthenticator(DbClient dbClient, CredentialsAuthenticator credentialsAuthenticator,
    UserTokenAuthenticator userTokenAuthenticator) {
    this.dbClient = dbClient;
    this.credentialsAuthenticator = credentialsAuthenticator;
    this.userTokenAuthenticator = userTokenAuthenticator;
  }

  public Optional<UserDto> authenticate(HttpServletRequest request) {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (authorizationHeader == null || !authorizationHeader.toUpperCase(ENGLISH).startsWith(BASIC_AUTHORIZATION)) {
      return Optional.empty();
    }

    String[] credentials = getCredentials(authorizationHeader);
    String login = credentials[0];
    String password = credentials[1];
    return Optional.of(authenticate(login, password, request));
  }

  private static String[] getCredentials(String authorizationHeader) {
    String basicAuthEncoded = authorizationHeader.substring(6);
    String basicAuthDecoded = new String(BASE64_DECODER.decode(basicAuthEncoded.getBytes(Charsets.UTF_8)), Charsets.UTF_8);

    int semiColonPos = basicAuthDecoded.indexOf(':');
    if (semiColonPos <= 0) {
      throw new UnauthorizedException("Invalid credentials : " + basicAuthDecoded);
    }
    String login = basicAuthDecoded.substring(0, semiColonPos);
    String password = basicAuthDecoded.substring(semiColonPos + 1);
    return new String[] {login, password};
  }

  private UserDto authenticate(String login, String password, HttpServletRequest request) {
    if (isEmpty(password)) {
      return authenticateFromUserToken(login);
    } else {
      return credentialsAuthenticator.authenticate(login, password, request);
    }
  }

  private UserDto authenticateFromUserToken(String token) {
    Optional<String> authenticatedLogin = userTokenAuthenticator.authenticate(token);
    if (!authenticatedLogin.isPresent()) {
      throw new UnauthorizedException("Token doesn't exist");
    }
    DbSession dbSession = dbClient.openSession(false);
    try {
      UserDto userDto = dbClient.userDao().selectActiveUserByLogin(dbSession, authenticatedLogin.get());
      if (userDto == null) {
        throw new UnauthorizedException("User doesn't exist");
      }
      return userDto;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

}
