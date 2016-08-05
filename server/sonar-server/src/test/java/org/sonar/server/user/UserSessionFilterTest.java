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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.authentication.UserSessionInitializer;
import org.sonar.server.platform.Platform;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UserSessionFilterTest {

  private UserSessionInitializer userSessionInitializer = mock(UserSessionInitializer.class);
  private Platform platform = mock(Platform.class);
  private ComponentContainer componentContainer = mock(ComponentContainer.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private UserSessionFilter underTest = new UserSessionFilter(platform);

  @Before
  public void setUp() {
    when(platform.getContainer()).thenReturn(componentContainer);
  }

  @Test
  public void cleanup_user_session_after_request_handling() throws IOException, ServletException {
    when(componentContainer.getComponentByType(UserSessionInitializer.class)).thenReturn(userSessionInitializer);
    when(userSessionInitializer.initUserSession(request, response)).thenReturn(true);

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(userSessionInitializer).initUserSession(request, response);
    verify(userSessionInitializer).removeUserSession();
  }

  @Test
  public void stop_when_user_session_return_false() throws Exception {
    when(componentContainer.getComponentByType(UserSessionInitializer.class)).thenReturn(userSessionInitializer);
    when(userSessionInitializer.initUserSession(request, response)).thenReturn(false);

    underTest.doFilter(request, response, chain);

    verify(chain, never()).doFilter(request, response);
    verify(userSessionInitializer).initUserSession(request, response);
    verify(userSessionInitializer).removeUserSession();
  }

  @Test
  public void does_nothing_when_not_initialized() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyZeroInteractions(userSessionInitializer);
  }

  @Test
  public void just_for_fun_and_coverage() throws ServletException {
    UserSessionFilter filter = new UserSessionFilter();
    filter.init(mock(FilterConfig.class));
    filter.destroy();
    // do not fail
  }
}
