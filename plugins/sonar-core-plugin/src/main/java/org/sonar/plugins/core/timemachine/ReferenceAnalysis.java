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
package org.sonar.plugins.core.timemachine;

import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Resource;

import javax.persistence.Query;

import java.util.Collections;
import java.util.List;

public class ReferenceAnalysis implements BatchExtension {

  private DatabaseSession session;

  public ReferenceAnalysis(DatabaseSession session) {
    this.session = session;
  }

  public List<RuleFailureModel> getViolations(Resource resource) {
    Snapshot snapshot = getSnapshot(resource);
    if (snapshot != null) {
      return session.getResults(RuleFailureModel.class, "snapshotId", snapshot.getId());
    }
    return Collections.emptyList();
  }

  public String getSource(Resource resource) {
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
