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

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.db.user.UserDto;

public class AuthenticatorsImpl implements Authenticators {

  private final JwtHttpHandler jwtHttpHandler;
  private final BasicAuthenticator basicAuthenticator;
  private final SsoAuthenticator ssoAuthenticator;

  public AuthenticatorsImpl(JwtHttpHandler jwtHttpHandler, BasicAuthenticator basicAuthenticator, SsoAuthenticator ssoAuthenticator) {
    this.jwtHttpHandler = jwtHttpHandler;
    this.basicAuthenticator = basicAuthenticator;
    this.ssoAuthenticator = ssoAuthenticator;
  }

  // Try first to authenticate from SSO, then JWT token, then try from basic http header
  @Override
  public Optional<UserDto> authenticate(HttpServletRequest request, HttpServletResponse response) {
    // SSO authentication should come first in order to update JWT if user from header is not the same is user from JWT
    Optional<UserDto> user = ssoAuthenticator.authenticate(request, response);
    if (user.isPresent()) {
      return user;
    }
    user = jwtHttpHandler.validateToken(request, response);
    if (user.isPresent()) {
      return user;
    }
    return basicAuthenticator.authenticate(request);
  }
}
