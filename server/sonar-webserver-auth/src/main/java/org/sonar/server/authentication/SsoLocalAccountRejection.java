/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import org.sonar.server.authentication.event.AuthenticationException;

/**
 * Thrown by {@link UserRegistrarImpl} when an HTTP header (SSO) authentication attempt resolves to a pre-existing
 * local account. Unlike other {@link AuthenticationException} causes, this must not abort the whole authentication
 * chain: the caller ({@link HttpHeadersAuthentication}) treats it as "SSO declines to authenticate this request"
 * and lets JWT/basic authentication proceed instead, so a local account's password session is never disrupted by
 * a header claiming its login.
 */
class SsoLocalAccountRejection extends RuntimeException {

  private final AuthenticationException authenticationException;

  SsoLocalAccountRejection(AuthenticationException authenticationException) {
    super(authenticationException.getMessage());
    this.authenticationException = authenticationException;
  }

  AuthenticationException toAuthenticationException() {
    return authenticationException;
  }

}
