/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.auth.saml.SamlIdentityProvider;
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.authentication.OAuthCsrfVerifier;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ValidationActionTest {

  private ValidationAction underTest;
  private SamlAuthenticator samlAuthenticator;
  private ThreadLocalUserSession userSession;
  private OAuthCsrfVerifier oAuthCsrfVerifier;
  private SamlIdentityProvider samlIdentityProvider;
  public static final List<String> CSP_HEADERS = List.of("Content-Security-Policy", "X-Content-Security-Policy", "X-WebKit-CSP");

  @Before
  public void setup() {
    samlAuthenticator = mock(SamlAuthenticator.class);
    userSession = mock(ThreadLocalUserSession.class);
    oAuthCsrfVerifier = mock(OAuthCsrfVerifier.class);
    samlIdentityProvider = mock(SamlIdentityProvider.class);
    var oAuth2ContextFactory = mock(OAuth2ContextFactory.class);
    underTest = new ValidationAction(userSession, samlAuthenticator, oAuth2ContextFactory, samlIdentityProvider, oAuthCsrfVerifier);
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/saml/validation")).isTrue();
    assertThat(underTest.doGetPattern().matches("/saml/validation2")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/saml/validation")).isFalse();
    assertThat(underTest.doGetPattern().matches("/saml/validation_callback2")).isFalse();
    assertThat(underTest.doGetPattern().matches("/saml/")).isFalse();
  }

  @Test
  public void do_filter_admin() throws IOException {
    HttpServletRequest servletRequest = spy(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    doReturn(new PrintWriter(stringWriter)).when(servletResponse).getWriter();
    FilterChain filterChain = mock(FilterChain.class);

    doReturn(true).when(userSession).hasSession();
    doReturn(true).when(userSession).isSystemAdministrator();
    final String mockedHtmlContent = "mocked html content";
    doReturn(mockedHtmlContent).when(samlAuthenticator).getAuthenticationStatusPage(any());

    underTest.doFilter(new JakartaHttpRequest(servletRequest), new JakartaHttpResponse(servletResponse), filterChain);

    verify(samlAuthenticator).getAuthenticationStatusPage(any());
    verify(servletResponse).getWriter();
    CSP_HEADERS.forEach(h -> verify(servletResponse).setHeader(eq(h), anyString()));
    assertEquals(mockedHtmlContent, stringWriter.toString());
  }

  @Test
  public void do_filter_not_authorized() throws IOException {
    HttpRequest servletRequest = spy(HttpRequest.class);
    HttpResponse servletResponse = mock(HttpResponse.class);
    StringWriter stringWriter = new StringWriter();
    doReturn(new PrintWriter(stringWriter)).when(servletResponse).getWriter();
    FilterChain filterChain = mock(FilterChain.class);

    doReturn(true).when(userSession).hasSession();
    doReturn(false).when(userSession).isSystemAdministrator();

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(samlAuthenticator);
  }

  @Test
  public void do_filter_failed_csrf_verification() throws IOException {
    HttpRequest servletRequest = spy(HttpRequest.class);
    HttpResponse servletResponse = mock(HttpResponse.class);
    StringWriter stringWriter = new StringWriter();
    doReturn(new PrintWriter(stringWriter)).when(servletResponse).getWriter();
    FilterChain filterChain = mock(FilterChain.class);

    doReturn("IdentityProviderName").when(samlIdentityProvider).getName();
    doThrow(AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.oauth2(samlIdentityProvider))
      .setMessage("Cookie is missing").build()).when(oAuthCsrfVerifier).verifyState(any(), any(), any(), any());

    doReturn(true).when(userSession).hasSession();
    doReturn(true).when(userSession).isSystemAdministrator();

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(samlAuthenticator);
  }

  @Test
  public void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);
    underTest.define(newController);
    newController.done();

    WebService.Action validationInitAction = context.controller(controllerKey)
      .action(ValidationAction.VALIDATION_CALLBACK_KEY);
    assertThat(validationInitAction).isNotNull();
    assertThat(validationInitAction.description()).isNotEmpty();
    assertThat(validationInitAction.handler()).isNotNull();
  }
}
