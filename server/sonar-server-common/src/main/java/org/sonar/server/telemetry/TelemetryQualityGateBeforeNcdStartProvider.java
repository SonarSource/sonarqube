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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.ViewsSnapshotDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

@ServerSide
public class TelemetryQualityGateBeforeNcdStartProvider extends AbstractTelemetryDataProvider<String> {
  private static final Logger LOG = LoggerFactory.getLogger(TelemetryQualityGateBeforeNcdStartProvider.class);
  private static final String METRIC_KEY = "project_main_branch_qg_at_nc_start";
  private final DbClient dbClient;

  public TelemetryQualityGateBeforeNcdStartProvider(DbClient dbClient) {
    super(METRIC_KEY, Dimension.PROJECT, Granularity.MONTHLY, TelemetryDataType.STRING);
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, String> getValues() {
    long startTime = System.currentTimeMillis();
    LOG.debug("Starting quality gate before NCD telemetry collection");

    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, String> result = new HashMap<>();

      List<ProjectDto> projects = dbClient.projectDao().selectProjects(dbSession);
      if (projects.isEmpty()) {
        LOG.debug("No projects found");
        return result;
      }

      Map<String, BranchDto> mainBranchesByProjectUuid = getMainBranches(dbSession, projects);
      if (mainBranchesByProjectUuid.isEmpty()) {
        LOG.debug("No main branches found for projects");
        return result;
      }

      Map<String, Long> periodDatesByBranchUuid = getPeriodDatesFromLatestSnapshots(dbSession, mainBranchesByProjectUuid.values());
      if (periodDatesByBranchUuid.isEmpty()) {
        LOG.debug("No period data found for any of the main branches of all projects");
        return result;
      }

      Map<String, String> snapshotsBeforePeriodByBranchUuid = getSnapshotsBeforePeriod(dbSession, periodDatesByBranchUuid);
      if (snapshotsBeforePeriodByBranchUuid.isEmpty()) {
        LOG.debug("No pre-ncd snapshot data found for any of the main branches of all projects");
        return result;
      }

      Map<String, String> alertStatusesByAnalysisUuid = getAlertStatuses(dbSession, snapshotsBeforePeriodByBranchUuid);
      if (alertStatusesByAnalysisUuid.isEmpty()) {
        LOG.debug("No alert_status measure data found for any of the analyses");
        return result;
      }

      // Map results back to project UUIDs
      for (Map.Entry<String, BranchDto> entry : mainBranchesByProjectUuid.entrySet()) {
        String projectUuid = entry.getKey();
        String branchUuid = entry.getValue().getUuid();
        String snapshotUuid = snapshotsBeforePeriodByBranchUuid.get(branchUuid);

        if (snapshotUuid != null) {
          String alertStatus = alertStatusesByAnalysisUuid.get(snapshotUuid);
          if (alertStatus != null) {
            result.put(projectUuid, alertStatus);
          } else {
            LOG.debug("No historical quality gate measure data found for snapshot uuid {}", snapshotUuid);
          }
        } else {
          LOG.debug("No pre-ncd snapshot data found for project with UUID {}", projectUuid);
        }
      }

      long durationMs = System.currentTimeMillis() - startTime;
      int totalProjects = projects.size();
      int projectsWithData = result.size();
      int projectsSkipped = totalProjects - projectsWithData;

      LOG.debug("Collected QG before NCD data: {} projects with data, {} skipped out of {} total ({}ms)",
        projectsWithData, projectsSkipped, totalProjects, durationMs);

      return result;
    }
  }

  private Map<String, BranchDto> getMainBranches(DbSession session, List<ProjectDto> projects) {
    List<String> projectUuids = projects.stream().map(ProjectDto::getUuid).toList();
    List<BranchDto> branches = dbClient.branchDao().selectMainBranchesByProjectUuids(session, projectUuids);

    return branches.stream()
      .collect(Collectors.toMap(BranchDto::getProjectUuid, branch -> branch));
  }

  private Map<String, Long> getPeriodDatesFromLatestSnapshots(DbSession session, Collection<BranchDto> branches) {
    List<String> branchUuids = branches.stream().map(BranchDto::getUuid).toList();
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(session, branchUuids);

    return snapshots.stream()
      .filter(snapshot -> snapshot.getPeriodDate() != null)
      .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, SnapshotDto::getPeriodDate));
  }

  private Map<String, String> getSnapshotsBeforePeriod(DbSession session, Map<String, Long> periodDatesByBranchUuid) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Long> entry : periodDatesByBranchUuid.entrySet()) {
      String branchUuid = entry.getKey();
      Long periodDate = entry.getValue();
      if (periodDate != null) {
        ViewsSnapshotDto snapshot = dbClient.snapshotDao().selectSnapshotBefore(branchUuid, periodDate, session);
        if (snapshot != null) {
          result.put(branchUuid, snapshot.getUuid());
        }
      }
    }
    return result;
  }

  private Map<String, String> getAlertStatuses(DbSession session, Map<String, String> snapshotsByBranchUuid) {
    Collection<String> analysisUuids = snapshotsByBranchUuid.values();

    return dbClient.projectMeasureDao()
      .selectMeasuresByAnalysisUuids(session, analysisUuids, ALERT_STATUS_KEY)
      .stream().collect(Collectors.toMap(ProjectMeasureDto::getAnalysisUuid, ProjectMeasureDto::getAlertStatus));
  }
}
