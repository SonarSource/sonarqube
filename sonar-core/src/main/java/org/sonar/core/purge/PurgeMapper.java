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

import java.util.Date;
import java.util.List;

public interface PurgeMapper {

  List<Long> selectSnapshotIds(PurgeSnapshotQuery query);

  List<Long> selectSnapshotIdsByResource(@Param("resourceIds") List<Long> resourceIds);

  List<Long> selectProjectIdsByRootId(long rootResourceId);

  void deleteSnapshot(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDependenciesFromSnapshotId(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDependenciesToSnapshotId(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDependenciesProjectSnapshotId(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotDuplications(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotEvents(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotMeasures(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotMeasureData(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotSource(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotGraphs(@Param("snapshotIds") List<Long> snapshotIds);

  void deleteSnapshotData(@Param("snapshotIds") List<Long> snapshotIds);

  List<Long> selectMetricIdsWithoutHistoricalData();

  List<Long> selectCharacteristicIdsToPurge();

  void deleteSnapshotWastedMeasures(@Param("snapshotIds") List<Long> snapshotIds, @Param("mids") List<Long> metricIds);

  void deleteSnapshotMeasuresOnCharacteristics(@Param("snapshotIds") List<Long> snapshotIds, @Param("cids") List<Long> characteristicIds);

  void updatePurgeStatusToOne(long snapshotId);

  void disableResource(long resourceId);

  void deleteResourceIndex(@Param("resourceIds") List<Long> resourceIds);

  void deleteEvent(long eventId);

  void setSnapshotIsLastToFalse(long resourceId);

  void deleteResourceLinks(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceProperties(@Param("resourceIds") List<Long> resourceIds);

  void deleteResource(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceGroupRoles(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceUserRoles(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceManualMeasures(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceEvents(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceActionPlans(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceGraphs(@Param("resourceIds") List<Long> resourceIds);

  void deleteAuthors(@Param("resourceIds") List<Long> resourceIds);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithEvents(long resourceId);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithoutEvents(long resourceId);

  List<Long> selectResourceIdsByRootId(long rootProjectId);

  void deleteResourceIssueChanges(@Param("resourceIds") List<Long> resourceIds);

  void deleteResourceIssues(@Param("resourceIds") List<Long> resourceIds);

  void deleteOldClosedIssueChanges(@Param("rootProjectId") long rootProjectId, @Nullable @Param("toDate") Date toDate);

  void deleteOldClosedIssues(@Param("rootProjectId") long rootProjectId, @Nullable @Param("toDate") Date toDate);
}
