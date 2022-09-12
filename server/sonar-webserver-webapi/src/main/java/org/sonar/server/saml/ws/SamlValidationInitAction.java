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
import org.sonar.server.authentication.OAuth2ContextFactory;
import org.sonar.server.ws.ServletFilterHandler;

public class SamlValidationInitAction extends ServletFilter implements SamlAction {

  public static final String VALIDATION_RELAY_STATE = "validation-query";
  private final SamlAuthenticator samlAuthenticator;
  private final OAuth2ContextFactory oAuth2ContextFactory;

  public SamlValidationInitAction(SamlAuthenticator samlAuthenticator, OAuth2ContextFactory oAuth2ContextFactory) {
    this.samlAuthenticator = samlAuthenticator;
    this.oAuth2ContextFactory = oAuth2ContextFactory;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create("/api/saml/validation_init");
  }

  @Override
  public void define(WebService.NewController controller) {
    controller
      .createAction("validation_init")
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

    samlAuthenticator.initLogin(oAuth2ContextFactory.generateCallbackUrl(SamlIdentityProvider.KEY),
      VALIDATION_RELAY_STATE, request, response);
  }
}
