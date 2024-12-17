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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;

import static org.sonar.auth.saml.SamlAuthStatusPageGenerator.getSamlAuthStatusHtml;
import static org.sonar.auth.saml.SamlStatusChecker.getSamlAuthenticationStatus;

@ServerSide
public class SamlAuthenticator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamlAuthenticator.class);

  private static final String STATE_REQUEST_PARAMETER = "RelayState";

  private final SamlSettings samlSettings;
  private final SamlMessageIdChecker samlMessageIdChecker;
  private final RedirectToUrlProvider redirectToUrlProvider;
  private final SamlResponseAuthenticator samlResponseAuthenticator;
  private final PrincipalToUserIdentityConverter principalToUserIdentityConverter;

  public SamlAuthenticator(SamlSettings samlSettings, SamlMessageIdChecker samlMessageIdChecker, RedirectToUrlProvider redirectToUrlProvider,
    SamlResponseAuthenticator samlResponseAuthenticator, PrincipalToUserIdentityConverter principalToUserIdentityConverter) {
    this.samlSettings = samlSettings;
    this.samlMessageIdChecker = samlMessageIdChecker;
    this.redirectToUrlProvider = redirectToUrlProvider;
    this.samlResponseAuthenticator = samlResponseAuthenticator;
    this.principalToUserIdentityConverter = principalToUserIdentityConverter;
  }

  public void initLogin(String callbackUrl, String relayState, HttpRequest request, HttpResponse response) {
    String redirectToUrl = redirectToUrlProvider.getRedirectToUrl(request, callbackUrl, relayState);
    try {
      response.sendRedirect(redirectToUrl);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public UserIdentity onCallback(OAuth2IdentityProvider.CallbackContext context, HttpRequest processedRequest) {
    context.verifyCsrfState(STATE_REQUEST_PARAMETER);

    Saml2AuthenticatedPrincipal principal = samlResponseAuthenticator.authenticate(processedRequest, context.getCallbackUrl());

    //LOGGER.trace("Name ID : {}", getNameId(auth)); //TODO extract nameid
    // this.checkMessageId(auth);

    //TODO create class to convert Saml2AuthenticatedPrincipal to UserIdentity
    LOGGER.trace("Attributes received : {}", principal.getAttributes());
    return principalToUserIdentityConverter.convertToUserIdentity(principal);
  }

  public String getAuthenticationStatusPage(HttpRequest request, HttpResponse response) {
    try {
      Saml2AuthenticatedPrincipal principal = samlResponseAuthenticator.authenticate(request, request.getRequestURL());
      String samlResponse = request.getParameter("SAMLResponse");
      return getSamlAuthStatusHtml(request, getSamlAuthenticationStatus(samlResponse, principal, samlSettings));
    } catch (Saml2AuthenticationException e) {
      return getSamlAuthStatusHtml(request, getSamlAuthenticationStatus(e.getMessage()));
    } catch (IllegalStateException e) {
      return getSamlAuthStatusHtml(request, getSamlAuthenticationStatus(String.format("%s due to: %s", e.getMessage(), e.getCause().getMessage())));
    }
  }
}
