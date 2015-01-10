/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.user;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserSessionFilterTest {

  @Before
  public void setUp() {
    // for test isolation
    UserSession.remove();
  }

  @Test
  public void should_cleanup_user_session_after_request_handling() throws Exception {
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    ServletResponse httpResponse = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    MockUserSession.set().setUserId(123).setLogin("karadoc");
    assertThat(UserSession.hasSession()).isTrue();
    UserSessionFilter filter = new UserSessionFilter();
    filter.doFilter(httpRequest, httpResponse, chain);

    verify(chain).doFilter(httpRequest, httpResponse);
    assertThat(UserSession.hasSession()).isFalse();
  }

  @Test
  public void just_for_fun_and_coverage() throws Exception {
    UserSessionFilter filter = new UserSessionFilter();
    filter.init(mock(FilterConfig.class));
    filter.destroy();
    // do not fail
  }
}
