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

import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SamlConfigurationTest {

  @Test
  void openSaml5AuthenticationProvider_doesNotFail() {
    SamlConfiguration samlConfiguration = new SamlConfiguration();
    SonarqubeSaml2ResponseValidator sonarqubeSaml2ResponseValidator = mock();

    OpenSaml5AuthenticationProvider openSaml5AuthenticationProvider = samlConfiguration.openSaml5AuthenticationProvider(sonarqubeSaml2ResponseValidator);

    assertThat(openSaml5AuthenticationProvider).isNotNull();
  }
}
