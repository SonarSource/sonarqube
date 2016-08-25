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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.session.SqlSession;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

class PurgeCommands {

  private static final int MAX_SNAPSHOTS_PER_QUERY = 1000;
  private static final int MAX_RESOURCES_PER_QUERY = 1000;

  private final SqlSession session;
  private final PurgeMapper purgeMapper;
  private final PurgeProfiler profiler;

  PurgeCommands(SqlSession session, PurgeMapper purgeMapper, PurgeProfiler profiler) {
    this.session = session;
    this.purgeMapper = purgeMapper;
    this.profiler = profiler;
  }

  @VisibleForTesting
  PurgeCommands(SqlSession session, PurgeProfiler profiler) {
    this(session, session.getMapper(PurgeMapper.class), profiler);
  }

  List<String> selectSnapshotUuids(PurgeSnapshotQuery query) {
    return purgeMapper.selectAnalysisIdsAndUuids(query).stream().map(IdUuidPair::getUuid).collect(Collectors.toList());
  }

  List<IdUuidPair> selectSnapshotIdUuids(PurgeSnapshotQuery query) {
    return purgeMapper.selectAnalysisIdsAndUuids(query);
  }

  void deleteAnalyses(String rootUuid) {
    deleteAnalyses(purgeMapper.selectAnalysisIdsAndUuids(new PurgeSnapshotQuery().setComponentUuid(rootUuid)));
  }

  void deleteComponents(List<IdUuidPair> componentIdUuids) {
    List<List<Long>> componentIdPartitions = Lists.partition(IdUuidPairs.ids(componentIdUuids), MAX_RESOURCES_PER_QUERY);
    List<List<String>> componentUuidsPartitions = Lists.partition(IdUuidPairs.uuids(componentIdUuids), MAX_RESOURCES_PER_QUERY);
    // Note : do not merge the delete statements into a single loop of resource ids. It's
    // voluntarily grouped by tables in order to benefit from JDBC batch mode.
    // Batch requests can only relate to the same PreparedStatement.

    // possible missing optimization: filter requests according to resource scope

    profiler.start("deleteResourceLinks (project_links)");
    componentUuidsPartitions.forEach(purgeMapper::deleteComponentLinks);
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceProperties (properties)");
    componentIdPartitions.forEach(purgeMapper::deleteComponentProperties);
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceIndex (resource_index)");
    componentUuidsPartitions.forEach(purgeMapper::deleteResourceIndex);
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceGroupRoles (group_roles)");
    componentIdPartitions.forEach(purgeMapper::deleteComponentGroupRoles);
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceUserRoles (user_roles)");
    componentIdPartitions.forEach(purgeMapper::deleteComponentUserRoles);
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceManualMeasures (manual_measures)");
    componentUuidsPartitions.forEach(purgeMapper::deleteComponentManualMeasures);
    session.commit();
    profiler.stop();

    profiler.start("deleteComponentIssueChanges (issue_changes)");
    componentUuidsPartitions.forEach(purgeMapper::deleteComponentIssueChanges);
    session.commit();
    profiler.stop();

    profiler.start("deleteComponentIssues (issues)");
    componentUuidsPartitions.forEach(purgeMapper::deleteComponentIssues);
    session.commit();
    profiler.stop();

    profiler.start("deleteComponentEvents (events)");
    componentUuidsPartitions.forEach(purgeMapper::deleteComponentEvents);
    session.commit();
    profiler.stop();

    profiler.start("deleteResource (projects)");
    componentUuidsPartitions.forEach(purgeMapper::deleteComponents);
    session.commit();
    profiler.stop();

    profiler.start("deleteAuthors (authors)");
    componentIdPartitions.forEach(purgeMapper::deleteAuthors);
    session.commit();
    profiler.stop();
  }

  public void deleteComponentMeasures(List<String> analysisUuids, List<String> componentUuids) {
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

  void deleteAnalyses(PurgeSnapshotQuery... queries) {
    List<IdUuidPair> snapshotIds = from(asList(queries))
      .transformAndConcat(purgeMapper::selectAnalysisIdsAndUuids)
      .toList();
    deleteAnalyses(snapshotIds);
  }

  @VisibleForTesting
  protected void deleteAnalyses(List<IdUuidPair> analysisIdUuids) {
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
  }

  public void purgeAnalyses(List<IdUuidPair> analysisUuids) {
    List<List<String>> analysisUuidsPartitions = Lists.partition(IdUuidPairs.uuids(analysisUuids), MAX_SNAPSHOTS_PER_QUERY);

    deleteAnalysisDuplications(analysisUuidsPartitions);

    profiler.start("deleteSnapshotWastedMeasures (project_measures)");
    List<Long> metricIdsWithoutHistoricalData = purgeMapper.selectMetricIdsWithoutHistoricalData();
    analysisUuidsPartitions.stream()
        .forEach(analysisUuidsPartition -> purgeMapper.deleteAnalysisWastedMeasures(analysisUuidsPartition, metricIdsWithoutHistoricalData));
    session.commit();
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

  public void deleteFileSources(String rootUuid) {
    profiler.start("deleteFileSources (file_sources)");
    purgeMapper.deleteFileSourcesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }

  public void deleteCeActivity(String rootUuid) {
    profiler.start("deleteCeActivity (ce_activity)");
    purgeMapper.deleteCeActivityByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }
}
