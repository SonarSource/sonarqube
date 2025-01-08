/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;

@ServerSide
public class SamlAuthenticator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamlAuthenticator.class);

  private static final String STATE_REQUEST_PARAMETER = "RelayState";

  private final RedirectToUrlProvider redirectToUrlProvider;
  private final SamlResponseAuthenticator samlResponseAuthenticator;
  private final PrincipalToUserIdentityConverter principalToUserIdentityConverter;
  private final SamlStatusChecker samlStatusChecker;
  private final SamlAuthStatusPageGenerator samlAuthStatusPageGenerator;

  public SamlAuthenticator(RedirectToUrlProvider redirectToUrlProvider,
    SamlResponseAuthenticator samlResponseAuthenticator, PrincipalToUserIdentityConverter principalToUserIdentityConverter, SamlStatusChecker samlStatusChecker,
    SamlAuthStatusPageGenerator samlAuthStatusPageGenerator) {
    this.redirectToUrlProvider = redirectToUrlProvider;
    this.samlResponseAuthenticator = samlResponseAuthenticator;
    this.principalToUserIdentityConverter = principalToUserIdentityConverter;
    this.samlStatusChecker = samlStatusChecker;
    this.samlAuthStatusPageGenerator = samlAuthStatusPageGenerator;
  }

  public void initLogin(String callbackUrl, String relayState, HttpRequest request, HttpResponse response) {
    String redirectToUrl = redirectToUrlProvider.getRedirectToUrl(request, callbackUrl, relayState);
    try {
      response.sendRedirect(redirectToUrl);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public UserIdentity onCallback(OAuth2IdentityProvider.CallbackContext context, HttpRequest processedRequest) {
    context.verifyCsrfState(STATE_REQUEST_PARAMETER);

    Saml2AuthenticatedPrincipal principal = samlResponseAuthenticator.authenticate(processedRequest, context.getCallbackUrl());

    LOGGER.trace("Attributes received : {}", principal.getAttributes());
    return principalToUserIdentityConverter.convertToUserIdentity(principal);
  }

  public String getAuthenticationStatusPage(HttpRequest request) {
    try {
      Saml2AuthenticatedPrincipal principal = samlResponseAuthenticator.authenticate(request, request.getRequestURL());
      String samlResponse = request.getParameter("SAMLResponse");
      SamlAuthenticationStatus samlAuthenticationStatus = samlStatusChecker.getSamlAuthenticationStatus(samlResponse, principal);
      return samlAuthStatusPageGenerator.getSamlAuthStatusHtml(request, samlAuthenticationStatus);
    } catch (Saml2AuthenticationException e) {
      SamlAuthenticationStatus samlAuthenticationStatus = samlStatusChecker.getSamlAuthenticationStatus(e.getMessage());
      return samlAuthStatusPageGenerator.getSamlAuthStatusHtml(request, samlAuthenticationStatus);
    } catch (IllegalStateException e) {
      SamlAuthenticationStatus samlAuthenticationStatus = samlStatusChecker.getSamlAuthenticationStatus(String.format("%s due to: %s", e.getMessage(), e.getCause().getMessage()));
      return samlAuthStatusPageGenerator.getSamlAuthStatusHtml(request, samlAuthenticationStatus);
    }
  }
}
