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
import org.apache.commons.lang.ArrayUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import java.util.Collections;
import java.util.List;

/**
 * @since 2.14
 */
public class PurgeDao {
  private final MyBatis mybatis;
  private final ResourceDao resourceDao;
  private static final Logger LOG = LoggerFactory.getLogger(PurgeDao.class);

  public PurgeDao(MyBatis mybatis, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
  }

  public PurgeDao purge(long rootResourceId, String[] scopesWithoutHistoricalData) {
    SqlSession session = mybatis.openBatchSession();
    PurgeMapper purgeMapper = session.getMapper(PurgeMapper.class);
    try {
      List<ResourceDto> projects = getProjects(rootResourceId, session);
      for (ResourceDto project : projects) {
        LOG.info("-> Clean " + project.getLongName() + " [id=" + project.getId() + "]");
        deleteAbortedBuilds(project, session, purgeMapper);
        purge(project, scopesWithoutHistoricalData, session, purgeMapper);
      }
      for (ResourceDto project : projects) {
        disableOrphanResources(project, session, purgeMapper);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }

  private void deleteAbortedBuilds(ResourceDto project, SqlSession session, PurgeMapper purgeMapper) {
    if (hasAbortedBuilds(project.getId(), purgeMapper)) {
      LOG.info("<- Delete aborted builds");
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
          .setIslast(false)
          .setStatus(new String[]{"U"})
          .setRootProjectId(project.getId());
      PurgeCommands.deleteSnapshots(query, session, purgeMapper);
      session.commit();
    }
  }

  private boolean hasAbortedBuilds(Long projectId, PurgeMapper purgeMapper) {
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
        .setIslast(false)
        .setStatus(new String[]{"U"})
        .setResourceId(projectId);
    return !purgeMapper.selectSnapshotIds(query).isEmpty();
  }

  private void purge(final ResourceDto project, final String[] scopesWithoutHistoricalData, final SqlSession session, final PurgeMapper purgeMapper) {
    List<Long> projectSnapshotIds = purgeMapper.selectSnapshotIds(
        PurgeSnapshotQuery.create().setResourceId(project.getId()).setIslast(false).setNotPurged(true)
    );
    for (final Long projectSnapshotId : projectSnapshotIds) {
      LOG.info("<- Clean snapshot " + projectSnapshotId);
      if (!ArrayUtils.isEmpty(scopesWithoutHistoricalData)) {
        PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
            .setIslast(false)
            .setScopes(scopesWithoutHistoricalData)
            .setRootSnapshotId(projectSnapshotId);
        PurgeCommands.deleteSnapshots(query, session, purgeMapper);
        session.commit();
      }

      PurgeSnapshotQuery query = PurgeSnapshotQuery.create().setRootSnapshotId(projectSnapshotId).setNotPurged(true);
      PurgeCommands.purgeSnapshots(query, session, purgeMapper);
      session.commit();

      // must be executed at the end for reentrance
      PurgeCommands.purgeSnapshots(PurgeSnapshotQuery.create().setId(projectSnapshotId).setNotPurged(true), session, purgeMapper);
      session.commit();
    }
  }

  private void disableOrphanResources(final ResourceDto project, final SqlSession session, final PurgeMapper purgeMapper) {
    session.select("org.sonar.core.purge.PurgeMapper.selectResourceIdsToDisable", project.getId(), new ResultHandler() {
      public void handleResult(ResultContext resultContext) {
        Long resourceId = (Long) resultContext.getResultObject();
        if (resourceId != null) {
          disableResource(resourceId, purgeMapper);
        }
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

  public PurgeDao deleteResourceTree(long rootProjectId) {
    final SqlSession session = mybatis.openBatchSession();
    final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    final PurgeVendorMapper vendorMapper = session.getMapper(PurgeVendorMapper.class);
    try {
      deleteProject(rootProjectId, session, mapper, vendorMapper);
      return this;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void deleteProject(long rootProjectId, SqlSession session, PurgeMapper mapper, PurgeVendorMapper vendorMapper) {
    List<Long> childrenIds = mapper.selectProjectIdsByRootId(rootProjectId);
    for (Long childId : childrenIds) {
      deleteProject(childId, session, mapper, vendorMapper);
    }

    List<Long> resourceIds = mapper.selectResourceIdsByRootId(rootProjectId);
    PurgeCommands.deleteResources(resourceIds, session, mapper, vendorMapper);
    session.commit();
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
      PurgeCommands.deleteSnapshots(query, session, mapper);
      session.commit();
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Load the whole tree of projects, including the project given in parameter.
   */
  private List<ResourceDto> getProjects(long rootProjectId, SqlSession session) {
    List<ResourceDto> projects = Lists.newArrayList();
    projects.add(resourceDao.getResource(rootProjectId, session));
    projects.addAll(resourceDao.getDescendantProjects(rootProjectId, session));
    return projects;
  }

}
