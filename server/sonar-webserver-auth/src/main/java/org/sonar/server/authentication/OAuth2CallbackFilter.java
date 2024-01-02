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
package org.sonar.server.authentication;

import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.UrlPattern;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ThreadLocalUserSession;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleAuthenticationError;
import static org.sonar.server.authentication.AuthenticationError.handleError;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class OAuth2CallbackFilter extends AuthenticationFilter {

  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final AuthenticationEvent authenticationEvent;
  private final OAuth2AuthenticationParameters oauth2Parameters;
  private final ThreadLocalUserSession threadLocalUserSession;

  public OAuth2CallbackFilter(IdentityProviderRepository identityProviderRepository, OAuth2ContextFactory oAuth2ContextFactory,
    AuthenticationEvent authenticationEvent, OAuth2AuthenticationParameters oauth2Parameters, ThreadLocalUserSession threadLocalUserSession) {
    super(identityProviderRepository);
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.authenticationEvent = authenticationEvent;
    this.oauth2Parameters = oauth2Parameters;
    this.threadLocalUserSession = threadLocalUserSession;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(CALLBACK_PATH + "*");
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
    IdentityProvider provider = resolveProviderOrHandleResponse(request, response, CALLBACK_PATH);
    if (provider != null) {
      handleProvider(request, response, provider);
    }
  }

  private void handleProvider(HttpRequest request, HttpResponse response, IdentityProvider provider) {
    try {
      if (provider instanceof OAuth2IdentityProvider oAuth2IdentityProvider) {
        handleOAuth2Provider(request, response, oAuth2IdentityProvider);
      } else {
        handleError(request, response, format("Not an OAuth2IdentityProvider: %s", provider.getClass()));
      }
    } catch (AuthenticationException e) {
      oauth2Parameters.delete(request, response);
      authenticationEvent.loginFailure(request, e);
      handleAuthenticationError(e, request, response);
    } catch (Exception e) {
      oauth2Parameters.delete(request, response);
      handleError(e, request, response, format("Fail to callback authentication with '%s'", provider.getKey()));
    }
  }

  private void handleOAuth2Provider(HttpRequest request, HttpResponse response, OAuth2IdentityProvider oAuth2Provider) {
    OAuth2IdentityProvider.CallbackContext context = oAuth2ContextFactory.newCallback(request, response, oAuth2Provider);
    try {
      oAuth2Provider.callback(context);
    } catch (UnauthorizedException e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.oauth2(oAuth2Provider))
        .setMessage(e.getMessage())
        .setPublicMessage(e.getMessage())
        .build();
    }
    if (threadLocalUserSession.hasSession()) {
      authenticationEvent.loginSuccess(request, threadLocalUserSession.getLogin(), Source.oauth2(oAuth2Provider));
    } else {
      throw AuthenticationException.newBuilder()
        .setSource(Source.oauth2(oAuth2Provider))
        .setMessage("Plugin did not call authenticate")
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
