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

import java.util.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.server.user.UserSession;

/**
 * Authenticates a request made by another <em>service</em> rather than by a person or a scanner: a
 * caller that proves its identity with something carried on the request itself and has no user
 * account behind it.
 *
 * <p>Extension point for core extensions. Implementations are consulted before the user-facing
 * mechanisms (SSO, JWT, user token, basic), since a service caller presents none of those. When none
 * is registered, {@link RequestAuthenticatorImpl} behaves as it did before this interface existed.
 *
 * <p>Contract:
 *
 * <ul>
 *   <li><b>Return empty only when the credential is absent</b> — it means "not mine, carry on with
 *       normal authentication".
 *   <li><b>Throw when the credential is present and does not verify.</b> Raise
 *       {@link org.sonar.server.authentication.event.AuthenticationException}, as
 *       {@link GithubWebhookAuthentication} does. Falling through would be a fail-open: an endpoint
 *       allowing anonymous access would serve a request whose credential was rejected.
 *   <li><b>Do not read the request body.</b> It is not buffered before this point, so draining it
 *       starves the handler behind it. Credentials must therefore cover only the method, the address
 *       and the headers.
 *   <li><b>The returned session must be logged in</b>, which is what satisfies
 *       {@code sonar.forceAuthentication}.
 *   <li><b>Authorize inside the implementation.</b> Returning a session grants it on <em>every</em>
 *       endpoint, so a credential meant for part of the API must enforce that itself.
 * </ul>
 *
 * @see GithubWebhookAuthentication for the equivalent mechanism built into the server, which
 *      predates this interface and is still wired directly.
 */
@ServerSide
public interface ServiceAuthentication {

  /**
   * @return the session to run the request as, or empty if this request carries no credential of
   *         this kind.
   * @throws org.sonar.server.authentication.event.AuthenticationException if the request carries such
   *         a credential and it is not acceptable.
   */
  Optional<UserSession> authenticate(HttpRequest request);
}
