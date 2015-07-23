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
package org.sonar.db.purge;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;

import static org.sonar.api.utils.DateUtils.dateToLong;

/**
 * @since 2.14
 */
public class PurgeDao implements Dao {
  private static final Logger LOG = LoggerFactory.getLogger(PurgeDao.class);
  private final MyBatis mybatis;
  private final ResourceDao resourceDao;
  private final System2 system2;

  public PurgeDao(MyBatis mybatis, ResourceDao resourceDao, System2 system2) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
    this.system2 = system2;
  }

  public PurgeDao purge(PurgeConfiguration conf, PurgeListener listener, PurgeProfiler profiler) {
    DbSession session = mybatis.openSession(true);
    try {
      purge(session, conf, listener, profiler);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }

  public void purge(DbSession session, PurgeConfiguration conf, PurgeListener listener, PurgeProfiler profiler) {
    PurgeMapper mapper = session.getMapper(PurgeMapper.class);
    PurgeCommands commands = new PurgeCommands(session, mapper, profiler);
    List<ResourceDto> projects = getProjects(conf.rootProjectIdUuid().getId(), session);
    for (ResourceDto project : projects) {
      LOG.debug("-> Clean " + project.getLongName() + " [id=" + project.getId() + "]");
      deleteAbortedBuilds(project, commands);
      purge(project, conf.scopesWithoutHistoricalData(), commands);
    }
    for (ResourceDto project : projects) {
      disableOrphanResources(project, session, mapper, listener);
    }
    deleteOldClosedIssues(conf, mapper);
  }

  private void deleteOldClosedIssues(PurgeConfiguration conf, PurgeMapper mapper) {
    Date toDate = conf.maxLiveDateOfClosedIssues();
    mapper.deleteOldClosedIssueChanges(conf.rootProjectIdUuid().getUuid(), dateToLong(toDate));
    mapper.deleteOldClosedIssues(conf.rootProjectIdUuid().getUuid(), dateToLong(toDate));
  }

  private void deleteAbortedBuilds(ResourceDto project, PurgeCommands commands) {
    if (hasAbortedBuilds(project.getId(), commands)) {
      LOG.debug("<- Delete aborted builds");
      PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
        .setIslast(false)
        .setStatus(new String[] {"U"})
        .setRootProjectId(project.getId());
      commands.deleteSnapshots(query);
    }
  }

  private boolean hasAbortedBuilds(Long projectId, PurgeCommands commands) {
    PurgeSnapshotQuery query = PurgeSnapshotQuery.create()
      .setIslast(false)
      .setStatus(new String[] {"U"})
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
      LOG.debug("<- Clean snapshot " + projectSnapshotId);
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

  private void disableOrphanResources(final ResourceDto project, final SqlSession session, final PurgeMapper purgeMapper, final PurgeListener purgeListener) {
    final List<IdUuidPair> componentIdUuids = new ArrayList<>();
    session.select("org.sonar.db.purge.PurgeMapper.selectComponentIdUuidsToDisable", project.getId(), new ResultHandler() {
      @Override
      public void handleResult(ResultContext resultContext) {
        IdUuidPair componentIdUuid = (IdUuidPair) resultContext.getResultObject();
        if (componentIdUuid.getId() != null) {
          componentIdUuids.add(componentIdUuid);
        }
      }
    });

    for (IdUuidPair componentIdUuid : componentIdUuids) {
      disableResource(componentIdUuid, purgeMapper);
      purgeListener.onComponentDisabling(componentIdUuid.getUuid());
    }

    session.commit();
  }

  public List<PurgeableSnapshotDto> selectPurgeableSnapshots(long resourceId) {
    DbSession session = mybatis.openSession(true);
    try {
      return selectPurgeableSnapshots(resourceId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<PurgeableSnapshotDto> selectPurgeableSnapshots(long resourceId, DbSession session) {
    List<PurgeableSnapshotDto> result = Lists.newArrayList();
    result.addAll(mapper(session).selectPurgeableSnapshotsWithEvents(resourceId));
    result.addAll(mapper(session).selectPurgeableSnapshotsWithoutEvents(resourceId));
    // sort by date
    Collections.sort(result);
    return result;
  }

  public PurgeDao deleteResourceTree(IdUuidPair rootIdUuid, PurgeProfiler profiler) {
    DbSession session = mybatis.openSession(true);
    try {
      return deleteResourceTree(session, rootIdUuid, profiler);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PurgeDao deleteResourceTree(DbSession session, IdUuidPair rootIdUuid, PurgeProfiler profiler) {
    deleteProject(rootIdUuid, mapper(session), new PurgeCommands(session, profiler));
    deleteFileSources(rootIdUuid.getUuid(), new PurgeCommands(session, profiler));
    return this;
  }

  private static void deleteFileSources(String rootUuid, PurgeCommands commands) {
    commands.deleteFileSources(rootUuid);
  }

  private static void deleteProject(IdUuidPair rootProjectId, PurgeMapper mapper, PurgeCommands commands) {
    List<IdUuidPair> childrenIdUuid = mapper.selectProjectIdUuidsByRootId(rootProjectId.getId());
    for (IdUuidPair childId : childrenIdUuid) {
      deleteProject(childId, mapper, commands);
    }

    List<IdUuidPair> componentIdUuids = mapper.selectComponentIdUuidsByRootId(rootProjectId.getId());
    commands.deleteResources(componentIdUuids);
  }

  private void disableResource(IdUuidPair componentIdUuid, PurgeMapper mapper) {
    long componentId = componentIdUuid.getId();
    mapper.deleteResourceIndex(Arrays.asList(componentId));
    mapper.setSnapshotIsLastToFalse(componentId);
    mapper.deleteFileSourcesByUuid(componentIdUuid.getUuid());
    mapper.disableResource(componentId);
    mapper.resolveResourceIssuesNotAlreadyResolved(componentIdUuid.getUuid(), system2.now());
  }

  public PurgeDao deleteSnapshots(PurgeSnapshotQuery query, PurgeProfiler profiler) {
    final DbSession session = mybatis.openSession(true);
    try {
      return deleteSnapshots(query, session, profiler);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public PurgeDao deleteSnapshots(PurgeSnapshotQuery query, DbSession session, PurgeProfiler profiler) {
    new PurgeCommands(session, profiler).deleteSnapshots(query);
    return this;
  }

  /**
   * Load the whole tree of projects, including the project given in parameter.
   */
  private List<ResourceDto> getProjects(long rootProjectId, SqlSession session) {
    List<ResourceDto> projects = Lists.newArrayList();
    projects.add(resourceDao.selectResource(rootProjectId, session));
    projects.addAll(resourceDao.getDescendantProjects(rootProjectId, session));
    return projects;
  }

  private PurgeMapper mapper(DbSession session) {
    return session.getMapper(PurgeMapper.class);
  }
}
