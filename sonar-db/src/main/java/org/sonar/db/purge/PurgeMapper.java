/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.purge;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface PurgeMapper {

  List<IdUuidPair> selectAnalysisIdsAndUuids(PurgeSnapshotQuery query);

  /**
   * Returns the list of components of a project from a project_uuid. The project itself is also returned.
   */
  List<IdUuidPair> selectComponentsByProjectUuid(String projectUuid);

  void deleteAnalyses(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisDuplications(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisEvents(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisMeasures(@Param("analysisUuids") List<String> analysisUuids);

  void deleteSnapshotMeasures(@Param("analysisUuids") List<String> analysisUuids);

  void deleteComponentMeasures(@Param("analysisUuids") List<String> analysisUuids, @Param("componentUuids") List<String> componentUuids);

  List<Long> selectMetricIdsWithoutHistoricalData();

  void deleteAnalysisWastedMeasures(@Param("analysisUuids") List<String> analysisUuids, @Param("metricIds") List<Long> metricIds);

  void updatePurgeStatusToOne(@Param("analysisUuids") List<String> analysisUuid);

  void resolveComponentIssuesNotAlreadyResolved(@Param("componentUuids") List<String> componentUuids, @Param("dateAsLong") Long dateAsLong);

  void deleteResourceIndex(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentLinks(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentProperties(@Param("componentIds") List<Long> componentIds);

  void deleteComponents(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentGroupRoles(@Param("componentIds") List<Long> componentIds);

  void deleteComponentUserRoles(@Param("componentIds") List<Long> componentIds);

  void deleteComponentManualMeasures(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentEvents(@Param("componentUuids") List<String> componentUuids);

  void deleteAuthors(@Param("resourceIds") List<Long> resourceIds);

  List<PurgeableAnalysisDto> selectPurgeableAnalysesWithEvents(@Param("componentUuid") String componentUuid);

  List<PurgeableAnalysisDto> selectPurgeableAnalysesWithoutEvents(@Param("componentUuid") String componentUuid);

  void deleteComponentIssueChanges(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentIssues(@Param("componentUuids") List<String> componentUuids);

  List<String> selectOldClosedIssueKeys(@Param("projectUuid") String projectUuid, @Nullable @Param("toDate") Long toDate);

  void deleteIssuesFromKeys(@Param("keys") List<String> keys);

  void deleteIssueChangesFromIssueKeys(@Param("issueKeys") List<String> issueKeys);

  void deleteFileSourcesByProjectUuid(String rootProjectUuid);

  void deleteFileSourcesByUuid(@Param("fileUuids") List<String> fileUuids);

  void deleteCeActivityByProjectUuid(String projectUuid);
}
