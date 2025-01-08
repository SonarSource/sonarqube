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

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.legacy.ProjectLocDistributionDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.legacy.ProjectLocDistributionDataProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.telemetry.TelemetryNclocProvider.METRIC_KEY;

class TelemetryNclocProviderTest {

  private final DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private final ProjectLocDistributionDataProvider projectLocDistributionDataProvider = mock(ProjectLocDistributionDataProvider.class);
  private final DbSession dbSession = mock(DbSession.class);

  @Test
  void getValues_returnsTheRightLanguageDistribution() {
    TelemetryNclocProvider telemetryNclocProvider = new TelemetryNclocProvider(dbClient, projectLocDistributionDataProvider);

    when(dbClient.openSession(false)).thenReturn(dbSession);

    when(projectLocDistributionDataProvider.getProjectLocDistribution(dbSession)).thenReturn(Arrays.asList(
      new ProjectLocDistributionDto("project1", "branch1-p1", "java=5000;xml=1000;js=1000"),
      new ProjectLocDistributionDto("project2", "branch1-p2", "java=10000;csharp=2000"),
      new ProjectLocDistributionDto("project3", "branch1-p3", "java=7000;js=500")
    ));

    assertEquals(METRIC_KEY, telemetryNclocProvider.getMetricKey());
    assertEquals(Granularity.DAILY, telemetryNclocProvider.getGranularity());
    assertEquals(Dimension.LANGUAGE, telemetryNclocProvider.getDimension());

    assertThat(telemetryNclocProvider.getValues()).containsOnlyKeys("java", "xml", "csharp", "js");
    assertThat(telemetryNclocProvider.getValues()).containsEntry("java", 22000L);
    assertThat(telemetryNclocProvider.getValues()).containsEntry("xml", 1000L);
    assertThat(telemetryNclocProvider.getValues()).containsEntry("csharp", 2000L);
    assertThat(telemetryNclocProvider.getValues()).containsEntry("js", 1500L);
  }
}
