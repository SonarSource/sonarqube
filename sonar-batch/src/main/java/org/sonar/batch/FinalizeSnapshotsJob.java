/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Purge;
import org.sonar.api.batch.PurgeContext;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.purge.DefaultPurgeContext;

import javax.persistence.Query;

public class FinalizeSnapshotsJob implements CoreJob {

  private DatabaseSession session;
  private Purge[] purges;
  private ServerMetadata server;
  private Snapshot snapshot;

  public FinalizeSnapshotsJob(ServerMetadata server, DatabaseSession session, Purge[] purges, Snapshot snapshot) {
    this.session = session;
    this.purges = purges;
    this.server = server;
    this.snapshot = snapshot;
  }

  public void execute(Project project, SensorContext context) {
    if (shouldExecuteOn(project)) {
      Snapshot previousLastSnapshot = getPreviousLastSnapshot(snapshot);
      updateFlags(snapshot, previousLastSnapshot);
      purge(snapshot, previousLastSnapshot);
    }
  }

  private boolean shouldExecuteOn(Project project) {
    return project.isRoot();
  }

  private Snapshot getPreviousLastSnapshot(Snapshot snapshot) {
    Query query = session.createQuery(
        "SELECT s FROM " + Snapshot.class.getSimpleName() + " s " +
            "WHERE s.last=true AND s.resourceId=:resourceId");
    query.setParameter("resourceId", snapshot.getResourceId());
    return session.getSingleResult(query, null);
  }


  private void updateFlags(Snapshot rootSnapshot, Snapshot previousLastSnapshot) {
    if (previousLastSnapshot != null && previousLastSnapshot.getCreatedAt().before(rootSnapshot.getCreatedAt())) {
      setFlags(previousLastSnapshot, false, null);
    }

    boolean isLast = (previousLastSnapshot == null || previousLastSnapshot.getCreatedAt().before(rootSnapshot.getCreatedAt()));
    setFlags(rootSnapshot, isLast, Snapshot.STATUS_PROCESSED);
    LoggerFactory.getLogger(getClass()).info("ANALYSIS SUCCESSFUL, you can browse {}", server.getUrl());
  }

  private void setFlags(Snapshot snapshot, boolean last, String status) {
    String hql = "UPDATE " + Snapshot.class.getSimpleName() + " SET last=:last";
    if (status != null) {
      hql += ", status=:status ";
    }
    hql += " WHERE root_snapshot_id=:rootId OR id=:rootId OR (path LIKE :path AND root_snapshot_id=:pathRootId)";

    Query query = session.createQuery(hql);
    if (status != null) {
      query.setParameter("status", status);
      snapshot.setStatus(status);
    }
    query.setParameter("last", last);
    query.setParameter("rootId", snapshot.getId());
    query.setParameter("path", snapshot.getPath() + snapshot.getId() + ".%");
    query.setParameter("pathRootId", (snapshot.getRootId() == null ? snapshot.getId() : snapshot.getRootId()));
    query.executeUpdate();
    session.commit();

    snapshot.setLast(last);
  }

  private void purge(Snapshot currentSnapshot, Snapshot previousLastSnapshot) {
    final Logger logger = LoggerFactory.getLogger(FinalizeSnapshotsJob.class);
    TimeProfiler profiler = new TimeProfiler(logger).start("Database optimization");
    PurgeContext context = createPurgeContext(currentSnapshot, previousLastSnapshot);
    logger.debug("Snapshots to purge: " + context);
    for (Purge purge : purges) {
      logger.debug("Executing {}...", purge.getClass().getName());
      purge.purge(context);
    }
    profiler.stop();
  }

  private PurgeContext createPurgeContext(Snapshot currentSnapshot, Snapshot previousLastSnapshot) {
    DefaultPurgeContext context = new DefaultPurgeContext(currentSnapshot);
    if (previousLastSnapshot != null && previousLastSnapshot.getCreatedAt().before(currentSnapshot.getCreatedAt())) {
      context.setLastSnapshotId(previousLastSnapshot.getId());
    }
    return context;
  }

}
