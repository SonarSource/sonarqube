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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryAgenticQPAdoptionProviderTest {

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private AgenticQPProjectResolver agenticQPProjectResolver;

  private TelemetryAgenticQPAdoptionProvider underTest;

  @BeforeEach
  void setUp() {
    lenient().when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest = new TelemetryAgenticQPAdoptionProvider(dbClient, agenticQPProjectResolver);
  }

  @Test
  void getValues_returnsCountPerLanguage() {
    when(agenticQPProjectResolver.resolveAgenticProjectUuidsByLanguage(dbSession)).thenReturn(Map.of(
      "java", Set.of("p1", "p2"),
      "python", Set.of("p1")));

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("java", 2, "python", 1));
  }

  @Test
  void getValues_whenNoProjects_returnsEmptyMap() {
    when(agenticQPProjectResolver.resolveAgenticProjectUuidsByLanguage(dbSession)).thenReturn(Map.of());

    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getMetricKey_returnsExpectedKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("agentic_qp_projects_count");
  }

  @Test
  void getDimension_returnsLanguage() {
    assertThat(underTest.getDimension()).isEqualTo(Dimension.LANGUAGE);
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
