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
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.web.ServletFilter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleError;
import static org.sonar.server.authentication.AuthenticationError.handleUnauthorizedError;

public class InitFilter extends ServletFilter {

  private static final String INIT_CONTEXT = "/sessions/init/";

  private final IdentityProviderRepository identityProviderRepository;
  private final BaseContextFactory baseContextFactory;
  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final Server server;

  public InitFilter(IdentityProviderRepository identityProviderRepository, BaseContextFactory baseContextFactory, OAuth2ContextFactory oAuth2ContextFactory, Server server) {
    this.identityProviderRepository = identityProviderRepository;
    this.baseContextFactory = baseContextFactory;
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.server = server;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(INIT_CONTEXT + "*");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String requestURI = httpRequest.getRequestURI();
    String keyProvider = "";
    try {
      keyProvider = extractKeyProvider(requestURI, server.getContextPath() + INIT_CONTEXT);
      IdentityProvider provider = identityProviderRepository.getEnabledByKey(keyProvider);
      if (provider instanceof BaseIdentityProvider) {
        BaseIdentityProvider baseIdentityProvider = (BaseIdentityProvider) provider;
        baseIdentityProvider.init(baseContextFactory.newContext(httpRequest, (HttpServletResponse) response, baseIdentityProvider));
      } else if (provider instanceof OAuth2IdentityProvider) {
        OAuth2IdentityProvider oAuth2IdentityProvider = (OAuth2IdentityProvider) provider;
        oAuth2IdentityProvider.init(oAuth2ContextFactory.newContext(httpRequest, (HttpServletResponse) response, oAuth2IdentityProvider));
      } else {
        throw new UnsupportedOperationException(format("Unsupported IdentityProvider class: %s ", provider.getClass()));
      }
    } catch (UnauthorizedException e) {
      handleUnauthorizedError(e, (HttpServletResponse) response);
    } catch (Exception e) {
      handleError(e, (HttpServletResponse) response, format("Fail to initialize authentication with provider '%s'", keyProvider));
    }
  }

  public static String extractKeyProvider(String requestUri, String context) {
    if (requestUri.contains(context)) {
      String key = requestUri.replace(context, "");
      if (!isNullOrEmpty(key)) {
        return key;
      }
    }
    throw new IllegalArgumentException("A valid identity provider key is required.");
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
