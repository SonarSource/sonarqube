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
package org.sonar.batch.scan;

import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrap.ServerClient;

import javax.annotation.CheckForNull;
import javax.persistence.Query;

import java.util.Collections;
import java.util.List;

public class LastSnapshots implements BatchComponent {

  private final Settings settings;
  private final DatabaseSession session;
  private final ServerClient server;

  public LastSnapshots(Settings settings, DatabaseSession session, ServerClient server) {
    this.settings = settings;
    this.session = session;
    this.server = server;
  }

  /**
   * Return null if this is the first scan (no last scan).
   */
  @CheckForNull
  public List<RuleFailureModel> getViolations(Resource resource) {
    Snapshot snapshot = getSnapshot(resource);
    if (snapshot != null) {
      return session.getResults(RuleFailureModel.class, "snapshotId", snapshot.getId());
    }
    return null;
  }

  public String getSource(Resource resource) {
    String source = "";
    if (ResourceUtils.isFile(resource)) {
      if (settings.getBoolean(CoreProperties.DRY_RUN)) {
        source = loadSourceFromWs(resource);
      } else {
        source = loadSourceFromDb(resource);
      }
    }
    return source;
  }

  private String loadSourceFromWs(Resource resource) {
    try {
      return server.request("/api/sources?resource=" + resource.getEffectiveKey() + "&format=txt", false);
    } catch (HttpDownloader.HttpException he) {
      if (he.getResponseCode() == 404) {
        return "";
      }
      throw he;
    }
  }

  private String loadSourceFromDb(Resource resource) {
    Snapshot snapshot = getSnapshot(resource);
    if (snapshot != null) {
      SnapshotSource source = session.getSingleResult(SnapshotSource.class, "snapshotId", snapshot.getId());
      if (source != null) {
        return source.getData();
      }
    }
    return "";
  }

  private Snapshot getSnapshot(Resource resource) {
    Query query = session.createQuery("from " + Snapshot.class.getSimpleName() + " s where s.last=:last and s.resourceId=(select r.id from "
      + ResourceModel.class.getSimpleName() + " r where r.key=:key)");
    query.setParameter("key", resource.getEffectiveKey());
    query.setParameter("last", Boolean.TRUE);
    return session.getSingleResult(query, null);
  }
}
