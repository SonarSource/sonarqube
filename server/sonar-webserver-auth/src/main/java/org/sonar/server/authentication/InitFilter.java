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
package org.sonar.server.authentication;

import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.UrlPattern;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleAuthenticationError;
import static org.sonar.server.authentication.AuthenticationError.handleError;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class InitFilter extends AuthenticationFilter {

  private static final String INIT_CONTEXT = "/sessions/init/";

  private final BaseContextFactory baseContextFactory;
  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final AuthenticationEvent authenticationEvent;
  private final OAuth2AuthenticationParameters oAuthOAuth2AuthenticationParameters;

  public InitFilter(IdentityProviderRepository identityProviderRepository, BaseContextFactory baseContextFactory,
    OAuth2ContextFactory oAuth2ContextFactory, AuthenticationEvent authenticationEvent, OAuth2AuthenticationParameters oAuthOAuth2AuthenticationParameters) {
    super(identityProviderRepository);
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
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
    IdentityProvider provider = resolveProviderOrHandleResponse(request, response, INIT_CONTEXT);
    if (provider != null) {
      handleProvider(request, response, provider);
    }
  }

  private void handleProvider(HttpRequest request, HttpResponse response, IdentityProvider provider) {
    try {
      if (provider instanceof BaseIdentityProvider baseIdentityProvider) {
        handleBaseIdentityProvider(request, response, baseIdentityProvider);
      } else if (provider instanceof OAuth2IdentityProvider oAuth2IdentityProvider) {
        oAuthOAuth2AuthenticationParameters.init(request, response);
        handleOAuth2IdentityProvider(request, response, oAuth2IdentityProvider);
      } else {
        handleError(request, response, format("Unsupported IdentityProvider class: %s", provider.getClass()));
      }
    } catch (AuthenticationException e) {
      oAuthOAuth2AuthenticationParameters.delete(request, response);
      authenticationEvent.loginFailure(request, e);
      handleAuthenticationError(e, request, response);
    } catch (Exception e) {
      oAuthOAuth2AuthenticationParameters.delete(request, response);
      handleError(e, request, response, format("Fail to initialize authentication with provider '%s'", provider.getKey()));
    }
  }

  private void handleBaseIdentityProvider(HttpRequest request, HttpResponse response, BaseIdentityProvider provider) {
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

  private void handleOAuth2IdentityProvider(HttpRequest request, HttpResponse response, OAuth2IdentityProvider provider) {
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
  public void init() {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
