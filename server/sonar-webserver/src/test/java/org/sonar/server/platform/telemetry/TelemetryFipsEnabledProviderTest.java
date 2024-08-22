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
package org.sonar.server.platform.telemetry;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.core.fips.FipsDetector;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TelemetryFipsEnabledProviderTest {

  /**
   * This test should only run when FIPS is enabled, i.e. when the FIPS provider is present in the list of providers.
   */
  @Test
  void testGetters_whenFipsEnabled() {
    assumeTrue(FipsDetector.isFipsEnabled());

    assertValues(true);
  }

  /**
   * This test should only run when FIPS is disabled, i.e. when the FIPS provider is not present in the list of providers.
   */
  @Test
  void testGetters_whenFipsDisabled() {
    assumeFalse(FipsDetector.isFipsEnabled());

    assertValues(false);
  }

  private void assertValues(boolean expectedFipsEnabled) {
    TelemetryFipsEnabledProvider telemetryVersionProvider = new TelemetryFipsEnabledProvider();
    assertEquals("is_fips_enabled", telemetryVersionProvider.getMetricKey());
    assertEquals(Dimension.INSTALLATION, telemetryVersionProvider.getDimension());
    assertEquals(Granularity.DAILY, telemetryVersionProvider.getGranularity());
    assertEquals(TelemetryDataType.BOOLEAN, telemetryVersionProvider.getType());
    assertEquals(Optional.of(expectedFipsEnabled), telemetryVersionProvider.getValue());
    assertThrows(IllegalStateException.class, telemetryVersionProvider::getUuidValues);
  }

}
