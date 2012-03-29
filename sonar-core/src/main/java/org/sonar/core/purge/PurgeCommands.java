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
import org.apache.ibatis.session.SqlSession;

import java.util.List;

final class PurgeCommands {
  private PurgeCommands() {
  }

  @VisibleForTesting
  static void deleteResources(List<Long> resourceIds, SqlSession session, PurgeMapper mapper, PurgeVendorMapper vendorMapper) {
    // Note : do not merge the delete statements into a single loop of resource ids. It's
    // voluntarily grouped by tables in order to benefit from JDBC batch mode.
    // Batch requests can only relate to the same PreparedStatement.

    for (Long resourceId : resourceIds) {
      deleteSnapshots(PurgeSnapshotQuery.create().setResourceId(resourceId), session, mapper);
    }

    // possible missing optimization: filter requests according to resource scope

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceLinks(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceProperties(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceIndex(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceGroupRoles(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceUserRoles(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceManualMeasures(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      vendorMapper.deleteResourceReviewComments(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      vendorMapper.deleteResourceActionPlansReviews(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceReviews(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceActionPlans(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResourceEvents(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteResource(resourceId);
    }
    session.commit();

    for (Long resourceId : resourceIds) {
      mapper.deleteAuthors(resourceId);
    }
    session.commit();
  }

  @VisibleForTesting
  static void deleteSnapshots(final PurgeSnapshotQuery query, final SqlSession session, final PurgeMapper mapper) {
    deleteSnapshots(mapper.selectSnapshotIds(query), session, mapper);
  }

  private static void deleteSnapshots(final List<Long> snapshotIds, final SqlSession session, final PurgeMapper mapper) {
    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotDependencies(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotDuplications(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotEvents(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotMeasureData(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotMeasures(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotSource(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotViolations(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshot(snapshotId);
    }

    session.commit();
  }

  @VisibleForTesting
  static void purgeSnapshots(final PurgeSnapshotQuery query, final SqlSession session, final PurgeMapper mapper) {
    purgeSnapshots(mapper.selectSnapshotIds(query), session, mapper);
  }

  private static void purgeSnapshots(final List<Long> snapshotIds, final SqlSession session, final PurgeMapper mapper) {
    // note that events are not deleted
    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotDependencies(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotDuplications(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotSource(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotViolations(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotWastedMeasures(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.deleteSnapshotMeasuresOnQualityModelRequirements(snapshotId);
    }
    session.commit();

    for (Long snapshotId : snapshotIds) {
      mapper.updatePurgeStatusToOne(snapshotId);
    }
    session.commit();
  }

}
