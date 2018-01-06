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

import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.time.DateUtils.addSeconds;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;

@ServerSide
public class JwtHttpHandler {

  private static final String SESSION_TIMEOUT_IN_MINUTES_PROPERTY = "sonar.web.sessionTimeoutInMinutes";
  private static final int SESSION_TIMEOUT_DEFAULT_VALUE_IN_MINUTES = 3 * 24 * 60;
  private static final int MAX_SESSION_TIMEOUT_IN_MINUTES = 3 * 30 * 24 * 60;

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
  private final JwtSerializer jwtSerializer;

  // This timeout is used to disconnect the user we he has not browse any page for a while
  private final int sessionTimeoutInSeconds;
  private final JwtCsrfVerifier jwtCsrfVerifier;

  public JwtHttpHandler(System2 system2, DbClient dbClient, Configuration config, JwtSerializer jwtSerializer, JwtCsrfVerifier jwtCsrfVerifier) {
    this.jwtSerializer = jwtSerializer;
    this.dbClient = dbClient;
    this.system2 = system2;
    this.sessionTimeoutInSeconds = getSessionTimeoutInSeconds(config);
    this.jwtCsrfVerifier = jwtCsrfVerifier;
  }

  public void generateToken(UserDto user, Map<String, Object> properties, HttpServletRequest request, HttpServletResponse response) {
    String csrfState = jwtCsrfVerifier.generateState(request, response, sessionTimeoutInSeconds);

    String token = jwtSerializer.encode(new JwtSerializer.JwtSession(
      user.getLogin(),
      sessionTimeoutInSeconds,
      ImmutableMap.<String, Object>builder()
        .putAll(properties)
        .put(LAST_REFRESH_TIME_PARAM, system2.now())
        .put(CSRF_JWT_PARAM, csrfState)
        .build()));
    response.addCookie(createCookie(request, JWT_COOKIE, token, sessionTimeoutInSeconds));
  }

  public void generateToken(UserDto user, HttpServletRequest request, HttpServletResponse response) {
    generateToken(user, Collections.emptyMap(), request, response);
  }

  public Optional<UserDto> validateToken(HttpServletRequest request, HttpServletResponse response) {
    Optional<Token> token = getToken(request, response);
    if (token.isPresent()) {
      return Optional.of(token.get().getUserDto());
    }
    return Optional.empty();
  }

  public Optional<Token> getToken(HttpServletRequest request, HttpServletResponse response) {
    Optional<String> encodedToken = getTokenFromCookie(request);
    if (!encodedToken.isPresent()) {
      return Optional.empty();
    }
    return validateToken(encodedToken.get(), request, response);
  }

  private static Optional<String> getTokenFromCookie(HttpServletRequest request) {
    Optional<Cookie> jwtCookie = findCookie(JWT_COOKIE, request);
    if (!jwtCookie.isPresent()) {
      return Optional.empty();
    }
    Cookie cookie = jwtCookie.get();
    String token = cookie.getValue();
    if (isEmpty(token)) {
      return Optional.empty();
    }
    return Optional.of(token);
  }

  private Optional<Token> validateToken(String tokenEncoded, HttpServletRequest request, HttpServletResponse response) {
    Optional<Claims> claims = jwtSerializer.decode(tokenEncoded);
    if (!claims.isPresent()) {
      return Optional.empty();
    }

    Date now = new Date(system2.now());
    Claims token = claims.get();
    if (now.after(addSeconds(token.getIssuedAt(), SESSION_DISCONNECT_IN_SECONDS))) {
      return Optional.empty();
    }
    jwtCsrfVerifier.verifyState(request, (String) token.get(CSRF_JWT_PARAM), token.getSubject());

    if (now.after(addSeconds(getLastRefreshDate(token), SESSION_REFRESH_IN_SECONDS))) {
      refreshToken(token, request, response);
    }

    Optional<UserDto> user = selectUserFromDb(token.getSubject());
    if (!user.isPresent()) {
      return Optional.empty();
    }
    return Optional.of(new Token(user.get(), claims.get()));
  }

  private static Date getLastRefreshDate(Claims token) {
    Long lastFreshTime = (Long) token.get(LAST_REFRESH_TIME_PARAM);
    requireNonNull(lastFreshTime, "last refresh time is missing in token");
    return new Date(lastFreshTime);
  }

  private void refreshToken(Claims token, HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = jwtSerializer.refresh(token, sessionTimeoutInSeconds);
    response.addCookie(createCookie(request, JWT_COOKIE, refreshToken, sessionTimeoutInSeconds));
    jwtCsrfVerifier.refreshState(request, response, (String) token.get(CSRF_JWT_PARAM), sessionTimeoutInSeconds);
  }

  public void removeToken(HttpServletRequest request, HttpServletResponse response) {
    response.addCookie(createCookie(request, JWT_COOKIE, null, 0));
    jwtCsrfVerifier.removeState(request, response);
  }

  private static Cookie createCookie(HttpServletRequest request, String name, @Nullable String value, int expirationInSeconds) {
    return newCookieBuilder(request).setName(name).setValue(value).setHttpOnly(true).setExpiry(expirationInSeconds).build();
  }

  private Optional<UserDto> selectUserFromDb(String userLogin) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.ofNullable(dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin));
    }
  }

  private static int getSessionTimeoutInSeconds(Configuration config) {
    int minutes = config.getInt(SESSION_TIMEOUT_IN_MINUTES_PROPERTY).orElse(SESSION_TIMEOUT_DEFAULT_VALUE_IN_MINUTES);
    checkArgument(minutes > 0, "Property %s must be strictly positive. Got %s", SESSION_TIMEOUT_IN_MINUTES_PROPERTY, minutes);
    checkArgument(minutes <= MAX_SESSION_TIMEOUT_IN_MINUTES, "Property %s must not be greater than 3 months (%s minutes). Got %s minutes",
      SESSION_TIMEOUT_IN_MINUTES_PROPERTY, MAX_SESSION_TIMEOUT_IN_MINUTES, minutes);
    return minutes * 60;
  }

  public static class Token {
    private final UserDto userDto;
    private final Map<String, Object> properties;

    public Token(UserDto userDto, Map<String, Object> properties) {
      this.userDto = userDto;
      this.properties = properties;
    }

    public UserDto getUserDto() {
      return userDto;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }
  }
}
