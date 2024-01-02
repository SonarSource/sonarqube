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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.ServletFilter;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.auth.saml.SamlIdentityProvider;
import org.sonar.server.authentication.AuthenticationError;
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.authentication.OAuthCsrfVerifier;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.ServletFilterHandler;

import static org.sonar.server.authentication.SamlValidationRedirectionFilter.SAML_VALIDATION_CONTROLLER_CONTEXT;
import static org.sonar.server.authentication.SamlValidationRedirectionFilter.SAML_VALIDATION_KEY;

public class ValidationInitAction extends ServletFilter implements SamlAction {

  public static final String VALIDATION_RELAY_STATE = "validation-query";
  public static final String VALIDATION_INIT_KEY = "validation_init";
  private final SamlAuthenticator samlAuthenticator;
  private final OAuthCsrfVerifier oAuthCsrfVerifier;
  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final UserSession userSession;

  public ValidationInitAction(SamlAuthenticator samlAuthenticator, OAuthCsrfVerifier oAuthCsrfVerifier, OAuth2ContextFactory oAuth2ContextFactory, UserSession userSession) {
    this.samlAuthenticator = samlAuthenticator;
    this.oAuthCsrfVerifier = oAuthCsrfVerifier;
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.userSession = userSession;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create("/" + SamlValidationWs.SAML_VALIDATION_CONTROLLER + "/" + VALIDATION_INIT_KEY);
  }

  @Override
  public void define(WebService.NewController controller) {
    controller
      .createAction(VALIDATION_INIT_KEY)
      .setInternal(true)
      .setPost(false)
      .setHandler(ServletFilterHandler.INSTANCE)
      .setDescription("Initiate a SAML request to the identity Provider for configuration validation purpose.")
      .setSince("9.7");
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    try {
      userSession.checkIsSystemAdministrator();
    } catch (ForbiddenException e) {
      AuthenticationError.handleError(request, response, "User needs to be logged in as system administrator to access this page.");
      return;
    }

    String csrfState = oAuthCsrfVerifier.generateState(request,response);

    try {
      samlAuthenticator.initLogin(oAuth2ContextFactory.generateCallbackUrl(SamlIdentityProvider.KEY),
        VALIDATION_RELAY_STATE + "/" + csrfState, request, response);
    } catch (IllegalStateException e) {
      response.sendRedirect("/" + SAML_VALIDATION_CONTROLLER_CONTEXT + "/" + SAML_VALIDATION_KEY);
    }
  }
}
