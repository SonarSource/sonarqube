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
package org.sonar.auth.saml;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SamlAuthenticatorTest {

  public static final String CALLBACK_URL = "callbackUrl";
  public static final String RELAY_STATE = "relayState";
  public static final String REDIRECT_URL = "redirectUrl";

  @Mock
  private RedirectToUrlProvider redirectToUrlProvider;
  @Mock
  private SamlResponseAuthenticator samlResponseAuthenticator;
  @Mock
  private PrincipalToUserIdentityConverter principalToUserIdentityConverter;
  @Mock
  private SamlStatusChecker samlStatusChecker;
  @Mock
  private SamlAuthStatusPageGenerator samlAuthStatusPageGenerator;
  @Mock
  private SamlAuthenticationStatus samlAuthenticationStatus;

  @InjectMocks
  private SamlAuthenticator samlAuthenticator;

  @Mock
  private JakartaHttpRequest request;
  @Mock
  private JakartaHttpResponse response;

  @Test
  void initLogin_generatesUrlAndSendsRedirect() throws IOException {
    when(redirectToUrlProvider.getRedirectToUrl(request, CALLBACK_URL, RELAY_STATE)).thenReturn(REDIRECT_URL);

    samlAuthenticator.initLogin(CALLBACK_URL, RELAY_STATE, request, response);

    verify(response).sendRedirect(REDIRECT_URL);
  }

  @Test
  void initLogin_whenIoException_convertsToRuntimeException() throws IOException {
    when(redirectToUrlProvider.getRedirectToUrl(request, CALLBACK_URL, RELAY_STATE)).thenReturn(REDIRECT_URL);

    IOException ioException = new IOException();
    doThrow(ioException).when(response).sendRedirect(REDIRECT_URL);

    assertThatException()
      .isThrownBy(() -> samlAuthenticator.initLogin(CALLBACK_URL, RELAY_STATE, request, response))
      .isInstanceOf(UncheckedIOException.class)
      .withCause(ioException);
  }

  @Test
  void onCallback_verifiesCsrfStateAndAuthenticates() {
    OAuth2IdentityProvider.CallbackContext context = mock();
    when(context.getCallbackUrl()).thenReturn(CALLBACK_URL);

    Saml2AuthenticatedPrincipal principal = mock();
    when(samlResponseAuthenticator.authenticate(request, CALLBACK_URL)).thenReturn(principal);

    UserIdentity userIdentity = mock();
    when(principalToUserIdentityConverter.convertToUserIdentity(principal)).thenReturn(userIdentity);

    UserIdentity actualUserIdentity = samlAuthenticator.onCallback(context, request);

    verify(context).verifyCsrfState("RelayState");
    verify(samlResponseAuthenticator).authenticate(request, CALLBACK_URL);
    assertThat(actualUserIdentity).isEqualTo(userIdentity);
  }

  @Test
  void getAuthenticationStatusPage_returnsHtml() {
    String samlResponse = "samlResponse";
    when(request.getRequestURL()).thenReturn("url");
    when(request.getParameter("SAMLResponse")).thenReturn(samlResponse);

    Saml2AuthenticatedPrincipal principal = mock();
    when(samlResponseAuthenticator.authenticate(request, request.getRequestURL())).thenReturn(principal);

    when(samlStatusChecker.getSamlAuthenticationStatus(samlResponse, principal)).thenReturn(samlAuthenticationStatus);
    when(samlAuthStatusPageGenerator.getSamlAuthStatusHtml(request, samlAuthenticationStatus)).thenReturn("html");

    String authenticationStatusPage = samlAuthenticator.getAuthenticationStatusPage(request);

    assertThat(authenticationStatusPage).isEqualTo("html");
  }

  @Test
  void getAuthenticationStatusPage_whenSaml2AuthenticationException_returnsHtml() {
    when(request.getRequestURL()).thenReturn("url");

    String errorMessage = "error";
    when(samlResponseAuthenticator.authenticate(request, request.getRequestURL())).thenThrow(new Saml2AuthenticationException(new Saml2Error("erorCode", errorMessage)));

    when(samlStatusChecker.getSamlAuthenticationStatus(errorMessage)).thenReturn(samlAuthenticationStatus);
    when(samlAuthStatusPageGenerator.getSamlAuthStatusHtml(request, samlAuthenticationStatus)).thenReturn("html");

    String authenticationStatusPage = samlAuthenticator.getAuthenticationStatusPage(request);

    assertThat(authenticationStatusPage).isEqualTo("html");
  }

  @Test
  void getAuthenticationStatusPage_whenIllegalStateException_returnsHtml() {
    when(request.getRequestURL()).thenReturn("url");

    RuntimeException runtimeException = new RuntimeException("error");
    IllegalStateException illegalStateException = new IllegalStateException(runtimeException);
    when(samlResponseAuthenticator.authenticate(request, request.getRequestURL())).thenThrow(illegalStateException);

    when(samlStatusChecker.getSamlAuthenticationStatus(any())).thenReturn(samlAuthenticationStatus);
    when(samlAuthStatusPageGenerator.getSamlAuthStatusHtml(request, samlAuthenticationStatus)).thenReturn("html");

    String authenticationStatusPage = samlAuthenticator.getAuthenticationStatusPage(request);

    assertThat(authenticationStatusPage).isEqualTo("html");
    verify(samlAuthStatusPageGenerator).getSamlAuthStatusHtml(request, samlAuthenticationStatus);
  }

}
