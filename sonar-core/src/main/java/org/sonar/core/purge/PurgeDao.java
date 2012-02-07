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
import com.google.common.collect.Lists;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

import java.util.Collections;
import java.util.List;

public class PurgeDao {
  private final MyBatis mybatis;
  private final ResourceDao resourceDao;

  public PurgeDao(MyBatis mybatis, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
  }

  public PurgeDao purgeProject(long rootProjectId) {
    SqlSession session = mybatis.openBatchSession();
    PurgeMapper purgeMapper = session.getMapper(PurgeMapper.class);
    try {
      List<Long> projectIds = Lists.newArrayList(rootProjectId);
      projectIds.addAll(resourceDao.getDescendantProjectIds(rootProjectId, session));
      for (Long projectId : projectIds) {
        purgeProject(projectId, session, purgeMapper);
      }

      for (Long projectId : projectIds) {
        disableOrphanResources(projectId, session, purgeMapper);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }

  private void purgeProject(final Long projectId, final SqlSession session, final PurgeMapper purgeMapper) {
    List<Long> projectSnapshotIds = purgeMapper.selectSnapshotIds(PurgeSnapshotQuery.create().setResourceId(projectId).setIslast(false).setNotPurged(true));
    for (final Long projectSnapshotId : projectSnapshotIds) {
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create().setRootSnapshotId(projectSnapshotId).setNotPurged(true);
      session.select("org.sonar.core.purge.PurgeMapper.selectSnapshotIds", query, new ResultHandler() {
        public void handleResult(ResultContext resultContext) {
          Long snapshotId = (Long) resultContext.getResultObject();
          purgeSnapshot(snapshotId, purgeMapper);
        }
      });

      // must be executed at the end for reentrance
      purgeSnapshot(projectSnapshotId, purgeMapper);
    }
    session.commit();
  }

  private void disableOrphanResources(final Long projectId, final SqlSession session, final PurgeMapper purgeMapper) {
    session.select("org.sonar.core.purge.PurgeMapper.selectResourceIdsToDisable", projectId, new ResultHandler() {
      public void handleResult(ResultContext resultContext) {
        Long resourceId = (Long) resultContext.getResultObject();
        disableResource(resourceId, purgeMapper);
      }
    });
    session.commit();
  }

  public List<PurgeableSnapshotDto> selectPurgeableSnapshots(long resourceId) {
    SqlSession session = mybatis.openBatchSession();
    try {
      PurgeMapper mapper = session.getMapper(PurgeMapper.class);
      List<PurgeableSnapshotDto> result = Lists.newArrayList();
      result.addAll(mapper.selectPurgeableSnapshotsWithEvents(resourceId));
      result.addAll(mapper.selectPurgeableSnapshotsWithoutEvents(resourceId));
      Collections.sort(result);// sort by date
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PurgeDao deleteProject(long rootProjectId) {
    final SqlSession session = mybatis.openBatchSession();
    final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    try {
      deleteProject(rootProjectId, session, mapper);
      return this;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void deleteProject(final long rootProjectId, final SqlSession session, final PurgeMapper mapper) {
    List<Long> childrenIds = mapper.selectProjectIdsByRootId(rootProjectId);
    for (Long childId : childrenIds) {
      deleteProject(childId, session, mapper);
    }

    session.select("org.sonar.core.purge.PurgeMapper.selectResourceTreeIdsByRootId", rootProjectId, new ResultHandler() {
      public void handleResult(ResultContext context) {
        Long resourceId = (Long) context.getResultObject();
        deleteResource(resourceId, session, mapper);
      }
    });
    session.commit();
  }

  void deleteResource(final long resourceId, final SqlSession session, final PurgeMapper mapper) {
    session.select("org.sonar.core.purge.PurgeMapper.selectSnapshotIdsByResource", resourceId, new ResultHandler() {
      public void handleResult(ResultContext context) {
        Long snapshotId = (Long) context.getResultObject();
        deleteSnapshot(snapshotId, mapper);
      }
    });
    // TODO optimization: filter requests according to resource scope
    mapper.deleteResourceLinks(resourceId);
    mapper.deleteResourceProperties(resourceId);
    mapper.deleteResourceIndex(resourceId);
    mapper.deleteResourceGroupRoles(resourceId);
    mapper.deleteResourceUserRoles(resourceId);
    mapper.deleteResourceManualMeasures(resourceId);
    mapper.deleteResourceReviews(resourceId);
    mapper.deleteResourceEvents(resourceId);
    mapper.deleteResource(resourceId);
  }

  @VisibleForTesting
  void disableResource(long resourceId, PurgeMapper mapper) {
    mapper.deleteResourceIndex(resourceId);
    mapper.setSnapshotIsLastToFalse(resourceId);
    mapper.disableResource(resourceId);
    mapper.closeResourceReviews(resourceId);
  }


  public PurgeDao deleteSnapshots(PurgeSnapshotQuery query) {
    final SqlSession session = mybatis.openBatchSession();
    try {
      final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
      session.select("org.sonar.core.purge.PurgeMapper.selectSnapshotIds", query, new ResultHandler() {
        public void handleResult(ResultContext context) {
          Long snapshotId = (Long) context.getResultObject();
          deleteSnapshot(snapshotId, mapper);
        }
      });
      session.commit();
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  void purgeSnapshot(long snapshotId, PurgeMapper mapper) {
    // note that events are not deleted
    mapper.deleteSnapshotDependencies(snapshotId);
    mapper.deleteSnapshotDuplications(snapshotId);
    mapper.deleteSnapshotSource(snapshotId);
    mapper.deleteSnapshotViolations(snapshotId);
    mapper.deleteSnapshotWastedMeasures(snapshotId);
    mapper.deleteSnapshotMeasuresOnQualityModelRequirements(snapshotId);
    mapper.updatePurgeStatusToOne(snapshotId);
  }

  @VisibleForTesting
  void deleteSnapshot(Long snapshotId, PurgeMapper mapper) {
    mapper.deleteSnapshotDependencies(snapshotId);
    mapper.deleteSnapshotDuplications(snapshotId);
    mapper.deleteSnapshotEvents(snapshotId);
    mapper.deleteSnapshotMeasureData(snapshotId);
    mapper.deleteSnapshotMeasures(snapshotId);
    mapper.deleteSnapshotSource(snapshotId);
    mapper.deleteSnapshotViolations(snapshotId);
    mapper.deleteSnapshot(snapshotId);
  }
}
