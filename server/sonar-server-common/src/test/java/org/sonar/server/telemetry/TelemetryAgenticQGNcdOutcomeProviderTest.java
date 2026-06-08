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

import java.util.List;
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
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasureDao;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

@ExtendWith(MockitoExtension.class)
class TelemetryAgenticQGNcdOutcomeProviderTest {

  private static final String PROJECT_UUID_1 = "project-uuid-1";
  private static final String PROJECT_UUID_2 = "project-uuid-2";
  private static final String BRANCH_UUID_1 = "branch-uuid-1";
  private static final String BRANCH_UUID_2 = "branch-uuid-2";
  private static final String SNAPSHOT_UUID_1 = "snapshot-uuid-1";
  private static final String SNAPSHOT_UUID_2 = "snapshot-uuid-2";
  private static final long PERIOD_DATE = 1_000_000L;

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private AgenticQGProjectResolver agenticQGProjectResolver;
  @Mock
  private BranchDao branchDao;
  @Mock
  private SnapshotDao snapshotDao;
  @Mock
  private ProjectMeasureDao projectMeasureDao;

  private TelemetryAgenticQGNcdOutcomeProvider underTest;

  @BeforeEach
  void setUp() {
    lenient().when(dbClient.openSession(false)).thenReturn(dbSession);
    lenient().when(dbClient.branchDao()).thenReturn(branchDao);
    lenient().when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    lenient().when(dbClient.projectMeasureDao()).thenReturn(projectMeasureDao);
    underTest = new TelemetryAgenticQGNcdOutcomeProvider(dbClient, agenticQGProjectResolver);
  }

  @Test
  void getValue_whenNoProjectsUseAgenticQG_returnsZero() {
    when(agenticQGProjectResolver.resolveAgenticProjectUuids(dbSession)).thenReturn(Set.of());

    assertThat(underTest.getValue()).contains(0);
  }

  @ParameterizedTest
  @MethodSource("provideGateStatusScenarios")
  void getValue_whenProjectHasAgenticQGAndGateStatusAtNcdReset_returnsExpectedCount(String alertStatus, int expectedCount) {
    when(agenticQGProjectResolver.resolveAgenticProjectUuids(dbSession)).thenReturn(Set.of(PROJECT_UUID_1));
    BranchDto branch = createBranchDto(BRANCH_UUID_1, PROJECT_UUID_1);
    when(branchDao.selectMainBranchesByProjectUuids(eq(dbSession), argThat(c -> c.contains(PROJECT_UUID_1)))).thenReturn(List.of(branch));
    SnapshotDto snapshot = createSnapshot(SNAPSHOT_UUID_1, BRANCH_UUID_1, PERIOD_DATE);
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(dbSession, List.of(BRANCH_UUID_1))).thenReturn(List.of(snapshot));
    when(projectMeasureDao.selectMeasuresByAnalysisUuids(eq(dbSession), argThat(c -> c.contains(SNAPSHOT_UUID_1)), eq(ALERT_STATUS_KEY)))
      .thenReturn(List.of(createMeasure(alertStatus)));

    assertThat(underTest.getValue()).contains(expectedCount);
  }

  private static Stream<Arguments> provideGateStatusScenarios() {
    return Stream.of(
      arguments("ERROR", 1),
      arguments("OK", 0));
  }

  @Test
  void getValue_whenSnapshotHasNoPeriodDate_skipsProject() {
    when(agenticQGProjectResolver.resolveAgenticProjectUuids(dbSession)).thenReturn(Set.of(PROJECT_UUID_1));
    BranchDto branch = createBranchDto(BRANCH_UUID_1, PROJECT_UUID_1);
    when(branchDao.selectMainBranchesByProjectUuids(eq(dbSession), argThat(c -> c.contains(PROJECT_UUID_1)))).thenReturn(List.of(branch));
    SnapshotDto snapshotWithoutPeriod = createSnapshot(SNAPSHOT_UUID_1, BRANCH_UUID_1, null);
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(dbSession, List.of(BRANCH_UUID_1))).thenReturn(List.of(snapshotWithoutPeriod));

    assertThat(underTest.getValue()).contains(0);
  }

  @Test
  void getValue_whenMultipleProjectsWithMixedOutcomes_returnsCorrectCount() {
    when(agenticQGProjectResolver.resolveAgenticProjectUuids(dbSession)).thenReturn(Set.of(PROJECT_UUID_1, PROJECT_UUID_2));

    BranchDto branch1 = createBranchDto(BRANCH_UUID_1, PROJECT_UUID_1);
    BranchDto branch2 = createBranchDto(BRANCH_UUID_2, PROJECT_UUID_2);
    when(branchDao.selectMainBranchesByProjectUuids(eq(dbSession),
      argThat(c -> c.containsAll(List.of(PROJECT_UUID_1, PROJECT_UUID_2)) && c.size() == 2)))
      .thenReturn(List.of(branch1, branch2));

    SnapshotDto snapshot1 = createSnapshot(SNAPSHOT_UUID_1, BRANCH_UUID_1, PERIOD_DATE);
    SnapshotDto snapshot2 = createSnapshot(SNAPSHOT_UUID_2, BRANCH_UUID_2, PERIOD_DATE);
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(dbSession, List.of(BRANCH_UUID_1, BRANCH_UUID_2)))
      .thenReturn(List.of(snapshot1, snapshot2));

    when(projectMeasureDao.selectMeasuresByAnalysisUuids(eq(dbSession),
      argThat(c -> c.containsAll(List.of(SNAPSHOT_UUID_1, SNAPSHOT_UUID_2)) && c.size() == 2), eq(ALERT_STATUS_KEY)))
      .thenReturn(List.of(createMeasure("ERROR"), createMeasure("OK")));

    assertThat(underTest.getValue()).contains(1);
  }

  @Test
  void getMetricKey_returnsExpectedKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("agentic_qg_ncd_failed_count");
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

  private static BranchDto createBranchDto(String uuid, String projectUuid) {
    BranchDto dto = mock(BranchDto.class);
    when(dto.getUuid()).thenReturn(uuid);
    lenient().when(dto.getProjectUuid()).thenReturn(projectUuid);
    return dto;
  }

  private static SnapshotDto createSnapshot(String uuid, String rootComponentUuid, Long periodDate) {
    SnapshotDto dto = mock(SnapshotDto.class);
    lenient().when(dto.getUuid()).thenReturn(uuid);
    lenient().when(dto.getRootComponentUuid()).thenReturn(rootComponentUuid);
    when(dto.getPeriodDate()).thenReturn(periodDate);
    return dto;
  }

  private static ProjectMeasureDto createMeasure(String alertStatus) {
    return new ProjectMeasureDto().setAlertStatus(alertStatus);
  }
}
