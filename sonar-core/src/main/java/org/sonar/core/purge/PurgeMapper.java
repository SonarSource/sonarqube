/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.purge;

import org.apache.ibatis.annotations.Param;

import javax.annotation.Nullable;

import java.util.List;

public interface PurgeMapper {

  List<Long> selectSnapshotIds(PurgeSnapshotQuery query);

  List<Long> selectSnapshotIdsByResource(@Param("resourceIds") List<Long> resourceIds);

  List<IdUuidPair> selectProjectIdUuidsByRootId(long rootResourceId);

  List<IdUuidPair> selectComponentIdUuidsByRootId(long rootProjectId);

  void deleteSnapshot(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDependenciesFromSnapshotId(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDependenciesToSnapshotId(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDependenciesProjectSnapshotId(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDuplications(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotEvents(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotMeasures(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotGraphs(@Param("snapshotIds") List<Long> snapshotIds);

  List<Long> selectMetricIdsWithoutHistoricalData();

  void deleteSnapshotWastedMeasures(@Param("snapshotIds") List<Long> snapshotIds, @Param("mids") List<Long> metricIds);

  void updatePurgeStatusToOne(long snapshotId);

  void disableResource(long resourceId);

  void resolveResourceIssuesNotAlreadyResolved(@Param("componentUuid") String componentUuid, @Param("dateAsLong") Long dateAsLong);

  void deleteResourceIndex(@Param("resourceIds") List<Long> resourceIds);

  void deleteEvent(long eventId);

  void setSnapshotIsLastToFalse(long resourceId);

  void deleteResourceLinks(@Param("componentUuids") List<String> componentUuids);

  void deleteResourceProperties(@Param("resourceIds") List<Long> resourceIds);

  void deleteResource(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceGroupRoles(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceUserRoles(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceManualMeasures(@Param("resourceIds") List<Long> resourceIds);

  void deleteComponentEvents(@Param("componentUuids") List<String> componentUuids);

  void deleteResourceActionPlans(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceGraphs(@Param("resourceIds") List<Long> resourceIds);

  void deleteAuthors(@Param("resourceIds") List<Long> resourceIds);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithEvents(long resourceId);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithoutEvents(long resourceId);

  void deleteComponentIssueChanges(@Param("componentUuids") List<String> componentUuids);

  void deleteComponentIssues(@Param("componentUuids") List<String> componentUuids);

  void deleteOldClosedIssueChanges(@Param("projectUuid") String projectUuid, @Nullable @Param("toDate") Long toDate);

  void deleteOldClosedIssues(@Param("projectUuid") String projectUuid, @Nullable @Param("toDate") Long toDate);

  void deleteFileSourcesByProjectUuid(String rootProjectUuid);

  void deleteFileSourcesByUuid(String fileUuid);

  List<String> selectPurgeableFileUuids(Long projectId);
}
