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
package org.sonar.server.user;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.platform.Platform;
import org.sonar.server.tester.MockUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserSessionFilterTest {
  private ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();
  private Platform platform = mock(Platform.class);
  private ComponentContainer componentContainer = mock(ComponentContainer.class);
  private HttpServletRequest httpRequest = mock(HttpServletRequest.class);
  private ServletResponse httpResponse = mock(ServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  @Before
  public void setUp() {
    when(platform.getContainer()).thenReturn(componentContainer);
    // for test isolation
    threadLocalUserSession.remove();
  }

  @After
  public void tearDown() {
    threadLocalUserSession.remove();
  }

  @Test
  public void should_cleanup_user_session_after_request_handling() throws IOException, ServletException {
    when(componentContainer.getComponentByType(ThreadLocalUserSession.class)).thenReturn(threadLocalUserSession);

    threadLocalUserSession.set(new MockUserSession("karadoc").setUserId(123));
    assertThat(threadLocalUserSession.hasSession()).isTrue();
    UserSessionFilter filter = new UserSessionFilter(platform);
    filter.doFilter(httpRequest, httpResponse, chain);

    verify(chain).doFilter(httpRequest, httpResponse);
    assertThat(threadLocalUserSession.hasSession()).isFalse();
  }

  @Test
  public void does_not_fail_if_container_has_no_ThreadLocalUserSession() throws Exception {
    UserSessionFilter filter = new UserSessionFilter(platform);
    filter.doFilter(httpRequest, httpResponse, chain);
  }

  @Test
  public void just_for_fun_and_coverage() throws ServletException {
    UserSessionFilter filter = new UserSessionFilter(platform);
    filter.init(mock(FilterConfig.class));
    filter.destroy();
    // do not fail
  }
}
