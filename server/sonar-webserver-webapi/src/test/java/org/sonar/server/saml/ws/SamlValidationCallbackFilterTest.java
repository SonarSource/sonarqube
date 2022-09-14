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

package org.sonar.server.saml.ws;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SamlValidationCallbackFilterTest {

  private SamlValidationCallbackFilter underTest;
  private SamlAuthenticator samlAuthenticator;
  private ThreadLocalUserSession userSession;

  @Before
  public void setup() {
    samlAuthenticator = mock(SamlAuthenticator.class);
    userSession = mock(ThreadLocalUserSession.class);
    var oAuth2ContextFactory = mock(OAuth2ContextFactory.class);
    underTest = new SamlValidationCallbackFilter(userSession, samlAuthenticator, oAuth2ContextFactory);
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/saml/validation_callback")).isTrue();
    assertThat(underTest.doGetPattern().matches("/saml/validation_callback2")).isFalse();
    assertThat(underTest.doGetPattern().matches("/saml/")).isFalse();
  }

  @Test
  public void do_filter_admin() throws ServletException, IOException {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    doReturn(new PrintWriter(stringWriter)).when(servletResponse).getWriter();
    FilterChain filterChain = mock(FilterChain.class);

    doReturn(true).when(userSession).hasSession();
    doReturn(true).when(userSession).isSystemAdministrator();

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(samlAuthenticator).getAuthenticationStatusPage(any(), any());
    verify(servletResponse).getWriter();
  }

  @Test
  public void do_filter_not_authorized() throws ServletException, IOException {
    HttpServletRequest servletRequest = spy(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    doReturn(new PrintWriter(stringWriter)).when(servletResponse).getWriter();
    FilterChain filterChain = mock(FilterChain.class);

    doReturn(true).when(userSession).hasSession();
    doReturn(false).when(userSession).isSystemAdministrator();

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(samlAuthenticator);
  }
}
