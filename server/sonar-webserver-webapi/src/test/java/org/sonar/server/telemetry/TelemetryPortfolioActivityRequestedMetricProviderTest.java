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
package org.sonar.server.telemetry;

import org.junit.jupiter.api.Test;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class TelemetryPortfolioActivityRequestedMetricProviderTest {

  @Test
  void testGetters() {
    TelemetryPortfolioActivityRequestedMetricProvider underTest = new TelemetryPortfolioActivityRequestedMetricProvider();
    underTest.metricRequested("ncloc");
    underTest.metricRequested("ncloc");
    underTest.metricRequested("duplicated_lines");
    underTest.metricRequested("accepted_issues");
    underTest.metricRequested("cognitive_complexity");
    assertThat(underTest.getMetricKey()).isEqualTo("portfolio_activity_requested_metric");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.BOOLEAN);
    assertThat(underTest.getValues()).hasSize(4);
    assertThat(underTest.getValues()).contains(
      entry("ncloc", true),
      entry("duplicated_lines", true),
      entry("accepted_issues", true),
      entry("cognitive_complexity", true));
    underTest.after();
    assertThat(underTest.getValues()).isEmpty();
  }

}
