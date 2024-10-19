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
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.server.http.HttpRequest;
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

import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.authentication.BasicAuthentication.extractCredentialsFromHeader;

public class UserTokenAuthentication {
  private static final String ACCESS_LOG_TOKEN_NAME = "TOKEN_NAME";
  private static final String BEARER_AUTHORIZATION_SCHEME = "bearer";
  private static final String API_MONITORING_METRICS_PATH = "/api/monitoring/metrics";
  private static final String AUTHORIZATION_HEADER = "Authorization";

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

  public Optional<UserAuthResult> authenticate(HttpRequest request) {
    return findBearerToken(request)
      .or(() -> findTokenUsedWithBasicAuthentication(request))
      .map(userAuthResult -> login(request, userAuthResult));
  }

  private static Optional<String> findBearerToken(HttpRequest request) {
    // hack necessary as #org.sonar.server.monitoring.MetricsAction and org.sonar.server.platform.ws.SafeModeMonitoringMetricAction
    // are providing their own bearer token based authentication mechanism that we can't get rid of for backward compatibility reasons
    if (request.getServletPath().startsWith(API_MONITORING_METRICS_PATH)) {
      return Optional.empty();
    }
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (startsWithIgnoreCase(authorizationHeader, BEARER_AUTHORIZATION_SCHEME)) {
      String token = StringUtils.removeStartIgnoreCase(authorizationHeader, BEARER_AUTHORIZATION_SCHEME + " ");
      return Optional.ofNullable(token);
    }
    return Optional.empty();
  }

  private static Optional<String> findTokenUsedWithBasicAuthentication(HttpRequest request) {
    Credentials credentials = extractCredentialsFromHeader(request).orElse(null);
    if (isTokenWithBasicAuthenticationMethod(credentials)) {
      return Optional.ofNullable(credentials.getLogin());
    }
    return Optional.empty();
  }

  private static boolean isTokenWithBasicAuthenticationMethod(@Nullable Credentials credentials) {
    return Optional.ofNullable(credentials).map(c -> c.getPassword().isEmpty()).orElse(false);
  }

  private UserAuthResult login(HttpRequest request, String token) {
    UserAuthResult userAuthResult = authenticateFromUserToken(token, request);
    authenticationEvent.loginSuccess(request, userAuthResult.getUserDto().getLogin(), AuthenticationEvent.Source.local(AuthenticationEvent.Method.SONARQUBE_TOKEN));
    return userAuthResult;
  }

  private UserAuthResult authenticateFromUserToken(String token, HttpRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserTokenDto userToken = authenticate(token);
      UserDto userDto = dbClient.userDao().selectByUuid(dbSession, userToken.getUserUuid());
      if (userDto == null || !userDto.isActive()) {
        throw AuthenticationException.newBuilder()
          .setSource(AuthenticationEvent.Source.local(AuthenticationEvent.Method.SONARQUBE_TOKEN))
          .setMessage("User doesn't exist")
          .build();
      }
      request.setAttribute(ACCESS_LOG_TOKEN_NAME, userToken.getName());
      return new UserAuthResult(userDto, userToken, UserAuthResult.AuthType.TOKEN);
    } catch (NotFoundException | IllegalStateException exception) {
      throw AuthenticationException.newBuilder()
        .setSource(AuthenticationEvent.Source.local(AuthenticationEvent.Method.SONARQUBE_TOKEN))
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
