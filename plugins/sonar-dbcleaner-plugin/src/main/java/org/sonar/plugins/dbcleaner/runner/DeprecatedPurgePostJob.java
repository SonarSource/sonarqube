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
package org.sonar.plugins.dbcleaner.runner;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.NotDryRun;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;

import javax.persistence.Query;

@NotDryRun
public final class DeprecatedPurgePostJob implements PostJob {

  private DatabaseSession session;
  private Snapshot snapshot;
  private Purge[] purges;
  private Project project;
  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedPurgePostJob.class);

  public DeprecatedPurgePostJob(DatabaseSession session, Project project, Snapshot snapshot, Purge[] purges) {
    this.session = session;
    this.project = project;
    this.snapshot = snapshot;
    this.purges = purges.clone();
  }

  public void executeOn(Project project, SensorContext context) {
    if (shouldExecuteOn(project)) {
      purge();
    }
  }

  static boolean shouldExecuteOn(Project project) {
    return project.isRoot();
  }

  public void purge() {
    TimeProfiler profiler = new TimeProfiler(LOG).start("Database optimization");
    DefaultPurgeContext context = newContext();
    LOG.debug("Snapshots to purge: " + context);
    executePurges(context);
    profiler.stop();
  }


  private void executePurges(DefaultPurgeContext context) {
    TimeProfiler profiler = new TimeProfiler();
    for (Purge purge : purges) {
      try {
        profiler.start("Purge " + purge.getClass().getName());
        purge.purge(context);
        session.commit(); // force hibernate to commit, so we're sure that the potential raised exception comes from this purge
        profiler.stop();
      } catch (javax.persistence.PersistenceException e) {
        // Temporary workaround for MySQL deadlocks. The exception must not fail the build
        // See https://jira.codehaus.org/browse/SONAR-2961 and https://jira.codehaus.org/browse/SONAR-2190
        LOG.warn("Fail to execute purge: " + purge, e);

      } catch (HibernateException e) {
        // Temporary workaround for MySQL deadlocks. The exception must not fail the build
        // See https://jira.codehaus.org/browse/SONAR-2961 and https://jira.codehaus.org/browse/SONAR-2190
        LOG.warn("Fail to execute purge: " + purge, e);
      }
    }
  }

  private DefaultPurgeContext newContext() {
    DefaultPurgeContext context = new DefaultPurgeContext(project, snapshot);
    Snapshot previousLastSnapshot = getPreviousLastSnapshot();
    if (previousLastSnapshot != null && previousLastSnapshot.getCreatedAt().before(snapshot.getCreatedAt())) {
      context.setLastSnapshotId(previousLastSnapshot.getId());
    }
    return context;
  }

  private Snapshot getPreviousLastSnapshot() {
    Query query = session.createQuery(
      "SELECT s FROM " + Snapshot.class.getSimpleName() + " s " +
        "WHERE s.status=:status AND s.resourceId=:resourceId AND s.createdAt<:date AND s.id <> :sid ORDER BY s.createdAt DESC");
    query.setParameter("status", Snapshot.STATUS_PROCESSED);
    query.setParameter("resourceId", snapshot.getResourceId());
    query.setParameter("date", snapshot.getCreatedAt());
    query.setParameter("sid", snapshot.getId());
    query.setMaxResults(1);
    return session.getSingleResult(query, null);
  }

  static final class DefaultPurgeContext implements PurgeContext {

    private Project project;
    private Integer currentSid;
    private Integer previousSid;

    public DefaultPurgeContext(Project project, Snapshot currentSnapshot) {
      this(project, currentSnapshot, null);
    }

    public DefaultPurgeContext(Project project, Snapshot currentSnapshot, Snapshot previousSnapshot) {
      this.project = project;
      if (currentSnapshot != null) {
        currentSid = currentSnapshot.getId();
      }
      if (previousSnapshot != null) {
        previousSid = previousSnapshot.getId();
      }
    }

    public DefaultPurgeContext setLastSnapshotId(Integer previousSid) {
      this.previousSid = previousSid;
      return this;
    }

    public Integer getSnapshotId() {
      return currentSid;
    }

    public Integer getPreviousSnapshotId() {
      return previousSid;
    }

    public Project getProject() {
      return project;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
        .append("currentSid", currentSid)
        .append("previousSid", previousSid)
        .toString();
    }
  }

}
