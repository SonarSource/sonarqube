/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.purge;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface PurgeMapper {

  List<IdUuidPair> selectAnalysisIdsAndUuids(PurgeSnapshotQuery query);

  /**
   * Returns the list of modules/subviews and the application/view/project for the specified project_uuid.
   */
  List<IdUuidPair> selectRootAndModulesOrSubviewsByProjectUuid(@Param("rootUuid") String rootUuid);

  void deleteAnalyses(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisProperties(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisDuplications(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisEvents(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisMeasures(@Param("analysisUuids") List<String> analysisUuids);

  void fullDeleteComponentMeasures(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentMeasures(@Param("analysisUuids") List<String> analysisUuids, @Param("componentUuids") List<String> componentUuids);

  List<Long> selectMetricIdsWithoutHistoricalData();

  void deleteAnalysisWastedMeasures(@Param("analysisUuids") List<String> analysisUuids, @Param("metricIds") List<Long> metricIds);

  void updatePurgeStatusToOne(@Param("analysisUuids") List<String> analysisUuid);

  void resolveComponentIssuesNotAlreadyResolved(@Param("componentUuids") List<String> componentUuids, @Param("dateAsLong") Long dateAsLong);

  void deleteProjectLinksByComponentUuid(@Param("rootUuid") String rootUuid);

  void deletePropertiesByComponentIds(@Param("componentIds") List<Long> componentIds);

  void deleteComponentsByProjectUuid(@Param("rootUuid") String rootUuid);

  void deleteComponentsByUuids(@Param("componentUuids") List<String> componentUuids);

  void deleteGroupRolesByComponentId(@Param("rootId") long rootId);

  void deleteUserRolesByComponentId(@Param("rootId") long rootId);

  void deleteManualMeasuresByComponentUuids(@Param("componentUuids") List<String> componentUuids);

  void deleteEventsByComponentUuid(@Param("componentUuid") String componentUuid);

  List<PurgeableAnalysisDto> selectPurgeableAnalysesWithEvents(@Param("componentUuid") String componentUuid);

  List<PurgeableAnalysisDto> selectPurgeableAnalysesWithoutEvents(@Param("componentUuid") String componentUuid);

  void deleteIssueChangesByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteIssuesByProjectUuid(@Param("projectUuid") String projectUuid);

  List<String> selectOldClosedIssueKeys(@Param("projectUuid") String projectUuid, @Nullable @Param("toDate") Long toDate);

  List<String> selectStaleShortLivingBranches(@Param("mainBranchProjectUuid") String mainBranchProjectUuid, @Param("toDate") Long toDate);

  void deleteIssuesFromKeys(@Param("keys") List<String> keys);

  void deleteIssueChangesFromIssueKeys(@Param("issueKeys") List<String> issueKeys);

  void deleteFileSourcesByProjectUuid(String rootProjectUuid);

  void deleteFileSourcesByFileUuid(@Param("fileUuids") List<String> fileUuids);

  void deleteCeTaskCharacteristicsOfCeActivityByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeTaskInputOfCeActivityByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeScannerContextOfCeActivityByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeActivityByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeScannerContextOfCeQueueByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeTaskCharacteristicsOfCeQueueByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeTaskInputOfCeQueueByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteCeQueueByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteWebhookDeliveriesByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteBranchByUuid(@Param("uuid") String uuid);

  void deleteLiveMeasuresByProjectUuid(@Param("projectUuid") String projectUuid);
}
