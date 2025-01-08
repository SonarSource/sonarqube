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
package org.sonar.server.platform.db.migration;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.telemetry.core.TelemetryClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatabaseMigrationTelemetryTest {

  private final UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  private final Server server = mock(Server.class);
  private final TelemetryClient telemetryClient = mock();
  private final Configuration configuration = mock();
  DatabaseMigrationLoggerContext databaseMigrationLoggerContext = new DatabaseMigrationLoggerContext();

  DatabaseMigrationTelemetry underTest = new DatabaseMigrationTelemetry(server, uuidFactory, databaseMigrationLoggerContext, telemetryClient, configuration);

  {
    when(server.getId()).thenReturn("serverId");
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
  }

  @Test
  void start() {
    databaseMigrationLoggerContext.addMigrationData("001", 45L, true, 100L, "2025.1");
    databaseMigrationLoggerContext.addMigrationData("002", 46L, false, 101L, "2025.1");
    databaseMigrationLoggerContext.addMigrationData("003", 47L, true, 102L, "2025.1");

    underTest.start();

    verify(telemetryClient, times(1)).uploadMetricAsync(anyString());
  }

  @Test
  void start_whenMetricsNotPresent_dontCallTelemetryClient() {
    // No database migration data found in the logger context

    underTest.start();

    verifyNoInteractions(telemetryClient);
  }

  @Test
  void start_whenMetricsPresentAndTelemetryNotEnabled_dontCallTelemetryClient() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));

    underTest.start();

    verifyNoInteractions(telemetryClient);
  }

}
