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

import static java.util.Objects.requireNonNull;
import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.server.authentication.CookieUtils.findCookie;
import static org.sonar.server.user.ServerUserSession.createForUser;

import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.ServerUserSession;
import org.sonar.server.user.ThreadLocalUserSession;

@ServerSide
public class JwtHttpHandler {

  private static final String SESSION_TIMEOUT_PROPERTY = "sonar.auth.sessionTimeoutInHours";
  private static final int SESSION_TIMEOUT_DEFAULT_VALUE_IN_SECONDS = 3 * 24 * 60 * 60;

  private static final String JWT_COOKIE = "JWT-SESSION";
  private static final String LAST_REFRESH_TIME_PARAM = "lastRefreshTime";

  private static final String CSRF_JWT_PARAM = "xsrfToken";

  // Time after which a user will be disconnected
  private static final int SESSION_DISCONNECT_IN_SECONDS = 3 * 30 * 24 * 60 * 60;

  // This refresh time is used to refresh the session
  // The value must be lower than sessionTimeoutInSeconds
  private static final int SESSION_REFRESH_IN_SECONDS = 5 * 60;

  private final System2 system2;
  private final DbClient dbClient;
  private final Server server;
  private final JwtSerializer jwtSerializer;

  // This timeout is used to disconnect the user we he has not browse any page for a while
  private final int sessionTimeoutInSeconds;
  private final JwtCsrfVerifier jwtCsrfVerifier;
  private final ThreadLocalUserSession threadLocalUserSession;

  public JwtHttpHandler(System2 system2, DbClient dbClient, Server server, Settings settings, JwtSerializer jwtSerializer, JwtCsrfVerifier jwtCsrfVerifier,
    ThreadLocalUserSession threadLocalUserSession) {
    this.jwtSerializer = jwtSerializer;
    this.server = server;
    this.dbClient = dbClient;
    this.system2 = system2;
    this.sessionTimeoutInSeconds = getSessionTimeoutInSeconds(settings);
    this.jwtCsrfVerifier = jwtCsrfVerifier;
    this.threadLocalUserSession = threadLocalUserSession;
  }

  void generateToken(UserDto user, HttpServletResponse response) {
    String csrfState = jwtCsrfVerifier.generateState(response, sessionTimeoutInSeconds);

    String token = jwtSerializer.encode(new JwtSerializer.JwtSession(
      user.getLogin(),
      sessionTimeoutInSeconds,
      ImmutableMap.of(
        LAST_REFRESH_TIME_PARAM, system2.now(),
        CSRF_JWT_PARAM, csrfState)));
    response.addCookie(createCookie(JWT_COOKIE, token, sessionTimeoutInSeconds));
    threadLocalUserSession.set(createForUser(dbClient, user));
  }

  void validateToken(HttpServletRequest request, HttpServletResponse response) {
    validate(request, response);
    if (!threadLocalUserSession.isLoggedIn()) {
      threadLocalUserSession.set(ServerUserSession.createForAnonymous(dbClient));
    }
  }

  private void validate(HttpServletRequest request, HttpServletResponse response) {
    Optional<String> token = getTokenFromCookie(request);
    if (!token.isPresent()) {
      return;
    }
    validateToken(token.get(), request, response);
  }

  private static Optional<String> getTokenFromCookie(HttpServletRequest request) {
    Optional<Cookie> jwtCookie = findCookie(JWT_COOKIE, request);
    if (!jwtCookie.isPresent()) {
      return Optional.empty();
    }
    Cookie cookie = jwtCookie.get();
    String token = cookie.getValue();
    if (isNullOrEmpty(token)) {
      return Optional.empty();
    }
    return Optional.of(token);
  }

  private void validateToken(String tokenEncoded, HttpServletRequest request, HttpServletResponse response) {
    Optional<Claims> claims = jwtSerializer.decode(tokenEncoded);
    if (!claims.isPresent()) {
      removeToken(response);
      return;
    }

    Date now = new Date(system2.now());
    Claims token = claims.get();
    if (now.after(DateUtils.addSeconds(token.getIssuedAt(), SESSION_DISCONNECT_IN_SECONDS))) {
      removeToken(response);
      return;
    }
    jwtCsrfVerifier.verifyState(request, (String) token.get(CSRF_JWT_PARAM));

    if (now.after(DateUtils.addSeconds(getLastRefreshDate(token), SESSION_REFRESH_IN_SECONDS))) {
      refreshToken(token, response);
    }

    Optional<UserDto> user = selectUserFromDb(token.getSubject());
    if (!user.isPresent()) {
      removeToken(response);
      throw new UnauthorizedException("User does not exist");
    }
    threadLocalUserSession.set(createForUser(dbClient, user.get()));
  }

  private static Date getLastRefreshDate(Claims token) {
    Long lastFreshTime = (Long) token.get(LAST_REFRESH_TIME_PARAM);
    requireNonNull(lastFreshTime, "last refresh time is missing in token");
    return new Date(lastFreshTime);
  }

  private void refreshToken(Claims token, HttpServletResponse response) {
    String refreshToken = jwtSerializer.refresh(token, sessionTimeoutInSeconds);
    response.addCookie(createCookie(JWT_COOKIE, refreshToken, sessionTimeoutInSeconds));
    jwtCsrfVerifier.refreshState(response, (String) token.get(CSRF_JWT_PARAM), sessionTimeoutInSeconds);
  }

  void removeToken(HttpServletResponse response) {
    response.addCookie(createCookie(JWT_COOKIE, null, 0));
    jwtCsrfVerifier.removeState(response);
    threadLocalUserSession.remove();
  }

  private Cookie createCookie(String name, @Nullable String value, int expirationInSeconds) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setSecure(server.isSecured());
    cookie.setHttpOnly(true);
    cookie.setMaxAge(expirationInSeconds);
    return cookie;
  }

  private Optional<UserDto> selectUserFromDb(String userLogin) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return Optional.ofNullable(dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static int getSessionTimeoutInSeconds(Settings settings) {
    int propertyFromSettings = settings.getInt(SESSION_TIMEOUT_PROPERTY);
    if (propertyFromSettings > 0) {
      return propertyFromSettings * 60 * 60;
    }
    return SESSION_TIMEOUT_DEFAULT_VALUE_IN_SECONDS;
  }
}
