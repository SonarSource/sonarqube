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
import org.sonar.db.user.UserTesting;
import org.sonar.server.tester.UserSessionRule;

public class JwtHttpHandlerTest {

  static final String JWT_TOKEN = "TOKEN";
  static final String USER_LOGIN = "john";
  static final String CSRF_STATE = "CSRF_STATE";

  static final long NOW = 10_000_000_000L;
  static final long FOUR_MINUTES_AGO = NOW - 4 * 60 * 1000L;
  static final long SIX_MINUTES_AGO = NOW - 6 * 60 * 1000L;
  static final long TEN_DAYS_AGO = NOW - 10 * 24 * 60 * 60 * 1000L;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(INSTANCE);

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

  JwtHttpHandler underTest = new JwtHttpHandler(system2, dbClient, server, settings, jwtSerializer, jwtCsrfVerifier);

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
    when(server.isSecured()).thenReturn(true);
    when(server.getContextPath()).thenReturn("");
    when(request.getSession()).thenReturn(httpSession);
    when(jwtSerializer.encode(any(JwtSerializer.JwtSession.class))).thenReturn(JWT_TOKEN);
    when(jwtCsrfVerifier.generateState(eq(response), anyInt())).thenReturn(CSRF_STATE);
  }

  @Test
  public void create_session() throws Exception {
    underTest.generateToken(USER_LOGIN, response);

    Optional<Cookie> jwtCookie = findCookie("JWT-SESSION");
    assertThat(jwtCookie).isPresent();
    verifyCookie(jwtCookie.get(), JWT_TOKEN, 3 * 24 * 60 * 60);

    verify(jwtSerializer).encode(jwtArgumentCaptor.capture());
    verifyToken(jwtArgumentCaptor.getValue(), 3 * 24 * 60 * 60, NOW);
  }

  @Test
  public void generate_csrf_state() throws Exception {
    underTest.generateToken(USER_LOGIN, response);

    verify(jwtCsrfVerifier).generateState(response, 3 * 24 * 60 * 60);

    verify(jwtSerializer).encode(jwtArgumentCaptor.capture());
    JwtSerializer.JwtSession token = jwtArgumentCaptor.getValue();
    assertThat(token.getProperties().get("xsrfToken")).isEqualTo(CSRF_STATE);
  }

  @Test
  public void validate_session() throws Exception {
    addJwtCookie();
    UserDto user = addUser();

    Claims claims = createToken(NOW);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(httpSession).setAttribute("user_id", user.getId());
    verify(jwtSerializer, never()).encode(any(JwtSerializer.JwtSession.class));
  }

  @Test
  public void use_session_timeout_from_settings() throws Exception {
    int sessionTimeoutInHours = 10;
    settings.setProperty("sonar.auth.sessionTimeoutInHours", sessionTimeoutInHours);

    underTest = new JwtHttpHandler(system2, dbClient, server, settings, jwtSerializer, jwtCsrfVerifier);
    underTest.generateToken(USER_LOGIN, response);

    verify(jwtSerializer).encode(jwtArgumentCaptor.capture());
    verifyToken(jwtArgumentCaptor.getValue(), sessionTimeoutInHours * 60 * 60, NOW);
  }

  @Test
  public void session_timeout_property_cannot_be_updated() throws Exception {
    int firstSessionTimeoutInHours = 10;
    settings.setProperty("sonar.auth.sessionTimeoutInHours", firstSessionTimeoutInHours);

    underTest = new JwtHttpHandler(system2, dbClient, server, settings, jwtSerializer, jwtCsrfVerifier);
    underTest.generateToken(USER_LOGIN, response);

    // The property is updated, but it won't be taking into account
    settings.setProperty("sonar.auth.sessionTimeoutInHours", 15);
    underTest.generateToken(USER_LOGIN, response);
    verify(jwtSerializer, times(2)).encode(jwtArgumentCaptor.capture());
    verifyToken(jwtArgumentCaptor.getAllValues().get(0), firstSessionTimeoutInHours * 60 * 60, NOW);
    verifyToken(jwtArgumentCaptor.getAllValues().get(1), firstSessionTimeoutInHours * 60 * 60, NOW);
  }

  @Test
  public void refresh_session_when_refresh_time_is_reached() throws Exception {
    addJwtCookie();
    UserDto user = addUser();

    // Token was created 10 days ago and refreshed 6 minutes ago
    Claims claims = createToken(TEN_DAYS_AGO);
    claims.put("lastRefreshTime", SIX_MINUTES_AGO);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(httpSession).setAttribute("user_id", user.getId());
    verify(jwtSerializer).refresh(any(Claims.class), eq(3 * 24 * 60 * 60));
  }

  @Test
  public void does_not_refresh_session_when_refresh_time_is_not_reached() throws Exception {
    addJwtCookie();
    UserDto user = addUser();

    // Token was created 10 days ago and refreshed 4 minutes ago
    Claims claims = createToken(TEN_DAYS_AGO);
    claims.put("lastRefreshTime", FOUR_MINUTES_AGO);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(httpSession).setAttribute("user_id", user.getId());
    verify(jwtSerializer, never()).refresh(any(Claims.class), anyInt());
  }

  @Test
  public void remove_session_when_disconnected_timeout_is_reached() throws Exception {
    addJwtCookie();
    addUser();

    // Token was created 4 months ago, refreshed 4 minutes ago, and it expired in 5 minutes
    Claims claims = createToken(NOW - (4L * 30 * 24 * 60 * 60 * 1000));
    claims.setExpiration(new Date(NOW + 5 * 60 * 1000));
    claims.put("lastRefreshTime", FOUR_MINUTES_AGO);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(httpSession).removeAttribute("user_id");
    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
  }

  @Test
  public void remove_session_when_user_is_disabled() throws Exception {
    addJwtCookie();
    addUser(false);

    Claims claims = createToken(NOW);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(httpSession).removeAttribute("user_id");
    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
  }

  @Test
  public void remove_session_when_token_is_no_more_valid() throws Exception {
    addJwtCookie();

    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.empty());

    underTest.validateToken(request, response);

    verify(httpSession).removeAttribute("user_id");
    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
  }

  @Test
  public void does_nothing_when_no_jwt_cookie() throws Exception {
    underTest.validateToken(request, response);

    verifyZeroInteractions(httpSession, jwtSerializer);
  }

  @Test
  public void does_nothing_when_empty_value_in_jwt_cookie() throws Exception {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("JWT-SESSION", "")});

    underTest.validateToken(request, response);

    verifyZeroInteractions(httpSession, jwtSerializer);
  }

  @Test
  public void verify_csrf_state() throws Exception {
    addJwtCookie();
    addUser();
    Claims claims = createToken(NOW);
    claims.put("xsrfToken", CSRF_STATE);
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtCsrfVerifier).verifyState(request, CSRF_STATE);
  }

  @Test
  public void refresh_state_when_refreshing_token() throws Exception {
    addJwtCookie();
    addUser();

    // Token was created 10 days ago and refreshed 6 minutes ago
    Claims claims = createToken(TEN_DAYS_AGO);
    claims.put("xsrfToken", "CSRF_STATE");
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.of(claims));

    underTest.validateToken(request, response);

    verify(jwtSerializer).refresh(any(Claims.class), anyInt());
    verify(jwtCsrfVerifier).refreshState(response,  "CSRF_STATE", 3 * 24 * 60 * 60);
  }

  @Test
  public void remove_state_when_removing_token() throws Exception {
    addJwtCookie();
    // Token is invalid => it will be removed
    when(jwtSerializer.decode(JWT_TOKEN)).thenReturn(Optional.empty());

    underTest.validateToken(request, response);

    verifyCookie(findCookie("JWT-SESSION").get(), null, 0);
    verify(jwtCsrfVerifier).removeState(response);
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

  private UserDto addUser() {
    return addUser(true);
  }

  private UserDto addUser(boolean active) {
    UserDto user = UserTesting.newUserDto()
      .setLogin(USER_LOGIN)
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

  private Claims createToken(long createdAt) {
    // Expired in 5 minutes by default
    return createToken(createdAt, NOW + 5 * 60 * 1000);
  }

  private Claims createToken(long createdAt, long expiredAt) {
    DefaultClaims claims = new DefaultClaims();
    claims.setId("ID");
    claims.setSubject(USER_LOGIN);
    claims.setIssuedAt(new Date(createdAt));
    claims.setExpiration(new Date(expiredAt));
    claims.put("lastRefreshTime", createdAt);
    return claims;
  }
}
