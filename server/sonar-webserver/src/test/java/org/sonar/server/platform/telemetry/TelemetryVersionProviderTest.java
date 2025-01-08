/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.platform.Server;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryVersionProviderTest {

  /**
   * To increase coverage :shrug:
   */
  @Test
  void testGetters() {
    Server server = mock();
    when(server.getVersion()).thenReturn("10.6");

    TelemetryVersionProvider telemetryVersionProvider = new TelemetryVersionProvider(server);

    assertEquals("version", telemetryVersionProvider.getMetricKey());
    assertEquals(Dimension.INSTALLATION, telemetryVersionProvider.getDimension());
    assertEquals(Granularity.DAILY, telemetryVersionProvider.getGranularity());
    assertEquals(TelemetryDataType.STRING, telemetryVersionProvider.getType());
    assertEquals(Optional.of("10.6"), telemetryVersionProvider.getValue());
  }
}
