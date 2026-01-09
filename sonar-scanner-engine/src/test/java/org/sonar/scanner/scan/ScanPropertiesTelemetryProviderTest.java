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
package org.sonar.scanner.scan;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.scanner.repository.TelemetryCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScanPropertiesTelemetryProviderTest {

  static Stream<Arguments> telemetryDataScenarios() {
    return Stream.of(
      // JRE provisioning disabled scenarios
      arguments("No properties set", null, null, null, null, null, "false", "unknown", "unknown"),
      arguments("JreAutoProvisioning disabled", "true", null, null, null, null, "true", "unknown", "unknown"),
      arguments("Skip JRE provisioning", null, "true", null, null, null, "true", "unknown", "unknown"),
      arguments("Java exe path present", null, null, "/path/to/java", null, null, "true", "unknown", "unknown"),
      arguments("Properties explicitly false", "false", "false", null, null, null, "false", "unknown", "unknown"),
      // Scanner app scenarios
      arguments("Scanner app provided", null, null, null, "ScannerCLI", null, "false", "ScannerCLI", "unknown"),
      arguments("Scanner app version provided", null, null, null, null, "1.2.3", "false", "unknown", "1.2.3"),
      arguments("All properties provided", "true", null, null, "Maven", "3.9.0", "true", "Maven", "3.9.0")
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("telemetryDataScenarios")
  void start_shouldStoreTelemetryData(
    String testName,
    String jreAutoProvisioningDisabled,
    String skipJreProvisioning,
    String javaExePath,
    String scannerApp,
    String scannerAppVersion,
    String expectedJreProvisioningDisabled,
    String expectedScannerApp,
    String expectedScannerAppVersion) {
    ScanProperties scanProperties = mock(ScanProperties.class);

    TelemetryCache telemetryCache = new TelemetryCache();
    ScanPropertiesTelemetryProvider underTest = new ScanPropertiesTelemetryProvider(scanProperties, telemetryCache);

    when(scanProperties.get("sonar.jreAutoProvisioning.disabled")).thenReturn(Optional.ofNullable(jreAutoProvisioningDisabled));
    when(scanProperties.get("sonar.scanner.skipJreProvisioning")).thenReturn(Optional.ofNullable(skipJreProvisioning));
    when(scanProperties.get("sonar.scanner.javaExePath")).thenReturn(Optional.ofNullable(javaExePath));
    when(scanProperties.get("sonar.scanner.app")).thenReturn(Optional.ofNullable(scannerApp));
    when(scanProperties.get("sonar.scanner.appVersion")).thenReturn(Optional.ofNullable(scannerAppVersion));

    underTest.start();

    assertThat(telemetryCache.getAll())
      .containsEntry("jre_provisioning_disabled", expectedJreProvisioningDisabled)
      .containsEntry("sonar.scanner.app", expectedScannerApp)
      .containsEntry("sonar.scanner.appVersion", expectedScannerAppVersion)
      .hasSize(3);
  }
}
