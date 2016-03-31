/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.authentication;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.web.ServletFilter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleError;
import static org.sonar.server.authentication.AuthenticationError.handleUnauthorizedError;

public class OAuth2CallbackFilter extends ServletFilter {

  public static final String CALLBACK_PATH = "/oauth2/callback/";

  private final IdentityProviderRepository identityProviderRepository;
  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final Server server;

  public OAuth2CallbackFilter(IdentityProviderRepository identityProviderRepository, OAuth2ContextFactory oAuth2ContextFactory, Server server) {
    this.identityProviderRepository = identityProviderRepository;
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.server = server;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(CALLBACK_PATH + "*");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String requestUri = httpRequest.getRequestURI();
    String keyProvider = "";
    try {
      keyProvider = extractKeyProvider(requestUri, server.getContextPath() + CALLBACK_PATH);
      IdentityProvider provider = identityProviderRepository.getEnabledByKey(keyProvider);
      if (provider instanceof OAuth2IdentityProvider) {
        OAuth2IdentityProvider oauthProvider = (OAuth2IdentityProvider) provider;
        oauthProvider.callback(oAuth2ContextFactory.newCallback(httpRequest, (HttpServletResponse) response, oauthProvider));
      } else {
        handleError((HttpServletResponse) response, format("Not an OAuth2IdentityProvider: %s", provider.getClass()));
      }
    } catch (UnauthorizedException e) {
      handleUnauthorizedError(e, (HttpServletResponse) response);
    } catch (Exception e) {
      handleError(e, (HttpServletResponse) response,
        keyProvider.isEmpty() ? "Fail to callback authentication" :
          format("Fail to callback authentication with '%s'", keyProvider));
    }
  }

  public static String extractKeyProvider(String requestUri, String context) {
    if (requestUri.contains(context)) {
      String key = requestUri.replace(context, "");
      if (!isNullOrEmpty(key)) {
        return key;
      }
    }
    throw new IllegalArgumentException(String.format("A valid identity provider key is required. Please check that property '%s' is valid.", CoreProperties.SERVER_BASE_URL));
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
