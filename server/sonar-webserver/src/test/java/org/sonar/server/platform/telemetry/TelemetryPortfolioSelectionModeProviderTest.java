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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.portfolio.PortfolioDao;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryPortfolioSelectionModeProviderTest {
  private final DbClient dbClient = mock(DbClient.class);
  private final PortfolioDao portfolioDao = mock(PortfolioDao.class);

  @Test
  void testGetters() {
    when(dbClient.portfolioDao()).thenReturn(portfolioDao);
    TelemetryPortfolioSelectionModeProvider underTest = new TelemetryPortfolioSelectionModeProvider(dbClient);

    assertThat(underTest.getMetricKey()).isEqualTo("portfolio_using_selection_mode");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
    Map<String, Integer> expected = Map.of("REGEXP", 42, "REST", 7);
    when(portfolioDao.countPortfoliosByMode(any())).thenReturn(expected);
    assertThat(underTest.getValues()).containsAllEntriesOf(expected);
  }
}
