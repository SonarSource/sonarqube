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
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Resource;

import java.util.Date;

public class LibraryPersister extends ResourcePersister<Library> {

  private Date now;

  public LibraryPersister(DatabaseSession session) {
    super(session);
    this.now = new Date();
  }

  LibraryPersister(DatabaseSession session, Date now) {
    super(session);
    this.now = now;
  }

  @Override
  protected String generateEffectiveKey(Bucket<Library> bucket) {
    return bucket.getResource().getKey();
  }

  @Override
  protected void prepareResourceModel(ResourceModel resourceModel, Bucket<Library> bucket) {
  }

  @Override
  protected Snapshot createSnapshot(Bucket<Library> bucket, ResourceModel resourceModel) {
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class,
        "resourceId", resourceModel.getId(),
        "version", bucket.getResource().getVersion(),
        "scope", Resource.SCOPE_SET,
        "qualifier", Resource.QUALIFIER_LIB);
    if (snapshot == null) {
      snapshot = new Snapshot(resourceModel, null);
      snapshot.setCreatedAt(now);
      snapshot.setVersion(bucket.getResource().getVersion());
      snapshot.setStatus(Snapshot.STATUS_PROCESSED);
    }
    return snapshot;
  }
}
