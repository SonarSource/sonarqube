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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RelyingPartyRegistrationRepositoryProviderTest {

  @Mock
  private SamlSettings samlSettings;

  @InjectMocks
  private RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider;

  @Test
  void provide_whenNullCallback_returnsRelyingPartyRegistrationRepository() {
    SonarqubeRelyingPartyRegistrationRepository relyingPartyRegistrationRepository = (SonarqubeRelyingPartyRegistrationRepository) relyingPartyRegistrationRepositoryProvider.provide(null);

    assertThat(relyingPartyRegistrationRepository).isNotNull();
    assertThat(relyingPartyRegistrationRepository.getSamlSettings()).isEqualTo(samlSettings);
    assertThat(relyingPartyRegistrationRepository.getCallbackUrl()).isNull();
  }

  @Test
  void provide_whenCallbackSet_returnsRelyingPartyRegistrationRepository() {
    SonarqubeRelyingPartyRegistrationRepository relyingPartyRegistrationRepository =
      (SonarqubeRelyingPartyRegistrationRepository) relyingPartyRegistrationRepositoryProvider.provide("callback");

    assertThat(relyingPartyRegistrationRepository).isNotNull();
    assertThat(relyingPartyRegistrationRepository.getSamlSettings()).isEqualTo(samlSettings);
    assertThat(relyingPartyRegistrationRepository.getCallbackUrl()).isEqualTo("callback");
  }

}
