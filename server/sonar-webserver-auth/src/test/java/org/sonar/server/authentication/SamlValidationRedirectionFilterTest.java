/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

package org.sonar.server.authentication;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SamlValidationRedirectionFilterTest {

  SamlValidationRedirectionFilter underTest;

  @Before
  public void setup() {
    Server server = mock(Server.class);
    doReturn("").when(server).getContextPath();
    underTest = new SamlValidationRedirectionFilter(server);
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/oauth2/callback/saml")).isTrue();
    assertThat(underTest.doGetPattern().matches("/oauth2/callback/")).isFalse();
    assertThat(underTest.doGetPattern().matches("/oauth2/callback/test")).isFalse();
    assertThat(underTest.doGetPattern().matches("/oauth2/")).isFalse();
  }

  @Test
  public void do_filter_validation_relay_state() throws ServletException, IOException {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    doReturn("validation-query").when(servletRequest).getParameter("RelayState");
    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(servletResponse).setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    verify(servletResponse).setHeader("Location", "/saml/validation_callback");
  }

  @Test
  public void do_filter_no_validation_relay_state() throws ServletException, IOException {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    doReturn("random_query").when(servletRequest).getParameter("RelayState");
    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(servletResponse);
  }
}
