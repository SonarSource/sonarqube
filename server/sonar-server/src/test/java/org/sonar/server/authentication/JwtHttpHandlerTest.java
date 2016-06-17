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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.System2.INSTANCE;
import static org.sonar.db.user.UserTesting.newUserDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.ServerUserSession;
import org.sonar.server.user.ThreadLocalUserSession;

public class JwtHttpHandlerTest {

  static final String JWT_TOKEN = "TOKEN";
  static final String CSRF_STATE = "CSRF_STATE";
  static final String USER_LOGIN = "john";

  static final long NOW = 10_000_000_000L;
  static final long FOUR_MINUTES_AGO = NOW - 4 * 60 * 1000L;
  static final long SIX_MINUTES_AGO = NOW - 6 * 60 * 1000L;
  static final long TEN_DAYS_AGO = NOW - 10 * 24 * 60 * 60 * 1000L;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(INSTANCE);

  ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);
  ArgumentCaptor<JwtSerializer.JwtSession> jwtArgumentCaptor = ArgumentCaptor.forClass(JwtSerializer.JwtSession.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  HttpSession httpSession = mock(HttpSession.class);

  System2 system2 = mock(System2.class);
  Server server = mock(Server.class);
  Settings settings = new Settings();
  JwtSerializer jwtSerializer = mock(JwtSerializer.class);
  JwtCsrfVerifier jwtCsrfVerifier = mock(JwtCsrfVerifier.class);

  UserDto userDto = newUserDto().setLogin(USER_LOGIN);

  JwtHttpHandler underTest = new JwtHttpHandler(system2, dbClient, server, settings, jwtSerializer, jwtCsrfVerifier, threadLocalUserSession);

  @Before
  public void setUp() throws Exception {
    threadLocalUserSession.remove();
    when(system2.now()).thenReturn(NOW);
    when(server.isSecured()).thenReturn(true);
    when(request.getSession()).thenReturn(httpSession);
    when(jwtSerializer.encode(any(JwtSerializer.JwtSession.class))).thenReturn(JWT_TOKEN);
    when(jwtCsrfVerifier.generateState(eq(response), anyInt())).thenReturn(CSRF_STATE);
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
  }

  @Test
  public void create_token() throws Exception {
    underTest.generateToken(userDto, response);

    Optional<Cookie> jwtCookie = findCookie("JWT-SESSION");
    assertThat(jwtCookie).isPresent();
    verifyCookie(jwtCookie.get(), JWT_TOKEN, 3 * 24 * 60 * 60);

    verify(jwtSerializer).encode(jwtArgumentCaptor.capture());
    verifyToken(jwtArgumentCaptor.getValue(), 3 * 24 * 60 * 60, NOW);
    assertThat(threadLocalUserSession.get().isLoggedIn()).isTrue();
  }

  @Test
  public void generate_csrf_state_when_creating_token() throws Exception {
    underTest.generateToken(userDto, response);

    verify(jwtCsrfVerifier).generateState(response, 3 * 24 * 60 * 60);

    verify(jwtSerializer).encode(jwtArgumentCaptor.capture());
    JwtSerializer.JwtSession token = jwtArgumentCaptor.getValue();
    assertThat(token.getProperties().get("xsrfToken")).isEqualTo(CSRF_STATE);
  }

  @Test
  public void generate_token_is_using_session_timeout_from_settings() throws Exception {
    int sessionTimeoutInHours = 10;
    settings.setProperty("sonar.auth.sessionTimeoutInHours", sessionTimeoutInHours);

    underTest = new JwtHttpHandler(system2, dbClient, server, settings, jwtSerializer, jwtCsrfVerifier, threadLocalUserSession);
    underTest.generateToken(userDto, response);

    verify(jwtSerializer).encode(jwtArgumentCaptor.capture());
    verifyToken(jwtArgumentCaptor.getValue(), sessionTimeoutInHours * 60 * 60, NOW);
  }

  @Test
  public void session_timeout_property_cannot_be_updated() throws Exception {
    int firstSessionTimeoutInHours = 10;
    settings.setProperty("sonar.auth.sessionTimeoutInHours", firstSessionTimeoutInHours);

    underTest = new JwtHttpHandler(system2, dbClient, server, settings, jwtSerializer, jwtCsrfVerifier, threadLocalUserSession);
    underTest.generateToken(userDto, response);

    // The property is updated, but it won't be taking into account
    settings.setProperty("sonar.auth.sessionTimeoutInHours", 15);
    underTest.generateToken(userDto, response);
    verify(jwtSerializer, times(2)).encode(jwtArgumentCaptor.capture());
    verifyToken(jwtArgumentCaptor.getAllValues().get(0), firstSessionTimeoutInHours * 60 * 60, NOW);
    verifyToken(jwtArgumentCaptor.getAllValues().get(1), firstSessionTimeoutInHours * 60 * 60, NOW);
  }

  @Test
  public void validate_token() throws Exception {
    addJwtCookie();

    Claims claims = createToken(USER_LOGIN, NOW);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtSerializer, never()).encode(any(JwtSerializer.JwtSession.class));
    assertThat(threadLocalUserSession.get().isLoggedIn()).isTrue();
  }

  @Test
  public void validate_token_refresh_session_when_refresh_time_is_reached() throws Exception {
    addJwtCookie();

    // Token was created 10 days ago and refreshed 6 minutes ago
    Claims claims = createToken(USER_LOGIN, TEN_DAYS_AGO);
    claims.put("lastRefreshTime", SIX_MINUTES_AGO);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtSerializer).refresh(any(Claims.class), eq(3 * 24 * 60 * 60));
    assertThat(threadLocalUserSession.get().isLoggedIn()).isTrue();
  }

  @Test
  public void validate_token_does_not_refresh_session_when_refresh_time_is_not_reached() throws Exception {
    addJwtCookie();

    // Token was created 10 days ago and refreshed 4 minutes ago
    Claims claims = createToken(USER_LOGIN, TEN_DAYS_AGO);
    claims.put("lastRefreshTime", FOUR_MINUTES_AGO);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtSerializer, never()).refresh(any(Claims.class), anyInt());
    assertThat(threadLocalUserSession.get().isLoggedIn()).isTrue();
  }

  @Test
  public void validate_token_removes_session_when_disconnected_timeout_is_reached() throws Exception {
    addJwtCookie();

    // Token was created 4 months ago, refreshed 4 minutes ago, and it expired in 5 minutes
    Claims claims = createToken(USER_LOGIN, NOW - (4L * 30 * 24 * 60 * 60 * 1000));
    claims.setExpiration(new Date(NOW + 5 * 60 * 1000));
    claims.put("lastRefreshTime", FOUR_MINUTES_AGO);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
    assertThat(threadLocalUserSession.get().isLoggedIn()).isFalse();
  }

  @Test
  public void validate_token_fails_with_unauthorized_when_user_is_disabled() throws Exception {
    addJwtCookie();
    UserDto user = addUser(false);

    Claims claims = createToken(user.getLogin(), NOW);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    thrown.expect(UnauthorizedException.class);
    underTest.validateToken(request, response);
  }

  @Test
  public void validate_token_removes_session_when_token_is_no_more_valid() throws Exception {
    addJwtCookie();

    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.empty());

    underTest.validateToken(request, response);

    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
    assertThat(threadLocalUserSession.get().isLoggedIn()).isFalse();
  }

  @Test
  public void validate_token_does_nothing_when_no_jwt_cookie() throws Exception {
    underTest.validateToken(request, response);

    verifyZeroInteractions(httpSession, jwtSerializer);
    assertThat(threadLocalUserSession.get().isLoggedIn()).isFalse();
  }

  @Test
  public void validate_token_does_nothing_when_empty_value_in_jwt_cookie() throws Exception {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("JWT-SESSION", "")});

    underTest.validateToken(request, response);

    verifyZeroInteractions(httpSession, jwtSerializer);
    assertThat(threadLocalUserSession.get().isLoggedIn()).isFalse();
  }

  @Test
  public void validate_token_verify_csrf_state() throws Exception {
    addJwtCookie();
    Claims claims = createToken(USER_LOGIN, NOW);
    claims.put("xsrfToken", CSRF_STATE);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtCsrfVerifier).verifyState(request, CSRF_STATE);
  }

  @Test
  public void validate_token_refresh_state_when_refreshing_token() throws Exception {
    addJwtCookie();

    // Token was created 10 days ago and refreshed 6 minutes ago
    Claims claims = createToken(USER_LOGIN, TEN_DAYS_AGO);
    claims.put("xsrfToken", "CSRF_STATE");
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtSerializer).refresh(any(Claims.class), anyInt());
    verify(jwtCsrfVerifier).refreshState(response,  "CSRF_STATE", 3 * 24 * 60 * 60);
  }

  @Test
  public void validate_token_remove_state_when_removing_token() throws Exception {
    addJwtCookie();
    // Token is invalid => it will be removed
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.empty());

    underTest.validateToken(request, response);

    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
    verify(jwtCsrfVerifier).removeState(response);
  }

  @Test
  public void remove_token() throws Exception {
    underTest.removeToken(response);

    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
    verify(jwtCsrfVerifier).removeState(response);
    assertThat(threadLocalUserSession.get().isLoggedIn()).isFalse();
  }

  @Test
  public void remove_token_is_removing_user_session() throws Exception {
    threadLocalUserSession.set(ServerUserSession.createForUser(dbClient, userDto));

    underTest.removeToken(response);

    assertThat(threadLocalUserSession.get().isLoggedIn()).isFalse();
  }

  private void verifyToken(JwtSerializer.JwtSession token, int expectedExpirationTime, long expectedRefreshTime) {
    assertThat(token.getExpirationTimeInSeconds()).isEqualTo(expectedExpirationTime);
    assertThat(token.getUserLogin()).isEqualTo(USER_LOGIN);
    assertThat(token.getProperties().get("lastRefreshTime")).isEqualTo(expectedRefreshTime);
  }

  private Optional<Cookie> findCookie(String name) {
    verify(response).addCookie(cookieArgumentCaptor.capture());
    return cookieArgumentCaptor.getAllValues().stream()
      .filter(cookie -> name.equals(cookie.getName()))
      .findFirst();
  }

  private void verifyCookie(Cookie cookie, @Nullable String value, int expiry) {
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(expiry);
    assertThat(cookie.getSecure()).isEqualTo(true);
    assertThat(cookie.getValue()).isEqualTo(value);
  }

  private UserDto addUser(boolean active) {
    UserDto user = newUserDto()
      .setActive(active);
    dbClient.userDao().insert(dbSession, user);
    dbSession.commit();
    return user;
  }

  private Cookie addJwtCookie() {
    Cookie cookie = new Cookie("JWT-SESSION", JWT_TOKEN);
    when(request.getCookies()).thenReturn(new Cookie[] {cookie});
    return cookie;
  }

  private Claims createToken(String userLogin, long createdAt) {
    // Expired in 5 minutes by default
    return createToken(userLogin, createdAt, NOW + 5 * 60 * 1000);
  }

  private Claims createToken(String userLogin, long createdAt, long expiredAt) {
    DefaultClaims claims = new DefaultClaims();
    claims.setId("ID");
    claims.setSubject(userLogin);
    claims.setIssuedAt(new Date(createdAt));
    claims.setExpiration(new Date(expiredAt));
    claims.put("lastRefreshTime", createdAt);
    return claims;
  }
}
