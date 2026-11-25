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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.AnalysisPropertiesDao;
import org.sonar.db.component.AnalysisPropertyValuePerProject;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryOnboardingProjectsByAnalysisModeProviderTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final ProjectDao projectDao = mock(ProjectDao.class);
  private final SnapshotDao snapshotDao = mock(SnapshotDao.class);
  private final AnalysisPropertiesDao analysisPropertiesDao = mock(AnalysisPropertiesDao.class);

  private final TelemetryOnboardingProjectsByAnalysisModeProvider underTest = new TelemetryOnboardingProjectsByAnalysisModeProvider(dbClient);

  @Test
  void metadata_shouldBeInstallationDailyInteger() {
    assertThat(underTest.getMetricKey()).isEqualTo("onboarding_projects_by_analysis_mode");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
  }

  @Test
  void getValues_whenMoreProjectsThanTheLargeInputLimit_shouldQueryAnalysesByPartitionsAndAddUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(dbClient.analysisPropertiesDao()).thenReturn(analysisPropertiesDao);
    List<ProjectDto> projects = IntStream.range(0, 2_500)
      .mapToObj(i -> new ProjectDto().setUuid("project-" + i))
      .toList();
    when(projectDao.selectProjects(dbSession)).thenReturn(projects);
    // multiples of 5 are never analysed; among the analysed ones, even indexes have a detected CI
    when(snapshotDao.selectLastAnalysisDateByProjectUuids(eq(dbSession), any())).thenAnswer(invocation -> {
      Collection<String> partition = invocation.getArgument(1);
      assertThat(partition).hasSizeLessThanOrEqualTo(1_000);
      return partition.stream()
        .filter(uuid -> index(uuid) % 5 != 0)
        .map(uuid -> new ProjectLastAnalysisDateDto(uuid, 1L))
        .toList();
    });
    when(analysisPropertiesDao.selectAnalysisPropertyValueInLastAnalysisPerProject(dbSession, "sonar.analysis.detectedci"))
      .thenReturn(IntStream.range(0, 2_500)
        .mapToObj(i -> property("project-" + i, i % 2 == 0 ? "Jenkins" : "undetected"))
        .toList());

    Map<String, Integer> values = underTest.getValues();

    // 2,000 analysed projects (indexes not divisible by 5): 1,000 even (CI detected) and 1,000 odd (undetected)
    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 1_000, "ci_undetected", 1_000));
  }

  @Test
  void getValues_whenDetectedCiPropertyHasNoValue_shouldCountAsCiUndetected() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(dbClient.analysisPropertiesDao()).thenReturn(analysisPropertiesDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(new ProjectDto().setUuid("p1")));
    when(snapshotDao.selectLastAnalysisDateByProjectUuids(eq(dbSession), any())).thenReturn(List.of(new ProjectLastAnalysisDateDto("p1", 1L)));
    when(analysisPropertiesDao.selectAnalysisPropertyValueInLastAnalysisPerProject(dbSession, "sonar.analysis.detectedci"))
      .thenReturn(List.of(property("p1", null)));

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("ci_detected", 0, "ci_undetected", 1));
  }

  private static int index(String uuid) {
    return Integer.parseInt(uuid.substring("project-".length()));
  }

  private static AnalysisPropertyValuePerProject property(String projectUuid, String value) {
    AnalysisPropertyValuePerProject property = new AnalysisPropertyValuePerProject();
    property.setProjectUuid(projectUuid);
    property.setPropertyValue(value);
    return property;
  }
}
