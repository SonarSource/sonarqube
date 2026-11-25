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
import org.sonar.db.alm.setting.ProjectAlmKeyAndProject;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.component.ProjectLastAnalysisDateDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryOnboardingBoundUnanalysedByAlmProviderTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
  private final SnapshotDao snapshotDao = mock(SnapshotDao.class);

  private final TelemetryOnboardingBoundUnanalysedByAlmProvider underTest = new TelemetryOnboardingBoundUnanalysedByAlmProvider(dbClient);

  @Test
  void metadata_shouldBeInstallationDailyInteger() {
    assertThat(underTest.getMetricKey()).isEqualTo("onboarding_bound_unanalysed_by_alm");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
  }

  @Test
  void getValues_whenMoreBoundProjectsThanTheLargeInputLimit_shouldQueryAnalysesByPartitions() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    List<ProjectAlmKeyAndProject> bindings = IntStream.range(0, 2_500)
      .mapToObj(i -> new ProjectAlmKeyAndProject().setProjectUuid("project-" + i).setAlmId(i % 2 == 0 ? "github" : "gitlab"))
      .toList();
    when(projectAlmSettingDao.selectAlmTypeAndUrlByProject(dbSession)).thenReturn(bindings);
    when(snapshotDao.selectLastAnalysisDateByProjectUuids(eq(dbSession), any())).thenAnswer(invocation -> {
      Collection<String> partition = invocation.getArgument(1);
      assertThat(partition).hasSizeLessThanOrEqualTo(1_000);
      return partition.stream()
        .filter(uuid -> Integer.parseInt(uuid.substring("project-".length())) % 5 != 0)
        .map(uuid -> new ProjectLastAnalysisDateDto(uuid, 1L))
        .toList();
    });

    Map<String, Integer> values = underTest.getValues();

    // 500 of the 2,500 projects (every fifth) were never analysed; project-0, -10, ... are github, project-5, -15, ... are gitlab
    assertThat(values).containsExactlyInAnyOrderEntriesOf(Map.of(
      "github", 250,
      "gitlab", 250,
      "azure_devops", 0,
      "bitbucket", 0,
      "bitbucket_cloud", 0));
  }
}
