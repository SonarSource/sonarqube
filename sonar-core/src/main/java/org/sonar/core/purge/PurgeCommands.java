/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

class PurgeCommands {
  private static final int MAX_CHARACTERISTICS_PER_QUERY = 1000;

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

  void deleteResources(List<Long> resourceIds) {
    // Note : do not merge the delete statements into a single loop of resource ids. It's
    // voluntarily grouped by tables in order to benefit from JDBC batch mode.
    // Batch requests can only relate to the same PreparedStatement.

    for (Long resourceId : resourceIds) {
      deleteSnapshots(PurgeSnapshotQuery.create().setResourceId(resourceId));
    }

    // possible missing optimization: filter requests according to resource scope

    profiler.start("deleteResourceLinks (project_links)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceLinks(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceProperties (properties)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceProperties(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceIndex (resource_index)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceIndex(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceGroupRoles (group_roles)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceGroupRoles(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceUserRoles (user_roles)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceUserRoles(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceManualMeasures (manual_measures)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceManualMeasures(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceReviewComments (review_comments)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceReviewComments(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceActionPlansReviews (action_plans_reviews)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceActionPlansReviews(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceReviews (reviews)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceReviews(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceActionPlans (action_plans)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceActionPlans(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceEvents (events)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceEvents(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResourceGraphs (graphs)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceGraphs(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteResource (projects)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResource(resourceId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteAuthors (authors)");
    for (Long resourceId : resourceIds) {
      purgeMapper.deleteAuthors(resourceId);
    }
    session.commit();
    profiler.stop();
  }

  void deleteSnapshots(final PurgeSnapshotQuery query) {
    deleteSnapshots(purgeMapper.selectSnapshotIds(query));
  }

  private void deleteSnapshots(final List<Long> snapshotIds) {

    deleteSnapshotDependencies(snapshotIds);

    deleteSnapshotDuplications(snapshotIds);

    profiler.start("deleteSnapshotEvents (events)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotEvents(snapshotId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteSnapshotMeasureData (measure_data)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotMeasureData(snapshotId);
    }
    session.commit();
    profiler.stop();

    profiler.start("deleteSnapshotMeasures (project_measures)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotMeasures(snapshotId);
    }
    session.commit();
    profiler.stop();

    deleteSnapshotSources(snapshotIds);

    deleteSnapshotViolations(snapshotIds);

    deleteSnapshotGraphs(snapshotIds);

    deleteSnapshotData(snapshotIds);

    profiler.start("deleteSnapshot (snapshots)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshot(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  void purgeSnapshots(final PurgeSnapshotQuery query) {
    purgeSnapshots(purgeMapper.selectSnapshotIds(query));
  }

  private void purgeSnapshots(final List<Long> snapshotIds) {
    // note that events are not deleted

    deleteSnapshotDependencies(snapshotIds);

    deleteSnapshotDuplications(snapshotIds);

    deleteSnapshotSources(snapshotIds);

    deleteSnapshotViolations(snapshotIds);

    deleteSnapshotGraphs(snapshotIds);

    deleteSnapshotData(snapshotIds);

    profiler.start("deleteSnapshotWastedMeasures (project_measures)");
    List<Long> metricIdsWithoutHistoricalData = purgeMapper.selectMetricIdsWithoutHistoricalData();
    if (!metricIdsWithoutHistoricalData.isEmpty()) {
      for (Long snapshotId : snapshotIds) {
        purgeMapper.deleteSnapshotWastedMeasures(snapshotId, metricIdsWithoutHistoricalData);
      }
      session.commit();
    }
    profiler.stop();

    profiler.start("deleteSnapshotMeasuresOnCharacteristics (project_measures)");
    List<Long> characteristicIds = purgeMapper.selectCharacteristicIdsToPurge();
    if (!characteristicIds.isEmpty()) {
      for (Long snapshotId : snapshotIds) {
        // SONAR-3641 We cannot process all characteristics at once
        for (List<Long> ids : Iterables.partition(characteristicIds, MAX_CHARACTERISTICS_PER_QUERY)) {
          purgeMapper.deleteSnapshotMeasuresOnCharacteristics(snapshotId, ids);
        }
      }
      session.commit();
    }
    profiler.stop();

    profiler.start("updatePurgeStatusToOne (snapshots)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.updatePurgeStatusToOne(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotData(final List<Long> snapshotIds) {
    profiler.start("deleteSnapshotData (snapshot_data)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotData(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotGraphs(final List<Long> snapshotIds) {
    profiler.start("deleteSnapshotGraphs (graphs)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotGraphs(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotViolations(final List<Long> snapshotIds) {
    profiler.start("deleteSnapshotViolations (rule_failures)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotViolations(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotSources(final List<Long> snapshotIds) {
    profiler.start("deleteSnapshotSource (snapshot_sources)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotSource(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotDuplications(final List<Long> snapshotIds) {
    profiler.start("deleteSnapshotDuplications (duplications_index)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotDuplications(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

  private void deleteSnapshotDependencies(final List<Long> snapshotIds) {
    profiler.start("deleteSnapshotDependencies (dependencies)");
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotDependencies(snapshotId);
    }
    session.commit();
    profiler.stop();
  }

}
