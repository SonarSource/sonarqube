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
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.ObjectUtils;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.server.http.JakartaHttpRequest;
import org.springframework.security.saml2.core.Saml2ParameterNames;
import org.springframework.security.saml2.provider.service.authentication.Saml2RedirectAuthenticationRequest;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

public class RedirectToUrlProvider {

  private final RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider;

  RedirectToUrlProvider(RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider) {
    this.relyingPartyRegistrationRepositoryProvider = relyingPartyRegistrationRepositoryProvider;
  }

  String getRedirectToUrl(HttpRequest httpRequest, String callbackUrl, String relayState) {
    OpenSaml4AuthenticationRequestResolver requestResolver = getOpenSaml4AuthenticationRequestResolver(callbackUrl, relayState);

    HttpServletRequest httpServletRequest = ((JakartaHttpRequest) httpRequest).getDelegate();
    Saml2RedirectAuthenticationRequest authenticationRequest = requestResolver.resolve(httpServletRequest);

    return buildSamlLoginRequest(authenticationRequest).build().toUriString();
  }

  private OpenSaml4AuthenticationRequestResolver getOpenSaml4AuthenticationRequestResolver(String callbackUrl, String relayState) {
    SonarqubeRelyingPartyRegistrationResolver relyingPartyRegistrationResolver = new SonarqubeRelyingPartyRegistrationResolver(relyingPartyRegistrationRepositoryProvider,
      callbackUrl);
    OpenSaml4AuthenticationRequestResolver authRequestResolver = new OpenSaml4AuthenticationRequestResolver(relyingPartyRegistrationResolver);
    authRequestResolver.setRelayStateResolver(httpServletRequest -> relayState);
    authRequestResolver.setRequestMatcher(httpServletRequest -> true);
    return authRequestResolver;
  }

  private static UriComponentsBuilder buildSamlLoginRequest(Saml2RedirectAuthenticationRequest authenticationRequest) {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(authenticationRequest.getAuthenticationRequestUri());
    addParameter(Saml2ParameterNames.SAML_REQUEST, authenticationRequest.getSamlRequest(), uriBuilder);
    addParameter(Saml2ParameterNames.RELAY_STATE, authenticationRequest.getRelayState(), uriBuilder);
    addParameter(Saml2ParameterNames.SIG_ALG, authenticationRequest.getSigAlg(), uriBuilder);
    addParameter(Saml2ParameterNames.SIGNATURE, authenticationRequest.getSignature(), uriBuilder);
    return uriBuilder;
  }

  private static void addParameter(String name, String value, UriComponentsBuilder builder) {
    ObjectUtils.requireNonEmpty(name, "name cannot be null");
    if (StringUtils.hasText(value)) {
      builder.queryParam(UriUtils.encode(name, StandardCharsets.ISO_8859_1),
        UriUtils.encode(value, StandardCharsets.ISO_8859_1));
    }
  }

}
