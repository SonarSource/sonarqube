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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;

class TelemetryQualityGateBeforeNcdStartProviderIT {

  private static final long NOW_MS = 1_000_000_000L;
  private static final long FIFTEEN_MINUTES_MS = 15 * 60 * 1000L;
  private static final long THIRTEEN_MINUTES_MS = 13 * 60 * 1000L;
  private static final long TWO_MINUTES_MS = 2 * 60 * 1000L;
  private static final long ONE_MINUTE_MS = 60 * 1000L;

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW_MS);

  @RegisterExtension
  public DbTester db = DbTester.create(system2);

  private final TelemetryQualityGateBeforeNcdStartProvider underTest = new TelemetryQualityGateBeforeNcdStartProvider(db.getDbClient());

  @Test
  void shouldReturnQualityGateStatus_whenProjectHasCompleteData() {
    // Create project with main branch
    ProjectData projectData = db.components().insertPublicProject();
    BranchDto mainBranch = projectData.getMainBranchDto();

    // Create metric for alert status
    MetricDto metricDto = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));

    // Snapshot 1: 15 minutes ago, QG status was OK
    long snapshot1Date = system2.now() - FIFTEEN_MINUTES_MS;
    SnapshotDto snapshot1 = db.components().insertSnapshot(mainBranch, s -> s
      .setCreatedAt(snapshot1Date)
      .setStatus(STATUS_PROCESSED)
      .setLast(false));

    ProjectMeasureDto measure1 = new ProjectMeasureDto()
      .setComponentUuid(mainBranch.getUuid())
      .setMetricUuid(metricDto.getUuid())
      .setAnalysisUuid(snapshot1.getUuid())
      .setAlertStatus(OK.name());
    db.getDbClient().projectMeasureDao().insert(db.getSession(), measure1);

    // Snapshot 2: 2 minutes ago (latest), QG status is ERROR
    long snapshot2Date = system2.now() - TWO_MINUTES_MS;
    long periodStartDate = snapshot1Date + ONE_MINUTE_MS; // Period starts at 14 minutes ago (between snapshot1 and snapshot2)

    SnapshotDto snapshot2 = db.components().insertSnapshot(mainBranch, s -> s
      .setCreatedAt(snapshot2Date)
      .setStatus(STATUS_PROCESSED)
      .setLast(true)
      .setPeriodMode(NewCodePeriodType.NUMBER_OF_DAYS.name())
      .setPeriodParam("30")
      .setPeriodDate(periodStartDate));

    ProjectMeasureDto measure2 = new ProjectMeasureDto()
      .setComponentUuid(mainBranch.getUuid())
      .setMetricUuid(metricDto.getUuid())
      .setAnalysisUuid(snapshot2.getUuid())
      .setAlertStatus(ERROR.name());
    db.getDbClient().projectMeasureDao().insert(db.getSession(), measure2);

    db.commit();

    // Execute
    Map<String, String> result = underTest.getValues();

    // Verify - should return the status from snapshot BEFORE the period start (snapshot1 with OK)
    assertThat(result)
      .hasSize(1)
      .containsEntry(projectData.getProjectDto().getUuid(), OK.name());
  }

  @Test
  void shouldReturnEmpty_whenNoPeriodDateConfigured() {
    // Create project with main branch
    ProjectData projectData = db.components().insertPublicProject();
    BranchDto mainBranch = projectData.getMainBranchDto();

    // Create snapshot without period date
    db.components().insertSnapshot(mainBranch, s -> s
      .setCreatedAt(system2.now())
      .setStatus(STATUS_PROCESSED)
      .setLast(true)
      .setPeriodDate(null));

    db.commit();

    // Execute
    Map<String, String> result = underTest.getValues();

    // Verify - no period date means no data
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmpty_whenNoSnapshotExistsBeforePeriodStart() {
    // Create project with main branch
    ProjectData projectData = db.components().insertPublicProject();
    BranchDto mainBranch = projectData.getMainBranchDto();

    // Create only one snapshot with period that starts before any snapshot exists
    long snapshotDate = system2.now();
    long periodStartDate = snapshotDate - (20 * FIFTEEN_MINUTES_MS); // Period starts way before first snapshot

    db.components().insertSnapshot(mainBranch, s -> s
      .setCreatedAt(snapshotDate)
      .setStatus(STATUS_PROCESSED)
      .setLast(true)
      .setPeriodMode(NewCodePeriodType.NUMBER_OF_DAYS.name())
      .setPeriodDate(periodStartDate));

    db.commit();

    // Execute
    Map<String, String> result = underTest.getValues();

    // Verify - no snapshot before period means no data
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnAllStatuses_whenMultipleProjectsHaveData() {
    // Create first project with QG status OK
    ProjectData project1 = db.components().insertPublicProject();
    BranchDto mainBranch1 = project1.getMainBranchDto();
    MetricDto metricDto = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));

    long snapshot1Date = system2.now() - FIFTEEN_MINUTES_MS;
    SnapshotDto snapshot1 = db.components().insertSnapshot(mainBranch1, s -> s
      .setCreatedAt(snapshot1Date)
      .setStatus(STATUS_PROCESSED)
      .setLast(false));

    ProjectMeasureDto measure1 = new ProjectMeasureDto()
      .setComponentUuid(mainBranch1.getUuid())
      .setMetricUuid(metricDto.getUuid())
      .setAnalysisUuid(snapshot1.getUuid())
      .setAlertStatus(OK.name());
    db.getDbClient().projectMeasureDao().insert(db.getSession(), measure1);

    db.components().insertSnapshot(mainBranch1, s -> s
      .setCreatedAt(system2.now())
      .setStatus(STATUS_PROCESSED)
      .setLast(true)
      .setPeriodDate(snapshot1Date + ONE_MINUTE_MS));

    // Create second project with QG status ERROR
    ProjectData project2 = db.components().insertPublicProject();
    BranchDto mainBranch2 = project2.getMainBranchDto();

    long snapshot2Date = system2.now() - THIRTEEN_MINUTES_MS;
    SnapshotDto snapshot2 = db.components().insertSnapshot(mainBranch2, s -> s
      .setCreatedAt(snapshot2Date)
      .setStatus(STATUS_PROCESSED)
      .setLast(false));

    ProjectMeasureDto measure2 = new ProjectMeasureDto()
      .setComponentUuid(mainBranch2.getUuid())
      .setMetricUuid(metricDto.getUuid())
      .setAnalysisUuid(snapshot2.getUuid())
      .setAlertStatus(ERROR.name());
    db.getDbClient().projectMeasureDao().insert(db.getSession(), measure2);

    db.components().insertSnapshot(mainBranch2, s -> s
      .setCreatedAt(system2.now())
      .setStatus(STATUS_PROCESSED)
      .setLast(true)
      .setPeriodDate(snapshot2Date + ONE_MINUTE_MS));

    db.commit();

    // Execute
    Map<String, String> result = underTest.getValues();

    // Verify - both projects with their respective statuses
    assertThat(result)
      .hasSize(2)
      .containsEntry(project1.getProjectDto().getUuid(), OK.name())
      .containsEntry(project2.getProjectDto().getUuid(), ERROR.name());
  }
}
