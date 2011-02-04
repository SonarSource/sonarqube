/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.components;

import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourcePersister;

import java.util.Collections;
import java.util.List;

public class PastViolationsLoader implements BatchExtension {

  private DatabaseSession session;
  private ResourcePersister resourcePersister;

  public PastViolationsLoader(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public List<RuleFailureModel> getPastViolations(Resource resource) {
    if (resource == null) {
      return Collections.emptyList();
    }

    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    if (snapshot == null) {
      throw new SonarException("This resource has no snapshot ???" + resource);
    }
    Snapshot previousLastSnapshot = resourcePersister.getLastSnapshot(snapshot, true);
    if (previousLastSnapshot == null) {
      return Collections.emptyList();
    }
    return session.getResults(RuleFailureModel.class,
        "snapshotId", previousLastSnapshot.getId());
  }

  public SnapshotSource getSource(Resource resource) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    return session.getSingleResult(SnapshotSource.class,
        "snapshotId", snapshot.getId());
  }

}
