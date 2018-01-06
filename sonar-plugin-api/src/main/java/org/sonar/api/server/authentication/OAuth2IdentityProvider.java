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
package org.sonar.api.server.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @since 5.4
 */
public interface OAuth2IdentityProvider extends IdentityProvider {

  /**
   * Entry-point of authentication workflow. Executed by core when user
   * clicks on the related button in login form (GET /sessions/init/{provider key}).
   */
  void init(InitContext context);

  /**
   * This method is called when the identity provider has authenticated a user.
   */
  void callback(CallbackContext context);

  interface OAuth2Context {

    /**
     * The callback URL that must be used by the identity provider
     */
    String getCallbackUrl();

    /**
     * Get the received HTTP request.
     * Note - {@code getRequest().getSession()} must not be used in order to support
     * future clustering of web servers without stateful server sessions.
     */
    HttpServletRequest getRequest();

    /**
     * Get the HTTP response to send
     */
    HttpServletResponse getResponse();
  }

  interface InitContext extends OAuth2Context {

    /**
     * Generate a non-guessable state to prevent Cross Site Request Forgery.
     */
    String generateCsrfState();

    /**
     * Redirect the request to the url.
     * Can be used to redirect to the identity provider authentication url.
     */
    void redirectTo(String url);
  }

  interface CallbackContext extends OAuth2Context {

    /**
     * Check that the state is valid.
     * It should only be called If {@link InitContext#generateCsrfState()} was used in the init
     */
    void verifyCsrfState();

    /**
     * Redirect the request to the requested page.
     * Must be called at the end of {@link OAuth2IdentityProvider#callback(CallbackContext)}
     */
    void redirectToRequestedPage();

    /**
     * Authenticate and register the user into the platform.
     * @see org.sonar.api.server.authentication.BaseIdentityProvider.Context#authenticate(UserIdentity)
     */
    void authenticate(UserIdentity userIdentity);
  }

}
