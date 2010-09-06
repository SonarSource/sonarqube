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

public class DefaultPersister extends ResourcePersister {

  public DefaultPersister(DatabaseSession session) {
    super(session);
  }

  @Override
  protected Snapshot createSnapshot(Bucket bucket, ResourceModel resourceModel) {
    Snapshot parentSnapshot = (bucket.getParent() != null ? bucket.getParent().getSnapshot() : null);
    Snapshot snapshot = new Snapshot(resourceModel, parentSnapshot);
    return snapshot;
  }

  @Override
  protected void prepareResourceModel(ResourceModel resourceModel, Bucket bucket) {
    resourceModel.setRootId(bucket.getProject() != null ? bucket.getProject().getResourceId() : null);
  }

  @Override
  protected String generateEffectiveKey(Bucket bucket) {
    return new StringBuilder(ResourceModel.KEY_SIZE)
        .append(bucket.getProject().getResource().getKey())
        .append(':')
        .append(bucket.getResource().getKey())
        .toString();
  }
}
