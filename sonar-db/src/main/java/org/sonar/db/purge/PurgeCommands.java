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
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.LinkedHashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.session.SqlSession;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

class PurgeCommands {

  private static final int MAX_SNAPSHOTS_PER_QUERY = 1000;
  private static final int MAX_RESOURCES_PER_QUERY = 1000;

  private final SqlSession session;
  private final PurgeMapper purgeMapper;
  private final PurgeProfiler profiler;
  private final Function<PurgeSnapshotQuery, Iterable<Long>> purgeSnapshotQueryToSnapshotIds = new Function<PurgeSnapshotQuery, Iterable<Long>>() {
    @Nullable
    @Override
    public Iterable<Long> apply(PurgeSnapshotQuery query) {
      return purgeMapper.selectSnapshotIds(query);
    }
  };

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

  void deleteComponents(List<IdUuidPair> componentIdUuids) {
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
    for (List<String> componentUuidPartition : componentUuidsPartitions) {
      purgeMapper.deleteResourceManualMeasures(componentUuidPartition);
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

    profiler.start("deleteComponentEvents (events)");
    for (List<String> componentUuidPartition : componentUuidsPartitions) {
      purgeMapper.deleteComponentEvents(componentUuidPartition);
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

  void deleteSnapshots(PurgeSnapshotQuery... queries) {
    List<Long> snapshotIds = from(asList(queries))
      .transformAndConcat(purgeSnapshotQueryToSnapshotIds).toList();
    deleteSnapshots(snapshotIds);
  }

  @VisibleForTesting
  protected void deleteSnapshots(final List<Long> snapshotIds) {

    List<List<Long>> snapshotIdsPartition = Lists.partition(snapshotIds, MAX_SNAPSHOTS_PER_QUERY);

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

    profiler.start("deleteSnapshot (snapshots)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshot(partSnapshotIds);
    }
    session.commit();
    profiler.stop();
  }

  void purgeSnapshots(PurgeSnapshotQuery... queries) {
    // use LinkedHashSet to keep order by remove duplicated ids
    LinkedHashSet<Long> snapshotIds = Sets.newLinkedHashSet(from(asList(queries)).transformAndConcat(purgeSnapshotQueryToSnapshotIds));
    purgeSnapshots(snapshotIds);
  }

  @VisibleForTesting
  protected void purgeSnapshots(Iterable<Long> snapshotIds) {
    // note that events are not deleted
    Iterable<List<Long>> snapshotIdsPartition = Iterables.partition(snapshotIds, MAX_SNAPSHOTS_PER_QUERY);

    deleteSnapshotDuplications(snapshotIdsPartition);

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

  private void deleteSnapshotDuplications(Iterable<List<Long>> snapshotIdsPartition) {
    profiler.start("deleteSnapshotDuplications (duplications_index)");
    for (List<Long> partSnapshotIds : snapshotIdsPartition) {
      purgeMapper.deleteSnapshotDuplications(partSnapshotIds);
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

  public void deleteCeActivity(String rootUuid) {
    profiler.start("deleteCeActivity (ce_activity)");
    purgeMapper.deleteCeActivityByProjectUuid(rootUuid);
    session.commit();
    profiler.stop();
  }
}
