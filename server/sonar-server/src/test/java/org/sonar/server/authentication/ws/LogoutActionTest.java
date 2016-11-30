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

package org.sonar.server.authentication.ws;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.authentication.JwtHttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LogoutActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  LogoutAction underTest = new LogoutAction(jwtHttpHandler);

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern().matches("/api/authentication/logout")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/authentication/login")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/authentication/logou")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/authentication/logoutthing")).isFalse();
    assertThat(underTest.doGetPattern().matches("/foo")).isFalse();
  }

  @Test
  public void return_400_on_get_request() throws Exception {
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(jwtHttpHandler, chain);
    verify(response).setStatus(400);
  }

  @Test
  public void logout() throws Exception {
    executeRequest();

    verify(jwtHttpHandler).removeToken(request, response);
    verifyZeroInteractions(chain);
  }

  private void executeRequest() throws IOException, ServletException {
    when(request.getMethod()).thenReturn("POST");
    underTest.doFilter(request, response, chain);
  }
}
