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
package org.sonar.api.server.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @since 5.4
 */
public interface BaseIdentityProvider extends IdentityProvider {

  /**
   * Entry-point of authentication workflow. Executed by core when user
   * clicks on the related button in login form (GET /sessions/init/{provider key}).
   */
  void init(Context context);

  interface Context {

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

    /**
     * Return the server base URL
     * @see org.sonar.api.platform.Server#getPublicRootUrl()
     */
    String getServerBaseURL();

    /**
     * Authenticate and register the user into the platform.
     *
     * The first time a user is authenticated (and if {@link #allowsUsersToSignUp()} is true), a new user will be registered.
     * Then, only user's name and email are updated.
     *
     * If @link #allowsUsersToSignUp()} is set to false and a new user try to authenticate,
     * then the user is not authenticated and he's redirected to a dedicated page.
     *
     * If the email of the user is already used by an existing user of the platform,
     * then the user is not authenticated and he's redirected to a dedicated page.
     */
    void authenticate(UserIdentity userIdentity);

  }
}
