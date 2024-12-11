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
import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.auth.saml.SamlIdentityProvider;
import org.sonar.server.authentication.AuthenticationError;
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.authentication.OAuthCsrfVerifier;
import org.sonar.server.authentication.SamlValidationCspHeaders;
import org.sonar.server.authentication.SamlValidationRedirectionFilter;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.ws.ServletFilterHandler;

import static org.sonar.server.saml.ws.SamlValidationWs.SAML_VALIDATION_CONTROLLER;

public class ValidationAction extends HttpFilter implements SamlAction {

  static final String VALIDATION_CALLBACK_KEY = SamlValidationRedirectionFilter.SAML_VALIDATION_KEY;
  private static final String URL_DELIMITER = "/";
  private final ThreadLocalUserSession userSession;
  private final SamlAuthenticator samlAuthenticator;
  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final SamlIdentityProvider samlIdentityProvider;
  private final OAuthCsrfVerifier oAuthCsrfVerifier;

  public ValidationAction(ThreadLocalUserSession userSession, SamlAuthenticator samlAuthenticator, OAuth2ContextFactory oAuth2ContextFactory,
    SamlIdentityProvider samlIdentityProvider, OAuthCsrfVerifier oAuthCsrfVerifier) {
    this.samlAuthenticator = samlAuthenticator;
    this.userSession = userSession;
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.samlIdentityProvider = samlIdentityProvider;
    this.oAuthCsrfVerifier = oAuthCsrfVerifier;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(composeUrlPattern(SAML_VALIDATION_CONTROLLER, VALIDATION_CALLBACK_KEY));
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) throws IOException {
    try {
      oAuthCsrfVerifier.verifyState(request, response, samlIdentityProvider, "CSRFToken");
    } catch (AuthenticationException exception) {
      AuthenticationError.handleError(request, response, exception.getMessage());
      return;
    }

    if (!userSession.hasSession() || !userSession.isSystemAdministrator()) {
      AuthenticationError.handleError(request, response, "User needs to be logged in as system administrator to access this page.");
      return;
    }

    HttpServletRequest httpRequest = new HttpServletRequestWrapper(((JakartaHttpRequest) request).getDelegate()) {
      @Override
      public StringBuffer getRequestURL() {
        return new StringBuffer(oAuth2ContextFactory.generateCallbackUrl(SamlIdentityProvider.KEY));
      }
    };

    response.setContentType("text/html");

    String htmlResponse = samlAuthenticator.getAuthenticationStatusPage(new JakartaHttpRequest(httpRequest), response);
    String nonce = SamlValidationCspHeaders.addCspHeadersWithNonceToResponse(response);
    htmlResponse = htmlResponse.replace("%NONCE%", nonce);
    response.getWriter().print(htmlResponse);
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(VALIDATION_CALLBACK_KEY)
      .setInternal(true)
      .setPost(true)
      .setHandler(ServletFilterHandler.INSTANCE)
      .setDescription("Handle the callback of a SAML assertion from the identity Provider and produces " +
        "an HTML page with all information available in the assertion.")
      .setSince("9.7");
    action.createParam("SAMLResponse")
      .setDescription("SAML assertion value")
      .setRequired(true);
  }

  private static String composeUrlPattern(String... parameters) {
    return Arrays
      .stream(parameters)
      .map(URL_DELIMITER::concat)
      .collect(Collectors.joining());
  }
}
