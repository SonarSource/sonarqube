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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.db.DbSession;

class PurgeCommands {

  private static final int MAX_SNAPSHOTS_PER_QUERY = 1000;
  private static final int MAX_RESOURCES_PER_QUERY = 1000;

  private final DbSession session;
  private final PurgeMapper purgeMapper;
  private final PurgeProfiler profiler;

  PurgeCommands(DbSession session, PurgeMapper purgeMapper, PurgeProfiler profiler) {
    this.session = session;
    this.purgeMapper = purgeMapper;
    this.profiler = profiler;
  }

  @VisibleForTesting
  PurgeCommands(DbSession session, PurgeProfiler profiler) {
    this(session, session.getMapper(PurgeMapper.class), profiler);
  }

  List<String> selectSnapshotUuids(PurgeSnapshotQuery query) {
    return purgeMapper.selectAnalysisIdsAndUuids(query).stream().map(IdUuidPair::getUuid).collect(Collectors.toList());
  }

  List<IdUuidPair> selectSnapshotIdUuids(PurgeSnapshotQuery query) {
    return purgeMapper.selectAnalysisIdsAndUuids(query);
  }

  void deleteAnalyses(String rootComponentUuid) {
    profiler.start("deleteAnalyses (events)");
    purgeMapper.deleteEventsByComponentUuid(rootComponentUuid);
    session.commit();
    profiler.stop();

    List<List<String>> analysisUuidsPartitions = Lists.partition(IdUuidPairs.uuids(
      purgeMapper.selectAnalysisIdsAndUuids(new PurgeSnapshotQuery().setComponentUuid(rootComponentUuid))), MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("deleteAnalyses (project_measures)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisMeasures);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (snapshots)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalyses);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (analysis_properties)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisProperties);
    session.commit();
    profiler.stop();
  }

  void deleteAnalyses(PurgeSnapshotQuery... queries) {
    List<IdUuidPair> snapshotIds = Arrays.stream(queries)
      .flatMap(q -> purgeMapper.selectAnalysisIdsAndUuids(q).stream())
      .collect(Collectors.toList());

    deleteAnalyses(snapshotIds);
  }

  @VisibleForTesting
  void deleteAnalyses(List<IdUuidPair> analysisIdUuids) {
    List<List<String>> analysisUuidsPartitions = Lists.partition(IdUuidPairs.uuids(analysisIdUuids), MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("deleteAnalyses (events)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisEvents);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (project_measures)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisMeasures);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (snapshots)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalyses);
    session.commit();
    profiler.stop();

    profiler.start("deleteAnalyses (analysis_properties)");
    analysisUuidsPartitions.forEach(purgeMapper::deleteAnalysisProperties);
    session.commit();
    profiler.stop();
  }

  void purgeAnalyses(List<IdUuidPair> analysisUuids) {
    List<List<String>> analysisUuidsPartitions = Lists.partition(IdUuidPairs.uuids(analysisUuids), MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("deleteSnapshotWastedMeasures (project_measures)");
    List<Long> metricIdsWithoutHistoricalData = purgeMapper.selectMetricIdsWithoutHistoricalData();
    if (!metricIdsWithoutHistoricalData.isEmpty()) {
      analysisUuidsPartitions
        .forEach(analysisUuidsPartition -> purgeMapper.deleteAnalysisWastedMeasures(analysisUuidsPartition, metricIdsWithoutHistoricalData));
      session.commit();
    }
    profiler.stop();

    profiler.start("updatePurgeStatusToOne (snapshots)");
    analysisUuidsPartitions.forEach(purgeMapper::updatePurgeStatusToOne);
    session.commit();
    profiler.stop();
  }

  private void deleteAnalysisDuplications(List<List<String>> snapshotUuidsPartitions) {
    profiler.start("deleteAnalysisDuplications (duplications_index)");
    snapshotUuidsPartitions.forEach(purgeMapper::deleteAnalysisDuplications);
    session.commit();
    profiler.stop();
  }

  void deletePermissions(long rootId) {
    profiler.start("deletePermissions (group_roles)");
    purgeMapper.deleteGroupRolesByComponentId(rootId);
    session.commit();
    profiler.stop();

    profiler.start("deletePermissions (user_roles)");
    purgeMapper.deleteUserRolesByComponentId(rootId);
    session.commit();
    profiler.stop();
  }

  void deleteIssues(String rootUuid) {
    profiler.start("deleteIssues (issue_changes)");
    purgeMapper.deleteIssueChangesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();

    profiler.start("deleteIssues (issues)");
    purgeMapper.deleteIssuesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteLinks(String rootUuid) {
    profiler.start("deleteLinks (project_links)");
    purgeMapper.deleteProjectLinksByComponentUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteByRootAndModulesOrSubviews(List<IdUuidPair> rootAndModulesOrSubviewsIds) {
    if (rootAndModulesOrSubviewsIds.isEmpty()) {
      return;
    }
    List<List<Long>> idPartitions = Lists.partition(IdUuidPairs.ids(rootAndModulesOrSubviewsIds), MAX_RESOURCES_PER_QUERY);
    List<List<String>> uuidsPartitions = Lists.partition(IdUuidPairs.uuids(rootAndModulesOrSubviewsIds), MAX_RESOURCES_PER_QUERY);

    profiler.start("deleteByRootAndModulesOrSubviews (properties)");
    idPartitions.forEach(purgeMapper::deletePropertiesByComponentIds);
    session.commit();
    profiler.stop();

    profiler.start("deleteByRootAndModulesOrSubviews (manual_measures)");
    uuidsPartitions.forEach(purgeMapper::deleteManualMeasuresByComponentUuids);
    session.commit();
    profiler.stop();
  }

  void deleteComponents(String rootUuid) {
    profiler.start("deleteComponents (projects)");
    purgeMapper.deleteComponentsByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteComponents(List<String> componentUuids) {
    if (componentUuids.isEmpty()) {
      return;
    }

    profiler.start("deleteComponents (projects)");
    Lists.partition(componentUuids, MAX_RESOURCES_PER_QUERY).forEach(purgeMapper::deleteComponentsByUuids);
    session.commit();
    profiler.stop();
  }

  void deleteComponentMeasures(List<String> componentUuids) {
    if (componentUuids.isEmpty()) {
      return;
    }

    profiler.start("deleteComponentMeasures (project_measures)");
    Lists.partition(componentUuids, MAX_RESOURCES_PER_QUERY).forEach(purgeMapper::fullDeleteComponentMeasures);
    session.commit();
    profiler.stop();
  }

  void deleteComponentMeasures(List<String> analysisUuids, List<String> componentUuids) {
    if (analysisUuids.isEmpty() || componentUuids.isEmpty()) {
      return;
    }

    List<List<String>> analysisUuidsPartitions = Lists.partition(analysisUuids, MAX_SNAPSHOTS_PER_QUERY);
    List<List<String>> componentUuidsPartitions = Lists.partition(componentUuids, MAX_RESOURCES_PER_QUERY);

    profiler.start("deleteComponentMeasures");
    for (List<String> analysisUuidsPartition : analysisUuidsPartitions) {
      for (List<String> componentUuidsPartition : componentUuidsPartitions) {
        purgeMapper.deleteComponentMeasures(analysisUuidsPartition, componentUuidsPartition);
      }
    }
    session.commit();
    profiler.stop();
  }

  void deleteFileSources(String rootUuid) {
    profiler.start("deleteFileSources (file_sources)");
    purgeMapper.deleteFileSourcesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteCeActivity(String rootUuid) {
    profiler.start("deleteCeActivity (ce_scanner_context)");
    purgeMapper.deleteCeScannerContextOfCeActivityByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_task_characteristics)");
    purgeMapper.deleteCeTaskCharacteristicsOfCeActivityByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_task_input)");
    purgeMapper.deleteCeTaskInputOfCeActivityByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeActivity (ce_activity)");
    purgeMapper.deleteCeActivityByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteCeQueue(String rootUuid) {
    profiler.start("deleteCeQueue (ce_scanner_context)");
    purgeMapper.deleteCeScannerContextOfCeQueueByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_task_characteristics)");
    purgeMapper.deleteCeTaskCharacteristicsOfCeQueueByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_task_input)");
    purgeMapper.deleteCeTaskInputOfCeQueueByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
    profiler.start("deleteCeQueue (ce_queue)");
    purgeMapper.deleteCeQueueByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteWebhookDeliveries(String rootUuid) {
    profiler.start("deleteWebhookDeliveries (webhook_deliveries)");
    purgeMapper.deleteWebhookDeliveriesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteBranch(String rootUuid) {
    profiler.start("deleteBranch (project_branches)");
    purgeMapper.deleteBranchByUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  void deleteLiveMeasures(String rootUuid) {
    profiler.start("deleteLiveMeasures (live_measures)");
    purgeMapper.deleteLiveMeasuresByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }
}
