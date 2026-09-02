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
package org.sonar.server.platform.telemetry;

import org.sonarsource.reporting.dashboards.DashboardResourceType;
import org.junit.jupiter.api.Test;
import org.sonar.server.platform.DashboardCountService;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryCustomDashboardCountProviderTest {
  private final DashboardCountService dashboardCountService = mock();

  @Test
  void portfolioProvider_reportsLivePortfolioDashboardCount() {
    when(dashboardCountService.count(DashboardResourceType.PORTFOLIO)).thenReturn(3);

    TelemetryCustomPortfolioDashboardCountProvider underTest = new TelemetryCustomPortfolioDashboardCountProvider(dashboardCountService);

    assertThat(underTest.getMetricKey()).isEqualTo("number_of_custom_portfolio_dashboards");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    assertThat(underTest.getValue()).contains(3);
    verify(dashboardCountService).count(DashboardResourceType.PORTFOLIO);
  }

  @Test
  void projectProvider_reportsLiveProjectDashboardCount() {
    when(dashboardCountService.count(DashboardResourceType.PROJECT)).thenReturn(4);

    TelemetryCustomProjectDashboardCountProvider underTest = new TelemetryCustomProjectDashboardCountProvider(dashboardCountService);

    assertThat(underTest.getMetricKey()).isEqualTo("number_of_custom_project_dashboards");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    assertThat(underTest.getValue()).contains(4);
    verify(dashboardCountService).count(DashboardResourceType.PROJECT);
  }

  @Test
  void communityProviders_reportZeroDashboardCounts() {
    DashboardCountService dashboardCountService = new DefaultDashboardCountService();

    assertThat(new TelemetryCustomPortfolioDashboardCountProvider(dashboardCountService).getValue()).contains(0);
    assertThat(new TelemetryCustomProjectDashboardCountProvider(dashboardCountService).getValue()).contains(0);
  }
}
