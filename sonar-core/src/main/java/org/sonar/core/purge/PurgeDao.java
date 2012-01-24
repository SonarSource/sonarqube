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

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

public class PurgeDao {
  private final MyBatis mybatis;

  public PurgeDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public PurgeDao disableOrphanResources(Object... handlers) {
    SqlSession session = mybatis.openSession(ExecutorType.BATCH);
    try {
      final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
      final BatchSession batchSession = new BatchSession(session);
      session.select("selectResourceIdsToDisable", new ResultHandler() {
        public void handleResult(ResultContext context) {
          Long resourceId = (Long) context.getResultObject();
          // TODO execute handlers in order to close reviews
          batchSession.increment(disableResource(resourceId, mapper));
        }
      });
      batchSession.commit();
      return this;

    } finally {
      MyBatis.closeSessionQuietly(session);
    }
  }

  public PurgeDao disableResource(long resourceId, Object... handlers) {
    SqlSession session = mybatis.openSession(ExecutorType.BATCH);
    try {
      final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
      disableResource(resourceId, mapper);
      session.commit();
      return this;

    } finally {
      MyBatis.closeSessionQuietly(session);
    }
  }

  int disableResource(long resourceId, PurgeMapper mapper) {
    mapper.disableResource(resourceId);
    mapper.deleteResourceIndex(resourceId);
    mapper.unsetSnapshotIslast(resourceId);
    // TODO close reviews
    return 3; // nb of SQL requests
  }

  public PurgeDao deleteSnapshots(PurgeSnapshotQuery query) {
    SqlSession session = mybatis.openSession(ExecutorType.BATCH);
    try {
      final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
      final BatchSession batchSession = new BatchSession(session);
      session.select("selectSnapshotIds", query, new ResultHandler() {
        public void handleResult(ResultContext context) {
          Long snapshotId = (Long) context.getResultObject();
          batchSession.increment(deleteSnapshot(snapshotId, mapper));
        }
      });
      batchSession.commit();
      return this;

    } finally {
      MyBatis.closeSessionQuietly(session);
    }
  }

  public PurgeDao purgeSnapshots(PurgeSnapshotQuery query) {
    SqlSession session = mybatis.openSession(ExecutorType.BATCH);
    try {
      final PurgeMapper mapper = session.getMapper(PurgeMapper.class);
      final BatchSession batchSession = new BatchSession(session);
      session.select("selectSnapshotIdsToPurge", query, new ResultHandler() {
        public void handleResult(ResultContext context) {
          Long snapshotId = (Long) context.getResultObject();
          batchSession.increment(purgeSnapshot(snapshotId, mapper));
        }
      });
      batchSession.commit();
      return this;

    } finally {
      MyBatis.closeSessionQuietly(session);
    }
  }

  int purgeSnapshot(long snapshotId, PurgeMapper mapper) {
    // note that events are not deleted.
    mapper.deleteSnapshotDependencies(snapshotId);
    mapper.deleteSnapshotDuplications(snapshotId);
    mapper.deleteSnapshotSource(snapshotId);
    mapper.deleteSnapshotViolations(snapshotId);
    mapper.deleteSnapshotRuleMeasures(snapshotId);
    mapper.deleteSnapshotCharacteristicMeasures(snapshotId);
    // TODO SONAR-2061 delete wasted measures (!metric.keepHistory)
    mapper.updatePurgeStatusToOne(snapshotId);
    return 7; // nb of SQL requests
  }

  int deleteSnapshot(Long snapshotId, PurgeMapper mapper) {
    mapper.deleteSnapshotDependencies(snapshotId);
    mapper.deleteSnapshotDuplications(snapshotId);
    mapper.deleteSnapshotEvents(snapshotId);
    mapper.deleteSnapshotMeasureData(snapshotId);
    mapper.deleteSnapshotMeasures(snapshotId);
    mapper.deleteSnapshotSource(snapshotId);
    mapper.deleteSnapshotViolations(snapshotId);
    mapper.deleteSnapshot(snapshotId);
    return 8; // nb of SQL requests
  }

  // TODO could be moved to org.sonar.core.persistence
  private static class BatchSession {
    static final int MAX_BATCH_SIZE = 1000;

    int count = 0;
    SqlSession session;

    private BatchSession(SqlSession session) {
      this.session = session;
    }

    BatchSession increment(int i) {
      count += i;
      if (count > MAX_BATCH_SIZE) {
        commit();
      }
      return this;
    }

    BatchSession commit() {
      session.commit();
      count = 0;
      return this;
    }

  }
}
