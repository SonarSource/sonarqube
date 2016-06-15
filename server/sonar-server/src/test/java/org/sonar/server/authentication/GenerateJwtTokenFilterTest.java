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
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpMethod.POST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.tester.UserSessionRule;

public class GenerateJwtTokenFilterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);

  JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);

  GenerateJwtTokenFilter underTest = new GenerateJwtTokenFilter(jwtHttpHandler, userSession);

  @Before
  public void setUp() throws Exception {
    when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
  }

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern().matches("/sessions/login")).isTrue();
    assertThat(underTest.doGetPattern().matches("/")).isFalse();
  }

  @Test
  public void create_session_when_post_request_and_user_is_authenticated() throws Exception {
    executePostRequest();
    userSession.login("john");

    underTest.doFilter(request, response, chain);

    verify(jwtHttpHandler).generateToken("john", response);
  }

  @Test
  public void does_nothing_on_get_request() throws Exception {
    executeGetRequest();
    userSession.login("john");

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(jwtHttpHandler);
  }

  @Test
  public void does_nothing_when_user_is_not_authenticated() throws Exception {
    executePostRequest();
    userSession.anonymous();

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(jwtHttpHandler);
  }

  private void executePostRequest() {
    when(request.getMethod()).thenReturn(POST.getName());
  }

  private void executeGetRequest() {
    when(request.getMethod()).thenReturn(GET.getName());
  }

}
