/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.auth.saml;

import jakarta.servlet.http.HttpServletRequest;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.server.http.JakartaHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;

@ServerSide
class SamlResponseAuthenticator {

  private final OpenSaml5AuthenticationProvider openSaml5AuthenticationProvider;
  private final RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider;

  SamlResponseAuthenticator(OpenSaml5AuthenticationProvider openSaml5AuthenticationProvider,
    RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider) {
    this.openSaml5AuthenticationProvider = openSaml5AuthenticationProvider;
    this.relyingPartyRegistrationRepositoryProvider = relyingPartyRegistrationRepositoryProvider;
  }

  Saml2AuthenticatedPrincipal authenticate(HttpRequest request, String callbackUrl) {
    Authentication authenticationToken = processSamlResponse(request, callbackUrl);
    authenticationToken = openSaml5AuthenticationProvider.authenticate(authenticationToken);
    return (Saml2AuthenticatedPrincipal) authenticationToken.getPrincipal();
  }

  private Saml2AuthenticationToken processSamlResponse(HttpRequest processedRequest, String callbackUrl) {
    SonarqubeRelyingPartyRegistrationResolver registrationResolver = new SonarqubeRelyingPartyRegistrationResolver(relyingPartyRegistrationRepositoryProvider, callbackUrl);
    HttpServletRequest httpServletRequest = ((JakartaHttpRequest) processedRequest).getDelegate();
    Saml2AuthenticationTokenConverter converter = new Saml2AuthenticationTokenConverter(registrationResolver);
    return converter.convert(httpServletRequest);
  }

}
