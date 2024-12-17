/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import javax.annotation.Nullable;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;

public class SonarqubeRelyingPartyRegistrationResolver implements RelyingPartyRegistrationResolver {

  private final RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider;
  @Nullable
  private final String callbackUrl;

  public SonarqubeRelyingPartyRegistrationResolver(RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider,
    @Nullable String callbackUrl) {
    this.relyingPartyRegistrationRepositoryProvider = relyingPartyRegistrationRepositoryProvider;
    this.callbackUrl = callbackUrl;
  }

  @Override
  public RelyingPartyRegistration resolve(HttpServletRequest request, String relyingPartyRegistrationId) {
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository = relyingPartyRegistrationRepositoryProvider.provide(callbackUrl);
    DefaultRelyingPartyRegistrationResolver defaultRelyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);
    return defaultRelyingPartyRegistrationResolver.resolve(request, "saml");
  }
}
