/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static org.sonar.server.authentication.AuthenticationError.handleAuthenticationError;
import static org.sonar.server.authentication.AuthenticationError.handleError;
import static org.sonar.server.authentication.AuthenticationRedirection.redirectTo;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class OAuth2CallbackFilter extends AuthenticationFilter {

  private final OAuth2ContextFactory oAuth2ContextFactory;
  private final AuthenticationEvent authenticationEvent;
  private final OAuth2AuthenticationParameters oauth2Parameters;

  public OAuth2CallbackFilter(IdentityProviderRepository identityProviderRepository, OAuth2ContextFactory oAuth2ContextFactory,
    Server server, AuthenticationEvent authenticationEvent, OAuth2AuthenticationParameters oauth2Parameters) {
    super(server, identityProviderRepository);
    this.oAuth2ContextFactory = oAuth2ContextFactory;
    this.authenticationEvent = authenticationEvent;
    this.oauth2Parameters = oauth2Parameters;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(CALLBACK_PATH + "*");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    IdentityProvider provider = resolveProviderOrHandleResponse(httpRequest, httpResponse, CALLBACK_PATH);
    if (provider != null) {
      handleProvider(httpRequest, (HttpServletResponse) response, provider);
    }
  }

  private void handleProvider(HttpServletRequest request, HttpServletResponse response, IdentityProvider provider) {
    try {
      if (provider instanceof OAuth2IdentityProvider) {
        handleOAuth2Provider(response, request, (OAuth2IdentityProvider) provider);
      } else {
        handleError(response, format("Not an OAuth2IdentityProvider: %s", provider.getClass()));
      }
    } catch (AuthenticationException e) {
      oauth2Parameters.delete(request, response);
      authenticationEvent.loginFailure(request, e);
      handleAuthenticationError(e, response, getContextPath());
    } catch (EmailAlreadyExistsException e) {
      oauth2Parameters.delete(request, response);
      redirectTo(response, e.getPath(getContextPath()));
    } catch (Exception e) {
      oauth2Parameters.delete(request, response);
      handleError(e, response, format("Fail to callback authentication with '%s'", provider.getKey()));
    }
  }

  private void handleOAuth2Provider(HttpServletResponse response, HttpServletRequest httpRequest, OAuth2IdentityProvider oAuth2Provider) {
    OAuth2CallbackFilter.WrappedContext context = new OAuth2CallbackFilter.WrappedContext(oAuth2ContextFactory.newCallback(httpRequest, response, oAuth2Provider));
    try {
      oAuth2Provider.callback(context);
    } catch (UnauthorizedException e) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.oauth2(oAuth2Provider))
        .setMessage(e.getMessage())
        .setPublicMessage(e.getMessage())
        .build();
    }
    if (context.isAuthenticated()) {
      authenticationEvent.loginSuccess(httpRequest, context.getLogin(), Source.oauth2(oAuth2Provider));
    } else {
      throw AuthenticationException.newBuilder()
        .setSource(Source.oauth2(oAuth2Provider))
        .setMessage("Plugin did not call authenticate")
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

  private static final class WrappedContext implements OAuth2IdentityProvider.CallbackContext {
    private final OAuth2IdentityProvider.CallbackContext delegate;
    private boolean authenticated = false;
    @CheckForNull
    private String login;

    private WrappedContext(OAuth2IdentityProvider.CallbackContext delegate) {
      this.delegate = delegate;
    }

    @Override
    public String getCallbackUrl() {
      return delegate.getCallbackUrl();
    }

    @Override
    public HttpServletRequest getRequest() {
      return delegate.getRequest();
    }

    @Override
    public HttpServletResponse getResponse() {
      return delegate.getResponse();
    }

    @Override
    public void verifyCsrfState() {
      delegate.verifyCsrfState();
    }

    @Override
    public void redirectToRequestedPage() {
      delegate.redirectToRequestedPage();
    }

    @Override
    public void authenticate(UserIdentity userIdentity) {
      delegate.authenticate(userIdentity);
      this.authenticated = true;
      this.login = userIdentity.getLogin();
    }

    public boolean isAuthenticated() {
      return authenticated;
    }

    @CheckForNull
    public String getLogin() {
      return login;
    }
  }
}
