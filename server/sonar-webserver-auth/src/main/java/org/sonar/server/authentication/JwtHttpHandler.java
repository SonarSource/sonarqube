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
package org.sonar.server.authentication;

import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.sonar.process.ProcessProperties.Property.WEB_SESSION_TIMEOUT_IN_MIN;
import static org.sonar.server.authentication.Cookies.SAMESITE_LAX;
import static org.sonar.server.authentication.Cookies.SET_COOKIE;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;
import static org.sonar.server.authentication.JwtSerializer.LAST_REFRESH_TIME_PARAM;

@ServerSide
public class JwtHttpHandler {
  private static final int SESSION_TIMEOUT_DEFAULT_VALUE_IN_MINUTES = 3 * 24 * 60;
  private static final int MAX_SESSION_TIMEOUT_IN_MINUTES = 3 * 30 * 24 * 60;

  private static final String JWT_COOKIE = "JWT-SESSION";

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

  public void generateToken(UserDto user, Map<String, Object> properties, HttpRequest request, HttpResponse response) {
    String csrfState = jwtCsrfVerifier.generateState(request, response, sessionTimeoutInSeconds);
    long expirationTime = system2.now() + sessionTimeoutInSeconds * 1000L;
    SessionTokenDto sessionToken = createSessionToken(user, expirationTime);

    String token = jwtSerializer.encode(new JwtSerializer.JwtSession(
      user.getUuid(),
      sessionToken.getUuid(),
      expirationTime,
      ImmutableMap.<String, Object>builder()
        .putAll(properties)
        .put(LAST_REFRESH_TIME_PARAM, system2.now())
        .put(CSRF_JWT_PARAM, csrfState)
        .build()));
    response.addHeader(SET_COOKIE, createJwtSession(request, JWT_COOKIE, token, sessionTimeoutInSeconds));
  }

