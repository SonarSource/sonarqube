/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.usertoken;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.authentication.Credentials;
import org.sonar.server.authentication.UserAuthResult;
import org.sonar.server.authentication.UserLastConnectionDatesUpdater;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.NotFoundException;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.authentication.BasicAuthentication.extractCredentialsFromHeader;

public class UserTokenAuthentication {
  private static final String ACCESS_LOG_TOKEN_NAME = "TOKEN_NAME";

  private final TokenGenerator tokenGenerator;
  private final DbClient dbClient;
  private final UserLastConnectionDatesUpdater userLastConnectionDatesUpdater;
  private final AuthenticationEvent authenticationEvent;

  public UserTokenAuthentication(TokenGenerator tokenGenerator, DbClient dbClient, UserLastConnectionDatesUpdater userLastConnectionDatesUpdater,
    AuthenticationEvent authenticationEvent) {
    this.tokenGenerator = tokenGenerator;
    this.dbClient = dbClient;
    this.userLastConnectionDatesUpdater = userLastConnectionDatesUpdater;
    this.authenticationEvent = authenticationEvent;
  }

  public Optional<UserAuthResult> authenticate(HttpServletRequest request) {
    if (isTokenBasedAuthentication(request)) {
      Optional<Credentials> credentials = extractCredentialsFromHeader(request);
      if (credentials.isPresent()) {
        UserAuthResult userAuthResult = authenticateFromUserToken(credentials.get().getLogin(), request);
        authenticationEvent.loginSuccess(request, userAuthResult.getUserDto().getLogin(), AuthenticationEvent.Source.local(AuthenticationEvent.Method.BASIC_TOKEN));
        return Optional.of(userAuthResult);
      }
    }
    return Optional.empty();
  }

  public static boolean isTokenBasedAuthentication(HttpServletRequest request) {
    Optional<Credentials> credentialsOptional = extractCredentialsFromHeader(request);
    return credentialsOptional.map(credentials -> credentials.getPassword().isEmpty()).orElse(false);
  }

  private UserAuthResult authenticateFromUserToken(String token, HttpServletRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserTokenDto userToken = authenticate(token);
      UserDto userDto = dbClient.userDao().selectByUuid(dbSession, userToken.getUserUuid());
      if (userDto == null || !userDto.isActive()) {
        throw AuthenticationException.newBuilder()
          .setSource(AuthenticationEvent.Source.local(AuthenticationEvent.Method.BASIC_TOKEN))
          .setMessage("User doesn't exist")
          .build();
      }
      request.setAttribute(ACCESS_LOG_TOKEN_NAME, userToken.getName());
      return new UserAuthResult(userDto, userToken, UserAuthResult.AuthType.TOKEN);
    } catch (NotFoundException | IllegalStateException exception ) {
      throw AuthenticationException.newBuilder()
        .setSource(AuthenticationEvent.Source.local(AuthenticationEvent.Method.BASIC_TOKEN))
        .setMessage(exception.getMessage())
        .build();
    }
  }

  private UserTokenDto authenticate(String token) {
    UserTokenDto userToken = getUserToken(token);
    if (userToken == null) {
      throw new NotFoundException("Token doesn't exist");
    }
    if (userToken.isExpired()) {
      throw new IllegalStateException("The token expired on " + formatDateTime(userToken.getExpirationDate()));
    }
    userLastConnectionDatesUpdater.updateLastConnectionDateIfNeeded(userToken);
    return userToken;
  }

  @Nullable
  public UserTokenDto getUserToken(String token) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.userTokenDao().selectByTokenHash(dbSession, tokenGenerator.hash(token));
    }
  }
}
