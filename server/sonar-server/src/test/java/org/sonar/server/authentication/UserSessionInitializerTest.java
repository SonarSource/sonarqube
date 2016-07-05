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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.ServerUserSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

public class UserSessionInitializerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  ThreadLocalUserSession userSession = mock(ThreadLocalUserSession.class);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);

  JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  BasicAuthenticator basicAuthenticator = mock(BasicAuthenticator.class);

  Settings settings = new Settings();

  UserDto user = newUserDto();

  UserSessionInitializer underTest = new UserSessionInitializer(dbClient, settings, jwtHttpHandler, basicAuthenticator, userSession);

  @Before
  public void setUp() throws Exception {
    dbClient.userDao().insert(dbSession, user);
    dbSession.commit();
    when(request.getContextPath()).thenReturn("");
    when(request.getRequestURI()).thenReturn("/measures");
  }

  @Test
  public void check_urls() throws Exception {
    assertPathIsNotIgnored("/");
    assertPathIsNotIgnored("/foo");

    assertPathIsIgnored("/api/authentication/login");
    assertPathIsIgnored("/api/authentication/validate");
    assertPathIsIgnored("/batch/index");
    assertPathIsIgnored("/batch/file");
    assertPathIsIgnored("/maintenance/index");
    assertPathIsIgnored("/setup/index");
    assertPathIsIgnored("/sessions/new");
    assertPathIsIgnored("/sessions/logout");
    assertPathIsIgnored("/api/system/db_migration_status");
    assertPathIsIgnored("/api/system/status");
    assertPathIsIgnored("/api/system/migrate_db");
    assertPathIsIgnored("/api/server/index");

    // exclude static resources
    assertPathIsIgnored("/css/style.css");
    assertPathIsIgnored("/fonts/font.ttf");
    assertPathIsIgnored("/images/logo.png");
    assertPathIsIgnored("/js/jquery.js");
  }

  @Test
  public void validate_session_from_token() throws Exception {
    when(userSession.isLoggedIn()).thenReturn(true);
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.of(user));

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(jwtHttpHandler).validateToken(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void validate_session_from_basic_authentication() throws Exception {
    when(userSession.isLoggedIn()).thenReturn(false).thenReturn(true);
    when(basicAuthenticator.authenticate(request)).thenReturn(Optional.of(user));
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(jwtHttpHandler).validateToken(request, response);
    verify(basicAuthenticator).authenticate(request);
    verify(userSession).set(any(ServerUserSession.class));
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void return_code_401_when_invalid_token_exception() throws Exception {
    doThrow(new UnauthorizedException("invalid token")).when(jwtHttpHandler).validateToken(request, response);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(response).setStatus(401);
    verifyZeroInteractions(userSession);
  }

  @Test
  public void return_code_401_when_not_authenticated_and_with_force_authentication() throws Exception {
    when(userSession.isLoggedIn()).thenReturn(false);
    when(basicAuthenticator.authenticate(request)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());
    settings.setProperty("sonar.forceAuthentication", true);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(response).setStatus(401);
    verifyZeroInteractions(userSession);
  }

  @Test
  public void return_401_and_stop_on_ws() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/issues");
    doThrow(new UnauthorizedException("invalid token")).when(jwtHttpHandler).validateToken(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verifyZeroInteractions(userSession);
  }

  @Test
  public void return_401_and_stop_on_batch_ws() throws Exception {
    when(request.getRequestURI()).thenReturn("/batch/global");
    doThrow(new UnauthorizedException("invalid token")).when(jwtHttpHandler).validateToken(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verifyZeroInteractions(userSession);
  }

  @Test
  public void remove_user_session() throws Exception {
    underTest.removeUserSession();

    verify(userSession).remove();
  }

  private void assertPathIsIgnored(String path) {
    when(request.getRequestURI()).thenReturn(path);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verifyZeroInteractions(userSession, jwtHttpHandler, basicAuthenticator);
    reset(userSession, jwtHttpHandler, basicAuthenticator);
  }

  private void assertPathIsNotIgnored(String path) {
    when(request.getRequestURI()).thenReturn(path);
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.of(user));

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(userSession).set(any(UserSession.class));
    reset(userSession, jwtHttpHandler, basicAuthenticator);
  }
}
