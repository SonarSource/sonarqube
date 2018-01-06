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

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSessionFactory;

import static java.lang.String.format;
import static org.sonar.server.authentication.OAuth2CallbackFilter.CALLBACK_PATH;

@ServerSide
public class OAuth2ContextFactory {

  private final ThreadLocalUserSession threadLocalUserSession;
  private final UserIdentityAuthenticator userIdentityAuthenticator;
  private final Server server;
  private final OAuthCsrfVerifier csrfVerifier;
  private final JwtHttpHandler jwtHttpHandler;
  private final UserSessionFactory userSessionFactory;
  private final OAuth2Redirection oAuthRedirection;

  public OAuth2ContextFactory(ThreadLocalUserSession threadLocalUserSession, UserIdentityAuthenticator userIdentityAuthenticator, Server server,
    OAuthCsrfVerifier csrfVerifier, JwtHttpHandler jwtHttpHandler, UserSessionFactory userSessionFactory, OAuth2Redirection oAuthRedirection) {
    this.threadLocalUserSession = threadLocalUserSession;
    this.userIdentityAuthenticator = userIdentityAuthenticator;
    this.server = server;
    this.csrfVerifier = csrfVerifier;
    this.jwtHttpHandler = jwtHttpHandler;
    this.userSessionFactory = userSessionFactory;
    this.oAuthRedirection = oAuthRedirection;
  }

  public OAuth2IdentityProvider.InitContext newContext(HttpServletRequest request, HttpServletResponse response, OAuth2IdentityProvider identityProvider) {
    return new OAuthContextImpl(request, response, identityProvider);
  }

  public OAuth2IdentityProvider.CallbackContext newCallback(HttpServletRequest request, HttpServletResponse response, OAuth2IdentityProvider identityProvider) {
    return new OAuthContextImpl(request, response, identityProvider);
  }

  private class OAuthContextImpl implements OAuth2IdentityProvider.InitContext, OAuth2IdentityProvider.CallbackContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final OAuth2IdentityProvider identityProvider;

    public OAuthContextImpl(HttpServletRequest request, HttpServletResponse response, OAuth2IdentityProvider identityProvider) {
      this.request = request;
      this.response = response;
      this.identityProvider = identityProvider;
    }

    @Override
    public String getCallbackUrl() {
      return server.getPublicRootUrl() + CALLBACK_PATH + identityProvider.getKey();
    }

    @Override
    public String generateCsrfState() {
      return csrfVerifier.generateState(request, response);
    }

    @Override
    public HttpServletRequest getRequest() {
      return request;
    }

    @Override
    public HttpServletResponse getResponse() {
      return response;
    }

    @Override
    public void redirectTo(String url) {
      try {
        response.sendRedirect(url);
      } catch (IOException e) {
        throw new IllegalStateException(format("Fail to redirect to %s", url), e);
      }
    }

    @Override
    public void verifyCsrfState() {
      csrfVerifier.verifyState(request, response, identityProvider);
    }

    @Override
    public void redirectToRequestedPage() {
      try {
        Optional<String> redirectTo = oAuthRedirection.getAndDelete(request, response);
        getResponse().sendRedirect(redirectTo.orElse(server.getContextPath() + "/"));
      } catch (IOException e) {
        throw new IllegalStateException("Fail to redirect to home", e);
      }
    }

    @Override
    public void authenticate(UserIdentity userIdentity) {
      UserDto userDto = userIdentityAuthenticator.authenticate(userIdentity, identityProvider, AuthenticationEvent.Source.oauth2(identityProvider));
      jwtHttpHandler.generateToken(userDto, request, response);
      threadLocalUserSession.set(userSessionFactory.create(userDto));
    }
  }
}
