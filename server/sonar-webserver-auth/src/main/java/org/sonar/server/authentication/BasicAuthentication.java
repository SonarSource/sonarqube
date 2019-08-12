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

import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.usertoken.UserTokenAuthentication;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.startsWithIgnoreCase;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

/**
 * HTTP BASIC authentication relying on tuple {login, password}.
 * Login can represent a user access token.
 *
 * @see CredentialsAuthentication for standard login/password authentication
 * @see UserTokenAuthentication for user access token
 */
public class BasicAuthentication {

  private final DbClient dbClient;
  private final CredentialsAuthentication credentialsAuthentication;
  private final UserTokenAuthentication userTokenAuthentication;
  private final AuthenticationEvent authenticationEvent;

  public BasicAuthentication(DbClient dbClient, CredentialsAuthentication credentialsAuthentication,
    UserTokenAuthentication userTokenAuthentication, AuthenticationEvent authenticationEvent) {
    this.dbClient = dbClient;
    this.credentialsAuthentication = credentialsAuthentication;
    this.userTokenAuthentication = userTokenAuthentication;
    this.authenticationEvent = authenticationEvent;
  }

  public Optional<UserDto> authenticate(HttpServletRequest request) {
    return extractCredentialsFromHeader(request)
      .flatMap(credentials -> Optional.of(authenticate(credentials, request)));
  }

  public static Optional<Credentials> extractCredentialsFromHeader(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader == null || !startsWithIgnoreCase(authorizationHeader, "BASIC")) {
      return Optional.empty();
    }

    String basicAuthEncoded = authorizationHeader.substring(6);
    String basicAuthDecoded = getDecodedBasicAuth(basicAuthEncoded);

    int semiColonPos = basicAuthDecoded.indexOf(':');
    if (semiColonPos <= 0) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(Method.BASIC))
        .setMessage("Decoded basic auth does not contain ':'")
        .build();
    }
    String login = basicAuthDecoded.substring(0, semiColonPos);
    String password = basicAuthDecoded.substring(semiColonPos + 1);
    return Optional.of(new Credentials(login, password));
  }

  private static String getDecodedBasicAuth(String basicAuthEncoded) {
    try {
      return new String(Base64.getDecoder().decode(basicAuthEncoded.getBytes(UTF_8)), UTF_8);
    } catch (Exception e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(Method.BASIC))
        .setMessage("Invalid basic header")
        .build();
    }
  }

  private UserDto authenticate(Credentials credentials, HttpServletRequest request) {
    if (!credentials.getPassword().isPresent()) {
      UserDto userDto = authenticateFromUserToken(credentials.getLogin());
      authenticationEvent.loginSuccess(request, userDto.getLogin(), Source.local(Method.BASIC_TOKEN));
      return userDto;
    }
    return credentialsAuthentication.authenticate(credentials, request, Method.BASIC);
  }

  private UserDto authenticateFromUserToken(String token) {
    Optional<String> authenticatedUserUuid = userTokenAuthentication.authenticate(token);
    if (!authenticatedUserUuid.isPresent()) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(Method.BASIC_TOKEN))
        .setMessage("Token doesn't exist")
        .build();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = dbClient.userDao().selectByUuid(dbSession, authenticatedUserUuid.get());
      if (userDto == null || !userDto.isActive()) {
        throw AuthenticationException.newBuilder()
          .setSource(Source.local(Method.BASIC_TOKEN))
          .setMessage("User doesn't exist")
          .build();
      }
      return userDto;
    }
  }

}
