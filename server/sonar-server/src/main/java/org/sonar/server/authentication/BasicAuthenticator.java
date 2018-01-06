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

import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.usertoken.UserTokenAuthenticator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class BasicAuthenticator {

  private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BASIC_AUTHORIZATION = "BASIC";

  private final DbClient dbClient;
  private final CredentialsAuthenticator credentialsAuthenticator;
  private final UserTokenAuthenticator userTokenAuthenticator;
  private final AuthenticationEvent authenticationEvent;

  public BasicAuthenticator(DbClient dbClient, CredentialsAuthenticator credentialsAuthenticator,
    UserTokenAuthenticator userTokenAuthenticator, AuthenticationEvent authenticationEvent) {
    this.dbClient = dbClient;
    this.credentialsAuthenticator = credentialsAuthenticator;
    this.userTokenAuthenticator = userTokenAuthenticator;
    this.authenticationEvent = authenticationEvent;
  }

  public Optional<UserDto> authenticate(HttpServletRequest request) {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (authorizationHeader == null || !authorizationHeader.toUpperCase(ENGLISH).startsWith(BASIC_AUTHORIZATION)) {
      return Optional.empty();
    }

    String[] credentials = getCredentials(authorizationHeader);
    String login = credentials[0];
    String password = credentials[1];
    UserDto userDto = authenticate(login, password, request);
    return Optional.of(userDto);
  }

  private static String[] getCredentials(String authorizationHeader) {
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
    return new String[] {login, password};
  }

  private static String getDecodedBasicAuth(String basicAuthEncoded) {
    try {
      return new String(BASE64_DECODER.decode(basicAuthEncoded.getBytes(UTF_8)), UTF_8);
    } catch (Exception e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(Method.BASIC))
        .setMessage("Invalid basic header")
        .build();
    }
  }

  private UserDto authenticate(String login, String password, HttpServletRequest request) {
    if (isEmpty(password)) {
      UserDto userDto = authenticateFromUserToken(login);
      authenticationEvent.loginSuccess(request, userDto.getLogin(), Source.local(Method.BASIC_TOKEN));
      return userDto;
    } else {
      return credentialsAuthenticator.authenticate(login, password, request, Method.BASIC);
    }
  }

  private UserDto authenticateFromUserToken(String token) {
    Optional<String> authenticatedLogin = userTokenAuthenticator.authenticate(token);
    if (!authenticatedLogin.isPresent()) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(Method.BASIC_TOKEN))
        .setMessage("Token doesn't exist")
        .build();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = dbClient.userDao().selectActiveUserByLogin(dbSession, authenticatedLogin.get());
      if (userDto == null) {
        throw AuthenticationException.newBuilder()
          .setSource(Source.local(Method.BASIC_TOKEN))
          .setMessage("User doesn't exist")
          .build();
      }
      return userDto;
    }
  }

}
