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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.ViewsSnapshotDto;
import org.sonar.db.measure.ProjectMeasureDao;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

@ExtendWith(MockitoExtension.class)
class TelemetryQualityGateBeforeNcdStartProviderTest {

  @Mock
  private DbClient dbClient;

  @Mock
  private DbSession dbSession;

  @Mock
  private ProjectDao projectDao;

  @Mock
  private BranchDao branchDao;

  @Mock
  private SnapshotDao snapshotDao;

  @Mock
  private ProjectMeasureDao projectMeasureDao;

  @InjectMocks
  private TelemetryQualityGateBeforeNcdStartProvider underTest;

  @Test
  void getMetricKey_returnsCorrectKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("project_main_branch_qg_at_nc_start");
  }

  @Test
  void getDimension_returnsProject() {
    assertThat(underTest.getDimension()).isEqualTo(Dimension.PROJECT);
  }

  @Test
  void getGranularity_returnsMonthly() {
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.MONTHLY);
  }

  @Test
  void getType_returnsString() {
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.STRING);
  }

  @Test
  void getValues_whenNoProjects_returnsEmptyMap() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of());

    Map<String, String> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenProjectHasNoMainBranch_skipsProject() {
    ProjectDto project = createProject("project-uuid-1");

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection())).thenReturn(List.of());

    Map<String, String> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenMainBranchHasNoSnapshot_skipsProject() {
    ProjectDto project = createProject("project-uuid-1");
    BranchDto mainBranch = createMainBranch("branch-uuid-1", "project-uuid-1");

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(mainBranch));
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of());

    Map<String, String> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenSnapshotHasNoPeriodDate_skipsProject() {
    ProjectDto project = createProject("project-uuid-1");
    BranchDto mainBranch = createMainBranch("branch-uuid-1", "project-uuid-1");
    SnapshotDto snapshot = createSnapshot("snapshot-uuid-1", "branch-uuid-1", null);

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(mainBranch));
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(snapshot));

    Map<String, String> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenNoSnapshotBeforePeriod_skipsProject() {
    ProjectDto project = createProject("project-uuid-1");
    BranchDto mainBranch = createMainBranch("branch-uuid-1", "project-uuid-1");
    SnapshotDto snapshot = createSnapshot("snapshot-uuid-1", "branch-uuid-1", 1000000L);

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(mainBranch));
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(snapshot));
    when(snapshotDao.selectSnapshotBefore("branch-uuid-1", 1000000L, dbSession))
      .thenReturn(null);

    Map<String, String> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenNoMeasureForSnapshot_skipsProject() {
    ProjectDto project = createProject("project-uuid-1");
    BranchDto mainBranch = createMainBranch("branch-uuid-1", "project-uuid-1");
    SnapshotDto snapshot = createSnapshot("snapshot-uuid-1", "branch-uuid-1", 1000000L);
    ViewsSnapshotDto snapshotBeforePeriod = createViewsSnapshot("snapshot-before-uuid");

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(dbClient.projectMeasureDao()).thenReturn(projectMeasureDao);
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(mainBranch));
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(snapshot));
    when(snapshotDao.selectSnapshotBefore("branch-uuid-1", 1000000L, dbSession))
      .thenReturn(snapshotBeforePeriod);
    when(projectMeasureDao.selectMeasuresByAnalysisUuids(any(DbSession.class), anyCollection(), eq(ALERT_STATUS_KEY)))
      .thenReturn(List.of());

    Map<String, String> result = underTest.getValues();

    assertThat(result).isEmpty();
  }

  @Test
  void getValues_whenProjectHasQualityGateStatus_returnsStatus() {
    ProjectDto project1 = createProject("project-uuid-1");
    ProjectDto project2 = createProject("project-uuid-2");
    BranchDto mainBranch1 = createMainBranch("branch-uuid-1", "project-uuid-1");
    BranchDto mainBranch2 = createMainBranch("branch-uuid-2", "project-uuid-2");
    SnapshotDto snapshot1 = createSnapshot("snapshot-uuid-1", "branch-uuid-1", 1000000L);
    SnapshotDto snapshot2 = createSnapshot("snapshot-uuid-2", "branch-uuid-2", 2000000L);
    ViewsSnapshotDto snapshotBeforePeriod1 = createViewsSnapshot("snapshot-before-uuid-1");
    ViewsSnapshotDto snapshotBeforePeriod2 = createViewsSnapshot("snapshot-before-uuid-2");
    ProjectMeasureDto measure1 = createMeasure("OK", "snapshot-before-uuid-1");
    ProjectMeasureDto measure2 = createMeasure("ERROR", "snapshot-before-uuid-2");

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(dbClient.projectMeasureDao()).thenReturn(projectMeasureDao);

    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project1, project2));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(mainBranch1, mainBranch2));
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(snapshot1, snapshot2));
    when(snapshotDao.selectSnapshotBefore("branch-uuid-1", 1000000L, dbSession))
      .thenReturn(snapshotBeforePeriod1);
    when(snapshotDao.selectSnapshotBefore("branch-uuid-2", 2000000L, dbSession))
      .thenReturn(snapshotBeforePeriod2);
    when(projectMeasureDao.selectMeasuresByAnalysisUuids(any(DbSession.class), anyCollection(), eq(ALERT_STATUS_KEY)))
      .thenReturn(List.of(measure1, measure2));

    Map<String, String> result = underTest.getValues();

    assertThat(result)
      .hasSize(2)
      .containsEntry("project-uuid-1", "OK")
      .containsEntry("project-uuid-2", "ERROR");
  }

  @Test
  void getValues_withMixOfValidAndInvalidProjects_returnsOnlyValidOnes() {
    ProjectDto project1 = createProject("project-uuid-1");
    ProjectDto project2 = createProject("project-uuid-2"); // This one will have no main branch
    ProjectDto project3 = createProject("project-uuid-3");

    BranchDto mainBranch1 = createMainBranch("branch-uuid-1", "project-uuid-1");
    BranchDto mainBranch3 = createMainBranch("branch-uuid-3", "project-uuid-3");

    SnapshotDto snapshot1 = createSnapshot("snapshot-uuid-1", "branch-uuid-1", 1000000L);
    SnapshotDto snapshot3 = createSnapshot("snapshot-uuid-3", "branch-uuid-3", 3000000L);

    ViewsSnapshotDto snapshotBeforePeriod1 = createViewsSnapshot("snapshot-before-uuid-1");
    ViewsSnapshotDto snapshotBeforePeriod3 = createViewsSnapshot("snapshot-before-uuid-3");

    ProjectMeasureDto measure1 = createMeasure("OK", "snapshot-before-uuid-1");
    ProjectMeasureDto measure3 = createMeasure("WARN", "snapshot-before-uuid-3");

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);
    when(dbClient.projectMeasureDao()).thenReturn(projectMeasureDao);

    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project1, project2, project3));
    when(branchDao.selectMainBranchesByProjectUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(mainBranch1, mainBranch3));
    when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(DbSession.class), anyCollection()))
      .thenReturn(List.of(snapshot1, snapshot3));
    when(snapshotDao.selectSnapshotBefore("branch-uuid-1", 1000000L, dbSession))
      .thenReturn(snapshotBeforePeriod1);
    when(snapshotDao.selectSnapshotBefore("branch-uuid-3", 3000000L, dbSession))
      .thenReturn(snapshotBeforePeriod3);
    when(projectMeasureDao.selectMeasuresByAnalysisUuids(any(DbSession.class), anyCollection(), eq(ALERT_STATUS_KEY)))
      .thenReturn(List.of(measure1, measure3));

    Map<String, String> result = underTest.getValues();

    assertThat(result)
      .hasSize(2)
      .containsEntry("project-uuid-1", "OK")
      .containsEntry("project-uuid-3", "WARN")
      .doesNotContainKey("project-uuid-2");
  }

  private ProjectDto createProject(String uuid) {
    ProjectDto project = new ProjectDto();
    project.setUuid(uuid);
    return project;
  }

  private BranchDto createMainBranch(String uuid, String projectUuid) {
    BranchDto branch = new BranchDto();
    branch.setUuid(uuid);
    branch.setProjectUuid(projectUuid);
    branch.setIsMain(true);
    return branch;
  }

  private SnapshotDto createSnapshot(String uuid, String rootComponentUuid, Long periodDate) {
    SnapshotDto snapshot = new SnapshotDto();
    snapshot.setUuid(uuid);
    snapshot.setRootComponentUuid(rootComponentUuid);
    snapshot.setPeriodDate(periodDate);
    return snapshot;
  }

  private ViewsSnapshotDto createViewsSnapshot(String uuid) {
    ViewsSnapshotDto snapshot = new ViewsSnapshotDto();
    snapshot.setUuid(uuid);
    return snapshot;
  }

  private ProjectMeasureDto createMeasure(String alertStatus, String analysisUuid) {
    ProjectMeasureDto measure = new ProjectMeasureDto();
    measure.setAlertStatus(alertStatus);
    measure.setAnalysisUuid(analysisUuid);
    return measure;
  }
}
