/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class UserSessionFilterTest {

  @Before
  public void setUp() {
    // for test isolation
    UserSession.set(null);
  }

  @Test
  public void should_load_user_session() throws Exception {
    HttpSession httpSession = mock(HttpSession.class);
    // JRuby sets a long but not an integer
    when(httpSession.getAttribute("user_id")).thenReturn(123L);
    when(httpSession.getAttribute("login")).thenReturn("karadoc");
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getSession(true)).thenReturn(httpSession);
    ServletResponse httpResponse = mock(ServletResponse.class);

    FilterChain chain = mock(FilterChain.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        assertThat(UserSession.get()).isNotNull();
        assertThat(UserSession.get().login()).isEqualTo("karadoc");
        assertThat(UserSession.get().userId()).isEqualTo(123);
        assertThat(UserSession.get().isLoggedIn()).isTrue();
        return null;
      }
    }).when(chain).doFilter(httpRequest, httpResponse);

    UserSessionFilter filter = new UserSessionFilter();
    filter.doFilter(httpRequest, httpResponse, chain);

    verify(chain).doFilter(httpRequest, httpResponse);
  }

  /**
   * UserSession should always be set, even when end-user  is not logged in.
   */
  @Test
  public void should_load_anonymous_session() throws Exception {
    HttpSession httpSession = mock(HttpSession.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    when(httpRequest.getSession(true)).thenReturn(httpSession);
    ServletResponse httpResponse = mock(ServletResponse.class);

    FilterChain chain = mock(FilterChain.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        assertThat(UserSession.get()).isNotNull();
        assertThat(UserSession.get().login()).isNull();
        assertThat(UserSession.get().userId()).isNull();
        assertThat(UserSession.get().isLoggedIn()).isFalse();
        return null;
      }
    }).when(chain).doFilter(httpRequest, httpResponse);

    UserSessionFilter filter = new UserSessionFilter();
    filter.doFilter(httpRequest, httpResponse, chain);

    verify(chain).doFilter(httpRequest, httpResponse);
  }

  @Test
  public void just_for_fun_and_coverage() throws Exception {
    UserSessionFilter filter = new UserSessionFilter();
    filter.init(mock(FilterConfig.class));
    filter.destroy();
    // do not fail
  }
}
