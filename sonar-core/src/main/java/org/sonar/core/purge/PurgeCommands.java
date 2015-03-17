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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

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

  List<Long> selectSnapshotIds(PurgeSnapshotQuery query) {
    return purgeMapper.selectSnapshotIds(query);
  }

  void deleteResources(List<IdUuidPair> componentIdUuids) {
    List<List<Long>> componentIdPartitions = Lists.partition(IdUuidPairs.ids(componentIdUuids), MAX_RESOURCES_PER_QUERY);
    List<List<String>> componentUuidsPartitions = Lists.partition(IdUuidPairs.uuids(componentIdUuids), MAX_RESOURCES_PER_QUERY);
    // Note : do not merge the delete statements into a single loop of resource ids. It's
    // voluntarily grouped by tables in order to benefit from JDBC batch mode.
    // Batch requests can only relate to the same PreparedStatement.

    for (List<Long> partResourceIds : componentIdPartitions) {
      deleteSnapshots(purgeMapper.selectSnapshotIdsByResource(partResourceIds));
    }

    // possible missing optimization: filter requests according to resource scope

    profiler.start("deleteResourceLinks (project_links)");
    for (List<String> componentUuidPartition : componentUuidsPartitions) {
      purgeMapper.deleteResourceLinks(componentUuidPartition);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceProperties (properties)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceProperties(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceIndex (resource_index)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceIndex(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceGroupRoles (group_roles)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceGroupRoles(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceUserRoles (user_roles)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceUserRoles(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceManualMeasures (manual_measures)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceManualMeasures(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteComponentIssueChanges (issue_changes)");
    for (List<String> componentUuidPartition : componentUuidsPartitions) {
      purgeMapper.deleteComponentIssueChanges(componentUuidPartition);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteComponentIssues (issues)");
    for (List<String> componentUuidPartition : componentUuidsPartitions) {
      purgeMapper.deleteComponentIssues(componentUuidPartition);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceActionPlans (action_plans)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceActionPlans(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteComponentEvents (events)");
    for (List<String> componentUuidPartition : componentUuidsPartitions) {
      purgeMapper.deleteComponentEvents(componentUuidPartition);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceGraphs (graphs)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResourceGraphs(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResource (projects)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteResource(partResourceIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteAuthors (authors)");
    for (List<Long> partResourceIds : componentIdPartitions) {
      purgeMapper.deleteAuthors(partResourceIds);
    }
    session.commit();
    profiler.stop();
  }

  void deleteSnapshots(final PurgeSnapshotQuery query) {
    deleteSnapshots(purgeMapper.selectSnapshotIds(query));
  }

  @VisibleForTesting
  protected void deleteSnapshots(final List<Long> snapshotIds) {

    List<List<Long>> snapshotIdsPartition = Lists.partition(snapshotIds, MAX_SNAPSHOTS_PER_QUERY);

    deleteSnapshotDependencies(snapshotIdsPartition);

    deleteSnapshotDuplications(snapshotIdsPartition);

    profiler.start("deleteSnapshotEvents (events)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshotEvents(partSnapshotIds);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteSnapshotMeasures (project_measures)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshotMeasures(partSnapshotIds);
    }
    session.commit();
    profiler.stop();

    deleteSnapshotGraphs(snapshotIdsPartition);

    profiler.start("deleteSnapshot (snapshots)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshot(partSnapshotIds);
    }
    session.commit();
    profiler.stop();
  }

  void purgeSnapshots(final PurgeSnapshotQuery query) {
    purgeSnapshots(purgeMapper.selectSnapshotIds(query));
  }

  @VisibleForTesting
  protected void purgeSnapshots(final List<Long> snapshotIds) {
    // note that events are not deleted
    List<List<Long>> snapshotIdsPartition = Lists.partition(snapshotIds, MAX_SNAPSHOTS_PER_QUERY);

    deleteSnapshotDependencies(snapshotIdsPartition);

    deleteSnapshotDuplications(snapshotIdsPartition);

    deleteSnapshotGraphs(snapshotIdsPartition);

    profiler.start("deleteSnapshotWastedMeasures (project_measures)");
    List<Long> metricIdsWithoutHistoricalData = purgeMapper.selectMetricIdsWithoutHistoricalData();
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshotWastedMeasures(partSnapshotIds, metricIdsWithoutHistoricalData);
    }
    session.commit();
    profiler.stop();

    profiler.start("updatePurgeStatusToOne (snapshots)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.updatePurgeStatusToOne(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotGraphs(final List<List<Long>> snapshotIdsPartition) {
    profiler.start("deleteSnapshotGraphs (graphs)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshotGraphs(partSnapshotIds);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotDuplications(final List<List<Long>> snapshotIdsPartition) {
    profiler.start("deleteSnapshotDuplications (duplications_index)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshotDuplications(partSnapshotIds);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotDependencies(final List<List<Long>> snapshotIdsPartition) {
    profiler.start("deleteSnapshotDependencies (dependencies)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      // SONAR-4586
      // On MsSQL, the maximum number of parameters allowed in a query is 2000, so we have to execute 3 queries instead of one with 3 or
      // inside
      purgeMapper.deleteSnapshotDependenciesFromSnapshotId(partSnapshotIds);
      purgeMapper.deleteSnapshotDependenciesToSnapshotId(partSnapshotIds);
      purgeMapper.deleteSnapshotDependenciesProjectSnapshotId(partSnapshotIds);
    }
    session.commit();
    profiler.stop();
  }

  public void deleteFileSources(String rootUuid) {
    profiler.start("deleteFileSources (file_sources)");
    purgeMapper.deleteFileSourcesByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }
}
