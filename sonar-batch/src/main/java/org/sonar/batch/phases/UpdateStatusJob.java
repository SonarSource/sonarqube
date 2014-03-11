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
package org.sonar.batch.phases;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.index.ResourcePersister;

import javax.persistence.Query;

import java.util.List;

public class UpdateStatusJob implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateStatusJob.class);

  private DatabaseSession session;
  private ServerClient server;
  // TODO remove this component
  private Snapshot snapshot;
  private ResourcePersister resourcePersister;
  private Settings settings;
  private Project project;
  private AnalysisMode analysisMode;

  public UpdateStatusJob(Settings settings, ServerClient server, DatabaseSession session,
    ResourcePersister resourcePersister, Project project, Snapshot snapshot, AnalysisMode analysisMode) {
    this.session = session;
    this.server = server;
    this.resourcePersister = resourcePersister;
    this.project = project;
    this.snapshot = snapshot;
    this.settings = settings;
    this.analysisMode = analysisMode;
  }

  public void execute() {
    disablePreviousSnapshot();
    enableCurrentSnapshot();
    evictPreviewDB();
  }

  @VisibleForTesting
  void evictPreviewDB() {
    if (analysisMode.isPreview()) {
      // If this is a preview analysis then we should not evict remote preview database
      return;
    }
    String url = "/batch_bootstrap/evict?project=" + project.getId();
    try {
      LOG.debug("Evict preview database");
      server.request(url);
    } catch (Exception e) {
      throw new SonarException("Unable to evict preview database: " + url, e);
    }
  }

  private void disablePreviousSnapshot() {
    // disable on all modules
    Query query = session.createQuery("FROM " + Snapshot.class.getSimpleName() + " WHERE (root_snapshot_id=:rootId OR id=:rootId) AND scope=:scope");
    query.setParameter("rootId", snapshot.getId());
    query.setParameter("scope", Scopes.PROJECT);
    List<Snapshot> moduleSnapshots = query.getResultList();
    for (Snapshot moduleSnapshot : moduleSnapshots) {
      Snapshot previousLastSnapshot = resourcePersister.getLastSnapshot(moduleSnapshot, true);
      if (previousLastSnapshot != null) {
        setFlags(previousLastSnapshot, false, null);
      }
    }
  }

  private void enableCurrentSnapshot() {
    Snapshot previousLastSnapshot = resourcePersister.getLastSnapshot(snapshot, false);
    boolean isLast = previousLastSnapshot == null || previousLastSnapshot.getCreatedAt().before(snapshot.getCreatedAt());
    setFlags(snapshot, isLast, Snapshot.STATUS_PROCESSED);
    logSuccess(LoggerFactory.getLogger(getClass()));
  }

  @VisibleForTesting
  void logSuccess(Logger logger) {
    if (analysisMode.isPreview()) {
      logger.info("ANALYSIS SUCCESSFUL");

    } else {
      String baseUrl = settings.getString(CoreProperties.SERVER_BASE_URL);
      if (baseUrl.equals(settings.getDefaultValue(CoreProperties.SERVER_BASE_URL))) {
        // If server base URL was not configured in Sonar server then is is better to take URL configured on batch side
        baseUrl = server.getURL();
      }
      if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      String url = baseUrl + "dashboard/index/" + project.getKey();
      logger.info("ANALYSIS SUCCESSFUL, you can browse {}", url);
    }
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
    query.setParameter("pathRootId", snapshot.getRootId() == null ? snapshot.getId() : snapshot.getRootId());
    query.executeUpdate();
    session.commit();

    snapshot.setLast(last);
  }
}
