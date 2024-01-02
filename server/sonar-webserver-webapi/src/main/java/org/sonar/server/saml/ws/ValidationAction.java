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
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.ServletFilter;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.auth.saml.SamlIdentityProvider;
import org.sonar.server.authentication.AuthenticationError;
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.authentication.OAuthCsrfVerifier;
import org.sonar.server.authentication.SamlValidationRedirectionFilter;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.ws.ServletFilterHandler;

import static org.sonar.server.saml.ws.SamlValidationWs.SAML_VALIDATION_CONTROLLER;

public class ValidationAction extends ServletFilter implements SamlAction {

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
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    try {
      oAuthCsrfVerifier.verifyState(httpRequest, httpResponse, samlIdentityProvider, "CSRFToken");
    } catch (AuthenticationException exception) {
      AuthenticationError.handleError(httpRequest, httpResponse, exception.getMessage());
      return;
    }

    if (!userSession.hasSession() || !userSession.isSystemAdministrator()) {
      AuthenticationError.handleError(httpRequest, httpResponse, "User needs to be logged in as system administrator to access this page.");
      return;
    }

    httpRequest = new HttpServletRequestWrapper(httpRequest) {
      @Override
      public StringBuffer getRequestURL() {
        return new StringBuffer(oAuth2ContextFactory.generateCallbackUrl(SamlIdentityProvider.KEY));
      }
    };

    httpResponse.setContentType("text/html");
    httpResponse.getWriter().print(samlAuthenticator.getAuthenticationStatusPage(httpRequest, httpResponse));
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(VALIDATION_CALLBACK_KEY)
      .setInternal(true)
      .setPost(true)
      .setHandler(ServletFilterHandler.INSTANCE)
      .setDescription("Handle the callback of a SAML assertion from the identity Provider and produces " +
        "a HTML page with all information available in the assertion.")
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
