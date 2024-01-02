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
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.authentication.OAuthCsrfVerifier;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ValidationInitActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private ValidationInitAction underTest;
  private SamlAuthenticator samlAuthenticator;
  private OAuth2ContextFactory oAuth2ContextFactory;
  private OAuthCsrfVerifier oAuthCsrfVerifier;

  @Before
  public void setUp() throws Exception {
    samlAuthenticator = mock(SamlAuthenticator.class);
    oAuth2ContextFactory = mock(OAuth2ContextFactory.class);
    oAuthCsrfVerifier = mock(OAuthCsrfVerifier.class);
    underTest = new ValidationInitAction(samlAuthenticator, oAuthCsrfVerifier, oAuth2ContextFactory, userSession);
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/saml/validation_init")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/saml")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/saml/validation_init")).isFalse();
    assertThat(underTest.doGetPattern().matches("/saml/validation_init2")).isFalse();
  }

  @Test
  public void do_filter_as_admin() throws IOException, ServletException {
    userSession.logIn().setSystemAdministrator();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    String callbackUrl = "http://localhost:9000/api/validation_test";

    mockCsrfTokenGeneration(servletRequest, servletResponse);
    when(oAuth2ContextFactory.generateCallbackUrl(anyString())).thenReturn(callbackUrl);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(samlAuthenticator).initLogin(matches(callbackUrl),
      matches(ValidationInitAction.VALIDATION_RELAY_STATE),
      any(), any());
  }

  @Test
  public void do_filter_as_admin_with_init_issues() throws IOException, ServletException {
    userSession.logIn().setSystemAdministrator();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    String callbackUrl = "http://localhost:9000/api/validation_test";
    when(oAuth2ContextFactory.generateCallbackUrl(anyString()))
      .thenReturn(callbackUrl);

    mockCsrfTokenGeneration(servletRequest, servletResponse);
    doThrow(new IllegalStateException()).when(samlAuthenticator).initLogin(any(), any(), any(), any());

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(servletResponse).sendRedirect("/saml/validation");
  }

  @Test
  public void do_filter_as_not_admin() throws IOException, ServletException {
    userSession.logIn();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    String callbackUrl = "http://localhost:9000/api/validation_test";
    when(oAuth2ContextFactory.generateCallbackUrl(anyString()))
      .thenReturn(callbackUrl);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(samlAuthenticator);
    verify(servletResponse).sendRedirect(anyString());
  }

  @Test
  public void do_filter_as_anonymous() throws IOException, ServletException {
    userSession.anonymous();
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    String callbackUrl = "http://localhost:9000/api/validation_test";
    when(oAuth2ContextFactory.generateCallbackUrl(anyString()))
      .thenReturn(callbackUrl);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(samlAuthenticator);
    verify(servletResponse).sendRedirect(anyString());
  }

  @Test
  public void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);
    underTest.define(newController);
    newController.done();

    WebService.Action validationInitAction = context.controller(controllerKey).action("validation_init");
    assertThat(validationInitAction).isNotNull();
    assertThat(validationInitAction.description()).isNotEmpty();
    assertThat(validationInitAction.handler()).isNotNull();
  }

  private void mockCsrfTokenGeneration(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    when(oAuthCsrfVerifier.generateState(servletRequest, servletResponse)).thenReturn("CSRF_TOKEN");
  }
}
