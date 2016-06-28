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

  List<IdUuidPair> selectSnapshotIdsAndUuids(PurgeSnapshotQuery query);

  /**
   * Returns the list of components of a project from a project_uuid. The project itself is also returned.
   */
  List<IdUuidPair> selectComponentsByProjectUuid(String projectUuid);

  void deleteAnalyses(@Param("analysisUuids") List<String> analysisUuids);

  void deleteDescendantSnapshots(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshot(@Param("snapshotUuids") List<String> snapshotUuids);

  void deleteSnapshotDuplications(@Param("analysisUuids") List<String> analysisUuids);

  void deleteSnapshotEvents(@Param("analysisUuids") List<String> analysisUuids);

  void deleteSnapshotMeasures(@Param("snapshotIds") List<Long> snapshotIds);

  List<Long> selectMetricIdsWithoutHistoricalData();

  void deleteSnapshotWastedMeasures(@Param("snapshotIds") List<Long> snapshotIds, @Param("mids") List<Long> metricIds);

  void updatePurgeStatusToOne(String snapshotUuid);

  void disableComponent(String componentUuid);

  void resolveComponentIssuesNotAlreadyResolved(@Param("componentUuid") String componentUuid, @Param("dateAsLong") Long dateAsLong);

  void deleteResourceIndex(@Param("componentUuids") List<String> componentUuids);

  void setSnapshotIsLastToFalse(@Param("componentUuid") String componentUuid);

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

  void deleteFileSourcesByUuid(String fileUuid);

  void deleteCeActivityByProjectUuid(String projectUuid);

}
