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
package org.sonar.server.v2.telemetry;

import org.junit.jupiter.api.Test;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryDashboardViewCountProviderTest {

  @Test
  void customDashboardProvider_reportsAndResetsDailyViewCount() {
    TelemetryCustomDashboardViewCountProvider underTest = new TelemetryCustomDashboardViewCountProvider();

    underTest.incrementCount();
    underTest.incrementCount();

    assertThat(underTest.getMetricKey()).isEqualTo("number_of_custom_dashboard_views_daily");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    assertThat(underTest.getValue()).contains(2);

    underTest.after();

    assertThat(underTest.getValue()).contains(0);
  }

  @Test
  void builtInDashboardProvider_reportsAndResetsDailyViewCount() {
    TelemetryBuiltInDashboardViewCountProvider underTest = new TelemetryBuiltInDashboardViewCountProvider();

    underTest.incrementCount();
    underTest.incrementCount();
    underTest.incrementCount();

    assertThat(underTest.getMetricKey()).isEqualTo("number_of_builtin_dashboard_views_daily");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    assertThat(underTest.getValue()).contains(3);

    underTest.after();

    assertThat(underTest.getValue()).contains(0);
  }
}
