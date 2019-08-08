/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.platform.web;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.DBSessions;
import org.sonar.server.authentication.UserSessionInitializer;
import org.sonar.server.organization.DefaultOrganizationCache;
import org.sonar.server.platform.Platform;
import org.sonar.server.setting.ThreadLocalSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UserSessionFilterTest {

  private UserSessionInitializer userSessionInitializer = mock(UserSessionInitializer.class);
  private ComponentContainer container = new ComponentContainer();
  private Platform platform = mock(Platform.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);
  private DBSessions dbSessions = mock(DBSessions.class);
  private ThreadLocalSettings settings = mock(ThreadLocalSettings.class);
  private DefaultOrganizationCache defaultOrganizationCache = mock(DefaultOrganizationCache.class);
  private UserSessionFilter underTest = new UserSessionFilter(platform);

  @Before
  public void setUp() {
    container.add(dbSessions, settings, defaultOrganizationCache);
    when(platform.getContainer()).thenReturn(container);
  }

  @Test
  public void cleanup_user_session_after_request_handling() throws IOException, ServletException {
    mockUserSessionInitializer(true);

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(userSessionInitializer).initUserSession(request, response);
  }

  @Test
  public void stop_when_user_session_return_false() throws Exception {
    mockUserSessionInitializer(false);

    underTest.doFilter(request, response, chain);

    verify(chain, never()).doFilter(request, response);
    verify(userSessionInitializer).initUserSession(request, response);
  }

  @Test
  public void does_nothing_when_not_initialized() throws Exception {
    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyZeroInteractions(userSessionInitializer);
  }

  @Test
  public void doFilter_loads_and_unloads_settings() throws Exception {
    underTest.doFilter(request, response, chain);

    InOrder inOrder = inOrder(settings);
    inOrder.verify(settings).load();
    inOrder.verify(settings).unload();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void doFilter_unloads_Settings_even_if_chain_throws_exception() throws Exception {
    RuntimeException thrown = mockChainDoFilterError();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(settings).unload();
    }
  }

  @Test
  public void doFilter_enables_and_disables_caching_in_DbSessions() throws Exception {
    underTest.doFilter(request, response, chain);

    InOrder inOrder = inOrder(dbSessions);
    inOrder.verify(dbSessions).enableCaching();
    inOrder.verify(dbSessions).disableCaching();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void doFilter_disables_caching_in_DbSessions_even_if_chain_throws_exception() throws Exception {
    RuntimeException thrown = mockChainDoFilterError();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(dbSessions).disableCaching();
    }
  }

  @Test
  public void doFilter_unloads_Settings_even_if_DefaultOrganizationCache_unload_fails() throws Exception {
    RuntimeException thrown = new RuntimeException("Faking DefaultOrganizationCache.unload failing");
    doThrow(thrown)
        .when(defaultOrganizationCache)
        .unload();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(settings).unload();
    }
  }

  @Test
  public void doFilter_unloads_Settings_even_if_UserSessionInitializer_removeUserSession_fails() throws Exception {
    RuntimeException thrown = mockUserSessionInitializerRemoveUserSessionFailing();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(settings).unload();
    }
  }

  @Test
  public void doFilter_loads_and_unloads_DefaultOrganizationCache() throws Exception {
    underTest.doFilter(request, response, chain);

    InOrder inOrder = inOrder(defaultOrganizationCache);
    inOrder.verify(defaultOrganizationCache).load();
    inOrder.verify(defaultOrganizationCache).unload();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void doFilter_unloads_DefaultOrganizationCache_even_if_chain_throws_exception() throws Exception {
    RuntimeException thrown = mockChainDoFilterError();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(defaultOrganizationCache).unload();
    }
  }

  @Test
  public void doFilter_unloads_DefaultOrganizationCache_even_if_Settings_unload_fails() throws Exception {
    RuntimeException thrown = new RuntimeException("Faking Settings.unload failing");
    doThrow(thrown)
        .when(settings)
        .unload();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(defaultOrganizationCache).unload();
    }
  }

  @Test
  public void doFilter_unloads_DefaultOrganizationCache_even_if_UserSessionInitializer_removeUserSession_fails() throws Exception {
    RuntimeException thrown = mockUserSessionInitializerRemoveUserSessionFailing();

    try {
      underTest.doFilter(request, response, chain);
      fail("A RuntimeException should have been thrown");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(thrown);
      verify(defaultOrganizationCache).unload();
    }
  }

  @Test
  public void just_for_fun_and_coverage() throws ServletException {
    UserSessionFilter filter = new UserSessionFilter();
    filter.init(mock(FilterConfig.class));
    filter.destroy();
    // do not fail
  }

  private void mockUserSessionInitializer(boolean value) {
    container.add(userSessionInitializer);
    when(userSessionInitializer.initUserSession(request, response)).thenReturn(value);
  }

  private RuntimeException mockUserSessionInitializerRemoveUserSessionFailing() {
    container.add(userSessionInitializer);
    RuntimeException thrown = new RuntimeException("Faking UserSessionInitializer.removeUserSession failing");
    doThrow(thrown)
        .when(userSessionInitializer)
        .removeUserSession();
    return thrown;
  }

  private RuntimeException mockChainDoFilterError() throws IOException, ServletException {
    RuntimeException thrown = new RuntimeException("Faking chain.doFilter failing");
    doThrow(thrown)
        .when(chain)
        .doFilter(request, response);
    return thrown;
  }
}