  private SessionTokenDto createSessionToken(UserDto user, long expirationTime) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SessionTokenDto sessionToken = new SessionTokenDto()
        .setUserUuid(user.getUuid())
        .setExpirationDate(expirationTime);
      dbClient.sessionTokensDao().insert(dbSession, sessionToken);
      dbSession.commit();
      return sessionToken;
    }
  }

  public void generateToken(UserDto user, HttpRequest request, HttpResponse response) {
    generateToken(user, Collections.emptyMap(), request, response);
  }

  public Optional<UserDto> validateToken(HttpRequest request, HttpResponse response) {
    Optional<Token> token = getToken(request, response);
    return token.map(Token::getUserDto);
  }

  public Optional<Token> getToken(HttpRequest request, HttpResponse response) {
    Optional<String> encodedToken = getTokenFromCookie(request);
    if (!encodedToken.isPresent()) {
      return Optional.empty();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      return validateToken(dbSession, encodedToken.get(), request, response);
    }
  }

  private static Optional<String> getTokenFromCookie(HttpRequest request) {
    Optional<Cookie> jwtCookie = findCookie(JWT_COOKIE, request);
    if (jwtCookie.isEmpty()) {
      return Optional.empty();
    }
    Cookie cookie = jwtCookie.get();
    String token = cookie.getValue();
    if (isEmpty(token)) {
      return Optional.empty();
    }
    return Optional.of(token);
  }

  private Optional<Token> validateToken(DbSession dbSession, String tokenEncoded, HttpRequest request, HttpResponse response) {
    Optional<Claims> claims = jwtSerializer.decode(tokenEncoded);
    if (claims.isEmpty()) {
      return Optional.empty();
    }
    Claims token = claims.get();

    Optional<SessionTokenDto> sessionToken = dbClient.sessionTokensDao().selectByUuid(dbSession, token.getId());
    if (sessionToken.isEmpty()) {
      return Optional.empty();
    }
    // Check on expiration is already done when decoding the JWT token, but here is done a double check with the expiration date from DB.
    Date now = new Date(system2.now());
    if (now.getTime() > sessionToken.get().getExpirationDate()) {
      return Optional.empty();
    }
    if (now.after(addSeconds(token.getIssuedAt(), SESSION_DISCONNECT_IN_SECONDS))) {
      return Optional.empty();
    }
    jwtCsrfVerifier.verifyState(request, (String) token.get(CSRF_JWT_PARAM), token.getSubject());

    if (now.after(addSeconds(getLastRefreshDate(token), SESSION_REFRESH_IN_SECONDS))) {
      refreshToken(dbSession, sessionToken.get(), token, request, response);
    }

    Optional<UserDto> user = selectUserFromUuid(dbSession, token.getSubject());
    return user.map(userDto -> new Token(userDto, claims.get()));
  }

  private static Date getLastRefreshDate(Claims token) {
    Long lastFreshTime = (Long) token.get(LAST_REFRESH_TIME_PARAM);
    requireNonNull(lastFreshTime, "last refresh time is missing in token");
    return new Date(lastFreshTime);
  }

  private void refreshToken(DbSession dbSession, SessionTokenDto tokenFromDb, Claims tokenFromCookie, HttpRequest request, HttpResponse response) {
    long expirationTime = system2.now() + sessionTimeoutInSeconds * 1000L;
    String refreshToken = jwtSerializer.refresh(tokenFromCookie, expirationTime);
    response.addHeader(SET_COOKIE, createJwtSession(request, JWT_COOKIE, refreshToken, sessionTimeoutInSeconds));
    jwtCsrfVerifier.refreshState(request, response, (String) tokenFromCookie.get(CSRF_JWT_PARAM), sessionTimeoutInSeconds);

    dbClient.sessionTokensDao().update(dbSession, tokenFromDb.setExpirationDate(expirationTime));
    dbSession.commit();
  }

  public void removeToken(HttpRequest request, HttpResponse response) {
    removeSessionToken(request);
    response.addCookie(createCookie(request, JWT_COOKIE, null, 0));
    jwtCsrfVerifier.removeState(request, response);
  }

  private void removeSessionToken(HttpRequest request) {
    Optional<Cookie> jwtCookie = findCookie(JWT_COOKIE, request);
    if (!jwtCookie.isPresent()) {
      return;
    }
    Optional<Claims> claims = jwtSerializer.decode(jwtCookie.get().getValue());
    if (!claims.isPresent()) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.sessionTokensDao().deleteByUuid(dbSession, claims.get().getId());
      dbSession.commit();
    }
  }

  private static Cookie createCookie(HttpRequest request, String name, @Nullable String value, int expirationInSeconds) {
    return newCookieBuilder(request).setName(name).setValue(value).setHttpOnly(true).setExpiry(expirationInSeconds).build();
  }

  private static String createJwtSession(HttpRequest request, String name, @Nullable String value, int expirationInSeconds) {
    return newCookieBuilder(request).setName(name).setValue(value).setHttpOnly(true).setExpiry(expirationInSeconds).setSameSite(SAMESITE_LAX).toValueString();
  }

  private Optional<UserDto> selectUserFromUuid(DbSession dbSession, String userUuid) {
    UserDto user = dbClient.userDao().selectByUuid(dbSession, userUuid);
    return Optional.ofNullable(user != null && user.isActive() ? user : null);
  }

  private static int getSessionTimeoutInSeconds(Configuration config) {
    int minutes = config.getInt(WEB_SESSION_TIMEOUT_IN_MIN.getKey()).orElse(SESSION_TIMEOUT_DEFAULT_VALUE_IN_MINUTES);
    checkArgument(minutes > SESSION_REFRESH_IN_SECONDS / 60 && minutes <= MAX_SESSION_TIMEOUT_IN_MINUTES,
      "Property %s must be higher than 5 minutes and must not be greater than 3 months. Got %s minutes", WEB_SESSION_TIMEOUT_IN_MIN.getKey(),
      minutes);
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
