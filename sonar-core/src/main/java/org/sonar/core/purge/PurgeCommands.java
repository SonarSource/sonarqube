/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
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

  PurgeCommands(SqlSession session, PurgeMapper purgeMapper) {
    this.session = session;
    this.purgeMapper = purgeMapper;
  }

  @VisibleForTesting
  PurgeCommands(SqlSession session) {
    this(session, session.getMapper(PurgeMapper.class));
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

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceLinks(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceProperties(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceIndex(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceGroupRoles(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceUserRoles(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceManualMeasures(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceReviewComments(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceActionPlansReviews(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceReviews(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceActionPlans(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceEvents(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResourceGraphs(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteResource(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      purgeMapper.deleteAuthors(resourceId);
    }
    session.commit();
  }

  void deleteSnapshots(final PurgeSnapshotQuery query) {
    deleteSnapshots(purgeMapper.selectSnapshotIds(query));
  }

  private void deleteSnapshots(final List<Long> snapshotIds) {
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotDependencies(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotDuplications(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotEvents(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotMeasureData(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotMeasures(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotSource(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotViolations(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotGraphs(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshot(snapshotId);
    }

    session.commit();
  }

  void purgeSnapshots(final PurgeSnapshotQuery query) {
    purgeSnapshots(purgeMapper.selectSnapshotIds(query));
  }

  private void purgeSnapshots(final List<Long> snapshotIds) {
    // note that events are not deleted
    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotDependencies(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotDuplications(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotSource(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotViolations(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      purgeMapper.deleteSnapshotGraphs(snapshotId);
    }
    session.commit();

    List<Long> metricIdsWithoutHistoricalData = purgeMapper.selectMetricIdsWithoutHistoricalData();
    if (!metricIdsWithoutHistoricalData.isEmpty()) {
      for (Long snapshotId : snapshotIds) {
        purgeMapper.deleteSnapshotWastedMeasures(snapshotId, metricIdsWithoutHistoricalData);
      }
      session.commit();
    }

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

    for (Long snapshotId : snapshotIds) {
      purgeMapper.updatePurgeStatusToOne(snapshotId);
    }
    session.commit();
  }

}
