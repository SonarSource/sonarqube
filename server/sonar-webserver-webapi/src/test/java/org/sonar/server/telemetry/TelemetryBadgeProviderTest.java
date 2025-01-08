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
package org.sonar.server.telemetry;

import org.junit.jupiter.api.Test;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryBadgeProviderTest {

  @Test
  void testGetters() {
    TelemetryBadgeProvider underTest = new TelemetryBadgeProvider();

    underTest.incrementForMetric("bugs");
    underTest.incrementForMetric("code_smells");
    underTest.incrementForMetric("code_smells");
    underTest.incrementForMetric("code_smells");

    assertThat(underTest.getMetricKey()).isEqualTo("project_badges_count");
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    assertThat(underTest.getValue()).isEmpty();
    assertThat(underTest.getValues()).hasSize(2);
    assertThat(underTest.getValues()).containsEntry("bugs", 1);
    assertThat(underTest.getValues()).containsEntry("code_smells", 3);

    underTest.after();

    assertTrue(underTest.getValues().isEmpty());
  }

}
