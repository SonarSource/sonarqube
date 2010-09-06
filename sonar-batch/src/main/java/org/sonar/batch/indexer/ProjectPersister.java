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
package org.sonar.batch.indexer;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;

public class ProjectPersister extends ResourcePersister<Project> {

  public ProjectPersister(DatabaseSession session) {
    super(session);
  }

  @Override
  protected String generateEffectiveKey(Bucket<Project> bucket) {
    return bucket.getResource().getKey();
  }

  @Override
  protected void prepareResourceModel(ResourceModel resourceModel, Bucket<Project> bucket) {
    if (bucket.getProject() != null) {
      resourceModel.setRootId(bucket.getProject().getResourceId());
    }

    // LIMITATION : project.getLanguage() is set only in ProjectBatch, not in AggregatorBatch, so we
    // have to explicitely use project.getLanguageKey()
    resourceModel.setLanguageKey(bucket.getResource().getLanguageKey());
  }

  @Override
  protected Snapshot createSnapshot(Bucket<Project> bucket, ResourceModel resourceModel) {
    Project project = bucket.getResource();
    Snapshot parentSnapshot = (bucket.getParent() != null ? bucket.getParent().getSnapshot() : null);
    Snapshot snapshot = new Snapshot(resourceModel, parentSnapshot);
    snapshot.setVersion(project.getAnalysisVersion());
    snapshot.setCreatedAt(project.getAnalysisDate());
    return snapshot;
  }
}
