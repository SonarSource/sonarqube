/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.authentication;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.authentication.exception.RedirectionException;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleAuthenticationError;
import static org.sonar.server.authentication.AuthenticationError.handleError;
import static org.sonar.server.authentication.AuthenticationRedirection.redirectTo;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class InitFilter extends AuthenticationFilter {

  private static final String INIT_CONTEXT = "/sessions/init/";

  private final BaseContextFactory baseContextFactory;
  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final AuthenticationEvent authenticationEvent;
  private final OAuth2AuthenticationParameters oAuthOAuth2AuthenticationParameters;

  public InitFilter(IdentityProviderRepository identityProviderRepository, BaseContextFactory baseContextFactory,
    OAuth2ContextFactory oAuth2ContextFactory, Server server, AuthenticationEvent authenticationEvent, OAuth2AuthenticationParameters oAuthOAuth2AuthenticationParameters) {
    super(server, identityProviderRepository);
    this.baseContextFactory = baseContextFactory;
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.authenticationEvent = authenticationEvent;
    this.oAuthOAuth2AuthenticationParameters = oAuthOAuth2AuthenticationParameters;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(INIT_CONTEXT + "*");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    IdentityProvider provider = resolveProviderOrHandleResponse(httpRequest, httpResponse, INIT_CONTEXT);
    if (provider != null) {
      handleProvider(httpRequest, httpResponse, provider);
    }
  }

  private void handleProvider(HttpServletRequest request, HttpServletResponse response, IdentityProvider provider) {
    try {
      if (provider instanceof BaseIdentityProvider) {
        handleBaseIdentityProvider(request, response, (BaseIdentityProvider) provider);
      } else if (provider instanceof OAuth2IdentityProvider) {
        oAuthOAuth2AuthenticationParameters.init(request, response);
        handleOAuth2IdentityProvider(request, response, (OAuth2IdentityProvider) provider);
      } else {
        handleError(response, format("Unsupported IdentityProvider class: %s", provider.getClass()));
      }
    } catch (AuthenticationException e) {
      oAuthOAuth2AuthenticationParameters.delete(request, response);
      authenticationEvent.loginFailure(request, e);
      handleAuthenticationError(e, response, getContextPath());
    } catch (RedirectionException e) {
      oAuthOAuth2AuthenticationParameters.delete(request, response);
      redirectTo(response, e.getPath(getContextPath()));
    } catch (Exception e) {
      oAuthOAuth2AuthenticationParameters.delete(request, response);
      handleError(e, response, format("Fail to initialize authentication with provider '%s'", provider.getKey()));
    }
  }

  private void handleBaseIdentityProvider(HttpServletRequest request, HttpServletResponse response, BaseIdentityProvider provider) {
    try {
      provider.init(baseContextFactory.newContext(request, response, provider));
    } catch (UnauthorizedException e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.external(provider))
        .setMessage(e.getMessage())
        .setPublicMessage(e.getMessage())
        .build();
    }
  }

  private void handleOAuth2IdentityProvider(HttpServletRequest request, HttpServletResponse response, OAuth2IdentityProvider provider) {
    try {
      provider.init(oAuth2ContextFactory.newContext(request, response, provider));
    } catch (UnauthorizedException e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.oauth2(provider))
        .setMessage(e.getMessage())
        .setPublicMessage(e.getMessage())
        .build();
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
