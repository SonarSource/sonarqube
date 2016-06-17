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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;

public class ValidateJwtTokenFilterTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  Settings settings = new Settings();

  ValidateJwtTokenFilter underTest = new ValidateJwtTokenFilter(settings, jwtHttpHandler, userSession);

  @Before
  public void setUp() throws Exception {
    when(request.getContextPath()).thenReturn("");
    when(request.getRequestURI()).thenReturn("/measures");
  }

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern().matches("/")).isTrue();
    assertThat(underTest.doGetPattern().matches("/foo")).isTrue();

    assertThat(underTest.doGetPattern().matches("/api/authentication/login")).isFalse();
    assertThat(underTest.doGetPattern().matches("/batch/index")).isFalse();
    assertThat(underTest.doGetPattern().matches("/batch/file")).isFalse();
    assertThat(underTest.doGetPattern().matches("/batch_bootstrap/index")).isFalse();
    assertThat(underTest.doGetPattern().matches("/maintenance/index")).isFalse();
    assertThat(underTest.doGetPattern().matches("/setup/index")).isFalse();
    assertThat(underTest.doGetPattern().matches("/sessions/new")).isFalse();
    assertThat(underTest.doGetPattern().matches("/sessions/logout")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/system/db_migration_status")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/system/status")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/system/status")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/system/migrate_db")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/server/index")).isFalse();

    // exclude static resources
    assertThat(underTest.doGetPattern().matches("/css/style.css")).isFalse();
    assertThat(underTest.doGetPattern().matches("/fonts/font.ttf")).isFalse();
    assertThat(underTest.doGetPattern().matches("/images/logo.png")).isFalse();
    assertThat(underTest.doGetPattern().matches("/js/jquery.js")).isFalse();
  }

  @Test
  public void validate_session() throws Exception {
    userSession.login("john");
    underTest.doFilter(request, response, chain);

    verify(jwtHttpHandler).validateToken(request, response);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void return_code_401_when_invalid_token_exception() throws Exception {
    userSession.login("john");
    doThrow(new UnauthorizedException("invalid token")).when(jwtHttpHandler).validateToken(request, response);

    underTest.doFilter(request, response, chain);

    verify(response).setStatus(401);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void return_code_401_when_not_authenticated_and_with_force_authentication() throws Exception {
    settings.setProperty("sonar.forceAuthentication", true);
    userSession.anonymous();

    underTest.doFilter(request, response, chain);

    verify(response).setStatus(401);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void return_401_and_stop_on_ws() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/issues");
    doThrow(new UnauthorizedException("invalid token")).when(jwtHttpHandler).validateToken(request, response);

    underTest.doFilter(request, response, chain);

    verify(response).setStatus(401);
    verifyZeroInteractions(chain);
  }

  @Test
  public void return_401_and_stop_on_batch_ws() throws Exception {
    when(request.getRequestURI()).thenReturn("/batch/index");
    doThrow(new UnauthorizedException("invalid token")).when(jwtHttpHandler).validateToken(request, response);

    underTest.doFilter(request, response, chain);

    verify(response).setStatus(401);
    verifyZeroInteractions(chain);
  }

  @Test
  public void ignore_old_batch_ws() throws Exception {
    when(request.getRequestURI()).thenReturn("/batch/name.jar");

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyZeroInteractions(jwtHttpHandler, response);
  }
}
