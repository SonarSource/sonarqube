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
package org.sonar.server.telemetry.gessie;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.platform.Server;
import org.sonarsource.gessie.server.InstallationDateProvider;
import org.sonarsource.gessie.server.LicenseInfoProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GessieMetadataSourceImplTest {

  private final Server server = mock(Server.class);
  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final LicenseInfoProvider licenseInfoProvider = mock(LicenseInfoProvider.class);
  private final InstallationDateProvider installationDateProvider = mock(InstallationDateProvider.class);

  private final GessieMetadataSourceImpl underTest = new GessieMetadataSourceImpl(server, sonarRuntime, licenseInfoProvider, installationDateProvider);

  @Test
  void getId_delegates_to_server() {
    when(server.getId()).thenReturn("installation-id");

    assertThat(underTest.getId()).isEqualTo("installation-id");
  }

  @Test
  void getVersion_delegates_to_server() {
    when(server.getVersion()).thenReturn("2026.1");

    assertThat(underTest.getVersion()).isEqualTo("2026.1");
  }

  @Test
  void getEdition_delegates_to_sonarRuntime() {
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.ENTERPRISE);

    assertThat(underTest.getEdition()).isEqualTo("ENTERPRISE");
  }

  @Test
  void getInstallationDate_delegates_to_installationDateProvider() {
    when(installationDateProvider.getInstallationDate()).thenReturn(Optional.of(1234567890L));

    assertThat(underTest.getInstallationDate()).contains(1234567890L);
  }

  @Test
  void getLicenseType_delegates_to_licenseInfoProvider_when_present() {
    when(licenseInfoProvider.getLicenseType()).thenReturn(Optional.of("ENTERPRISE"));

    assertThat(underTest.getLicenseType()).contains("ENTERPRISE");
  }

  @Test
  void getLicenseType_returns_empty_when_licenseInfoProvider_is_null() {
    GessieMetadataSourceImpl withoutLicense = new GessieMetadataSourceImpl(server, sonarRuntime, null, installationDateProvider);

    assertThat(withoutLicense.getLicenseType()).isEmpty();
  }
}
