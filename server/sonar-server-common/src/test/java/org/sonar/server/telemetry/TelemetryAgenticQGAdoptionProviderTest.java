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
package org.sonar.server.telemetry;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryAgenticQGAdoptionProviderTest {

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private AgenticQGProjectResolver agenticQGProjectResolver;

  private TelemetryAgenticQGAdoptionProvider underTest;

  @BeforeEach
  void setUp() {
    lenient().when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest = new TelemetryAgenticQGAdoptionProvider(dbClient, agenticQGProjectResolver);
  }

  @ParameterizedTest
  @MethodSource("provideProjectCountScenarios")
  void getValue_returnsCountOfAgenticProjects(Set<String> projectUuids, int expectedCount) {
    when(agenticQGProjectResolver.resolveAgenticProjectUuids(dbSession)).thenReturn(projectUuids);

    assertThat(underTest.getValue()).contains(expectedCount);
  }

  private static Stream<Arguments> provideProjectCountScenarios() {
    return Stream.of(
      arguments(Set.of(), 0),
      arguments(Set.of("p1", "p2"), 2));
  }

  @Test
  void getMetricKey_returnsExpectedKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("agentic_qg_projects_count");
  }

  @Test
  void getDimension_returnsInstallation() {
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
  }

  @Test
  void getGranularity_returnsWeekly() {
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.WEEKLY);
  }

  @Test
  void getType_returnsInteger() {
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
  }
}
