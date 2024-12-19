/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.telemetry.legacy;

import java.util.ArrayList;
import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.project.ProjectDto;

import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;

public class ProjectLocDistributionDataProvider {

  private final DbClient dbClient;

  public ProjectLocDistributionDataProvider(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Returns the loc distribution of the largest branch of each project.
   */
  public List<ProjectLocDistributionDto> getProjectLocDistribution(DbSession dbSession) {
    List<ProjectLocDistributionDto> branchesWithLargestNcloc = new ArrayList<>();
    List<ProjectDto> projects = dbClient.projectDao().selectProjects(dbSession);
    for (ProjectDto project : projects) {
      List<String> branchUuids = dbClient.branchDao().selectByProjectUuid(dbSession, project.getUuid()).stream()
        .map(BranchDto::getUuid)
        .toList();
      List<MeasureDto> branchMeasures = dbClient.measureDao()
        .selectByComponentUuidsAndMetricKeys(dbSession, branchUuids, List.of(NCLOC_KEY, NCLOC_LANGUAGE_DISTRIBUTION_KEY));

      long maxncloc = 0;
      String largestBranchUuid = null;
      String locDistribution = null;
      for (MeasureDto measure : branchMeasures) {
        Long branchNcloc = measure.getLong(NCLOC_KEY);
        if (branchNcloc != null && branchNcloc >= maxncloc) {
          maxncloc = branchNcloc;
          largestBranchUuid = measure.getBranchUuid();
          locDistribution = measure.getString(NCLOC_LANGUAGE_DISTRIBUTION_KEY);
        }
      }

      if (locDistribution != null) {
        branchesWithLargestNcloc.add(new ProjectLocDistributionDto(project.getUuid(), largestBranchUuid, locDistribution));
      }
    }
    return branchesWithLargestNcloc;
  }
}
