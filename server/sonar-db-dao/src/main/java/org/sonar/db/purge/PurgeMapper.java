/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface PurgeMapper {

  List<IdUuidPair> selectAnalysisIdsAndUuids(PurgeSnapshotQuery query);

  /**
   * Returns the list of modules/subviews and the application/view/project for the specified project_uuid.
   */
  List<IdUuidPair> selectRootAndModulesOrSubviewsByProjectUuid(@Param("rootUuid") String rootUuid);

  Set<String> selectDisabledComponentsWithFileSource(@Param("projectUuid") String projectUuid);

  Set<String> selectDisabledComponentsWithUnresolvedIssues(@Param("projectUuid") String projectUuid);

  Set<String> selectDisabledComponentsWithLiveMeasures(@Param("projectUuid") String projectUuid);

  void deleteAnalyses(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisProperties(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisDuplications(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisEvents(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisEventComponentChanges(@Param("analysisUuids") List<String> analysisUuids);

  void deleteAnalysisMeasures(@Param("analysisUuids") List<String> analysisUuids);

  void fullDeleteComponentMeasures(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentMeasures(@Param("analysisUuids") List<String> analysisUuids, @Param("componentUuids") List<String> componentUuids);

  List<Long> selectMetricIdsWithoutHistoricalData();

  void deleteAnalysisWastedMeasures(@Param("analysisUuids") List<String> analysisUuids, @Param("metricIds") List<Long> metricIds);

  /**
   * Purge status flag is used to not attempt to remove duplications & historical data of analyses
   * for which we already removed them.
   */
  void updatePurgeStatusToOne(@Param("analysisUuids") List<String> analysisUuid);

  void resolveComponentIssuesNotAlreadyResolved(@Param("componentUuids") List<String> componentUuids, @Param("dateAsLong") Long dateAsLong);

  void deleteProjectLinksByProjectUuid(@Param("rootUuid") String rootUuid);

  void deletePropertiesByComponentIds(@Param("componentIds") List<Long> componentIds);

  void deleteComponentsByProjectUuid(@Param("rootUuid") String rootUuid);

  void deleteComponentsByUuids(@Param("componentUuids") List<String> componentUuids);

  void deleteGroupRolesByComponentId(@Param("rootId") long rootId);

  void deleteUserRolesByComponentId(@Param("rootId") long rootId);

  void deleteManualMeasuresByComponentUuids(@Param("componentUuids") List<String> componentUuids);

  void deleteEventsByComponentUuid(@Param("componentUuid") String componentUuid);

  void deleteEventComponentChangesByComponentUuid(@Param("componentUuid") String componentUuid);

  List<PurgeableAnalysisDto> selectPurgeableAnalysesWithEvents(@Param("componentUuid") String componentUuid);

  List<PurgeableAnalysisDto> selectPurgeableAnalysesWithoutEvents(@Param("componentUuid") String componentUuid);

  void deleteIssueChangesByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteIssuesByProjectUuid(@Param("projectUuid") String projectUuid);

  List<String> selectOldClosedIssueKeys(@Param("projectUuid") String projectUuid, @Nullable @Param("toDate") Long toDate);

  List<String> selectStaleShortLivingBranchesAndPullRequests(@Param("projectUuid") String projectUuid, @Param("toDate") Long toDate);

  @CheckForNull
  String selectManualBaseline(@Param("projectUuid") String projectUuid);

  List<IdUuidPair> selectDisabledComponentsWithoutIssues(@Param("projectUuid") String projectUuid);

  void deleteIssuesFromKeys(@Param("keys") List<String> keys);

  void deleteIssueChangesFromIssueKeys(@Param("issueKeys") List<String> issueKeys);

  void deleteFileSourcesByProjectUuid(String rootProjectUuid);

  void deleteFileSourcesByFileUuid(@Param("fileUuids") List<String> fileUuids);

  void deleteCeTaskCharacteristicsOfCeActivityByRootUuidOrBefore(@Nullable @Param("rootUuid") String rootUuid,
    @Nullable @Param("createdAtBefore") Long createdAtBefore);

  void deleteCeTaskInputOfCeActivityByRootUuidOrBefore(@Nullable @Param("rootUuid") String rootUuid,
    @Nullable @Param("createdAtBefore") Long createdAtBefore);

  void deleteCeScannerContextOfCeActivityByRootUuidOrBefore(@Nullable @Param("rootUuid") String rootUuid,
    @Nullable @Param("createdAtBefore") Long createdAtBefore);

  void deleteCeTaskMessageOfCeActivityByRootUuidOrBefore(@Nullable @Param("rootUuid") String rootUuid,
    @Nullable @Param("createdAtBefore") Long createdAtBefore);

  /**
   * Delete rows in CE_ACTIVITY of tasks of the specified component and/or created before specified date.
   */
  void deleteCeActivityByRootUuidOrBefore(@Nullable @Param("rootUuid") String rootUuid,
    @Nullable @Param("createdAtBefore") Long createdAtBefore);

  void deleteCeScannerContextOfCeQueueByRootUuid(@Param("rootUuid") String rootUuid);

  void deleteCeTaskCharacteristicsOfCeQueueByRootUuid(@Param("rootUuid") String rootUuid);

  void deleteCeTaskInputOfCeQueueByRootUuid(@Param("rootUuid") String rootUuid);

  void deleteCeTaskMessageOfCeQueueByRootUuid(@Param("rootUuid") String rootUuid);

  void deleteCeQueueByRootUuid(@Param("rootUuid") String rootUuid);

  void deleteWebhooksByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteWebhookDeliveriesByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteProjectMappingsByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteProjectAlmBindingsByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteBranchByUuid(@Param("uuid") String uuid);

  void deleteLiveMeasuresByProjectUuid(@Param("projectUuid") String projectUuid);

  void deleteLiveMeasuresByComponentUuids(@Param("componentUuids") List<String> componentUuids);
}
