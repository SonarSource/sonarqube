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

  List<IdUuidPair> selectSnapshotIdAndUuidsByResource(@Param("componentUuids") List<String> componentUuids);

  /**
   * Returns the list of components of a project from a project_uuid. The project itself is also returned.
   */
  List<IdUuidPair> selectComponentsByProjectUuid(String projectUuid);

  void deleteSnapshot(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDuplications(@Param("analysisUuids") List<String> analysisUuids);

  void deleteSnapshotEvents(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotMeasures(@Param("snapshotIds") List<Long> snapshotIds);

  List<Long> selectMetricIdsWithoutHistoricalData();

  void deleteSnapshotWastedMeasures(@Param("snapshotIds") List<Long> snapshotIds, @Param("mids") List<Long> metricIds);

  void updatePurgeStatusToOne(long snapshotId);

  void disableResource(long resourceId);

  void resolveResourceIssuesNotAlreadyResolved(@Param("componentUuid") String componentUuid, @Param("dateAsLong") Long dateAsLong);

  void deleteResourceIndex(@Param("componentUuids") List<String> componentUuids);

  void deleteEvent(long eventId);

  void setSnapshotIsLastToFalse(@Param("componentUuid") String componentUuid);

  void deleteResourceLinks(@Param("componentUuids") List<String> componentUuids);

  void deleteResourceProperties(@Param("resourceIds") List<Long> resourceIds);

  void deleteResource(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceGroupRoles(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceUserRoles(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceManualMeasures(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentEvents(@Param("componentUuids") List<String> componentUuids);

  void deleteAuthors(@Param("resourceIds") List<Long> resourceIds);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithEvents(@Param("componentUuid") String componentUuid);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithoutEvents(@Param("componentUuid") String componentUuid);

  void deleteComponentIssueChanges(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentIssues(@Param("componentUuids") List<String> componentUuids);

  List<String> selectOldClosedIssueKeys(@Param("projectUuid") String projectUuid, @Nullable @Param("toDate") Long toDate);

  void deleteIssuesFromKeys(@Param("keys") List<String> keys);

  void deleteIssueChangesFromIssueKeys(@Param("issueKeys") List<String> issueKeys);

  void deleteFileSourcesByProjectUuid(String rootProjectUuid);

  void deleteFileSourcesByUuid(String fileUuid);

  void deleteCeActivityByProjectUuid(String projectUuid);

}
