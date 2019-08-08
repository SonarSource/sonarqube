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

import org.sonar.core.platform.Module;
import org.sonar.server.authentication.event.AuthenticationEventImpl;

public class AuthenticationModule extends Module {
  @Override
  protected void configureModule() {
    add(
      AuthenticationEventImpl.class,
      InitFilter.class,
      OAuth2CallbackFilter.class,
      IdentityProviderRepository.class,
      BaseContextFactory.class,
      OAuth2ContextFactory.class,
      UserRegistrarImpl.class,
      OAuthCsrfVerifier.class,
      UserSessionInitializer.class,
      JwtSerializer.class,
      JwtHttpHandler.class,
      JwtCsrfVerifier.class,
      OAuth2AuthenticationParametersImpl.class,
      CredentialsAuthentication.class,
      CredentialsLocalAuthentication.class,
      CredentialsExternalAuthentication.class,
      BasicAuthentication.class,
      HttpHeadersAuthentication.class,
      RequestAuthenticatorImpl.class,
      UserLastConnectionDatesUpdaterImpl.class);
  }
}
