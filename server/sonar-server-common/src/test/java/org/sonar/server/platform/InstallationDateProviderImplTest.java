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
package org.sonar.server.platform;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.server.property.InternalProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstallationDateProviderImplTest {

  private final InternalProperties internalProperties = mock(InternalProperties.class);
  private final InstallationDateProviderImpl underTest = new InstallationDateProviderImpl(internalProperties);

  @Test
  void getInstallationDate_returns_date_when_property_is_set() {
    when(internalProperties.read(InternalProperties.INSTALLATION_DATE)).thenReturn(Optional.of("1234567890"));

    assertThat(underTest.getInstallationDate()).contains(1234567890L);
  }

  @Test
  void getInstallationDate_returns_empty_when_property_is_absent() {
    when(internalProperties.read(InternalProperties.INSTALLATION_DATE)).thenReturn(Optional.empty());

    assertThat(underTest.getInstallationDate()).isEmpty();
  }
}
