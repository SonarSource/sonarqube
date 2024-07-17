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
package org.sonar.server.telemetry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.telemetry.core.Dimension.INSTALLATION;
import static org.sonar.telemetry.core.Granularity.ADHOC;
import static org.sonar.telemetry.core.TelemetryDataType.INTEGER;

class TelemetryDbMigrationTotalTimeProviderTest {

  @Test
  void testGetters() {
    TelemetryDbMigrationTotalTimeProvider underTest = new TelemetryDbMigrationTotalTimeProvider();
    underTest.setDbMigrationTotalTime(100L);

    assertThat(underTest.getMetricKey()).isEqualTo("db_migration_total_time_ms");
    assertThat(underTest.getDimension()).isEqualTo(INSTALLATION);
    assertThat(underTest.getType()).isEqualTo(INTEGER);
    assertThat(underTest.getGranularity()).isEqualTo(ADHOC);
    assertThat(underTest.getValue()).contains(100L);
  }

}
