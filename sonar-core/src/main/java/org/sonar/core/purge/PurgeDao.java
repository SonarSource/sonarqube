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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @since 2.14
 */
public class PurgeDao {
  private final MyBatis mybatis;
  private final ResourceDao resourceDao;
  private static final Logger LOG = LoggerFactory.getLogger(PurgeDao.class);
  private PurgeProfiler profiler;

  public PurgeDao(MyBatis mybatis, ResourceDao resourceDao, PurgeProfiler profiler) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
    this.profiler = profiler;
  }

  public PurgeDao purge(PurgeConfiguration conf) {
    SqlSession session = mybatis.openBatchSession();
    PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    PurgeCommands commands = new PurgeCommands(session, mapper, profiler);
    try {
      List<ResourceDto> projects = getProjects(conf.rootProjectId(), session);
      for (ResourceDto project : projects) {
        LOG.info("-> Clean " + project.getLongName() + " [id=" + project.getId() + "]");
        deleteAbortedBuilds(project, commands);
        purge(project, conf.scopesWithoutHistoricalData(), commands);
      }
      for (ResourceDto project : projects) {
        disableOrphanResources(project, session, mapper);
      }
      deleteOldClosedIssues(conf, mapper);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }

  private void deleteOldClosedIssues(PurgeConfiguration conf, PurgeMapper mapper) {
    Date toDate = conf.maxLiveDateOfClosedIssues();
    mapper.deleteOldClosedIssueChanges(conf.rootProjectId(), toDate);
    mapper.deleteOldClosedIssues(conf.rootProjectId(), toDate);
  }

  private void deleteAbortedBuilds(ResourceDto project, PurgeCommands commands) {
    if (hasAbortedBuilds(project.getId(), commands)) {
      LOG.info("<- Delete aborted builds");
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
        .setIslast(false)
        .setStatus(new String[]{"U"})
        .setRootProjectId(project.getId());
      commands.deleteSnapshots(query);
    }
  }

  private boolean hasAbortedBuilds(Long projectId, PurgeCommands commands) {
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
      .setIslast(false)
      .setStatus(new String[]{"U"})
      .setResourceId(projectId);
    return !commands.selectSnapshotIds(query).isEmpty();
  }

  private void purge(ResourceDto project, String[] scopesWithoutHistoricalData, PurgeCommands purgeCommands) {
    List<Long> projectSnapshotIds = purgeCommands.selectSnapshotIds(
      PurgeSnapshotQuery.create()
        .setResourceId(project.getId())
        .setIslast(false)
        .setNotPurged(true)
    );
    for (final Long projectSnapshotId : projectSnapshotIds) {
      LOG.info("<- Clean snapshot " + projectSnapshotId);
      if (!ArrayUtils.isEmpty(scopesWithoutHistoricalData)) {
        PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
          .setIslast(false)
          .setScopes(scopesWithoutHistoricalData)
          .setRootSnapshotId(projectSnapshotId);
        purgeCommands.deleteSnapshots(query);
      }

      PurgeSnapshotQuery query = PurgeSnapshotQuery.create().setRootSnapshotId(projectSnapshotId).setNotPurged(true);
      purgeCommands.purgeSnapshots(query);

      // must be executed at the end for reentrance
      purgeCommands.purgeSnapshots(PurgeSnapshotQuery.create().setId(projectSnapshotId).setNotPurged(true));
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
      // sort by date
      Collections.sort(result);
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PurgeDao deleteResourceTree(long rootProjectId) {
    final SqlSession session = mybatis.openBatchSession();
    final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    try {
      deleteProject(rootProjectId, mapper, new PurgeCommands(session, profiler));
      return this;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void deleteProject(long rootProjectId, PurgeMapper mapper, PurgeCommands commands) {
    List<Long> childrenIds = mapper.selectProjectIdsByRootId(rootProjectId);
    for (Long childId : childrenIds) {
      deleteProject(childId, mapper, commands);
    }

    List<Long> resourceIds = mapper.selectResourceIdsByRootId(rootProjectId);
    commands.deleteResources(resourceIds);
  }

  private void disableResource(long resourceId, PurgeMapper mapper) {
    mapper.deleteResourceIndex(Arrays.asList(resourceId));
    mapper.setSnapshotIsLastToFalse(resourceId);
    mapper.disableResource(resourceId);
    mapper.resolveResourceIssuesNotAlreadyResolved(resourceId);
  }

  public PurgeDao deleteSnapshots(PurgeSnapshotQuery query) {
    final SqlSession session = mybatis.openBatchSession();
    try {
      new PurgeCommands(session, profiler).deleteSnapshots(query);
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
