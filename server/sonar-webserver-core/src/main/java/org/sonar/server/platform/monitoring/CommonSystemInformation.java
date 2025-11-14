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
package org.sonar.server.platform.monitoring;

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.SecurityRealmFactory;

import static java.util.Collections.emptyList;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE;

public class CommonSystemInformation {
  private final Configuration config;
  private final IdentityProviderRepository identityProviderRepository;
  private final ManagedInstanceService managedInstanceService;
  private final SecurityRealmFactory securityRealmFactory;

  public CommonSystemInformation(Configuration config, IdentityProviderRepository identityProviderRepository,
    ManagedInstanceService managedInstanceService, SecurityRealmFactory securityRealmFactory) {
    this.config = config;
    this.identityProviderRepository = identityProviderRepository;
    this.managedInstanceService = managedInstanceService;
    this.securityRealmFactory = securityRealmFactory;
  }

  public boolean getForceAuthentication() {
    return config.getBoolean(CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY).orElse(CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE);
  }

  public List<String> getEnabledIdentityProviders() {
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .filter(IdentityProvider::isEnabled)
      .map(IdentityProvider::getName)
      .toList();
  }

  public List<String> getAllowsToSignUpEnabledIdentityProviders() {
    if (managedInstanceService.isInstanceExternallyManaged()) {
      return emptyList();
    }
    return identityProviderRepository.getAllEnabledAndSorted()
      .stream()
      .filter(IdentityProvider::isEnabled)
      .filter(IdentityProvider::allowsUsersToSignUp)
      .map(IdentityProvider::getName)
      .toList();
  }

  public String getManagedInstanceProviderName() {
    if (managedInstanceService.isInstanceExternallyManaged()) {
      return managedInstanceService.getProviderName();
    }
    return null;
  }

  @CheckForNull
  public String getExternalUserAuthentication() {
    SecurityRealm realm = securityRealmFactory.getRealm();
    return realm == null ? null : realm.getName();
  }
}
