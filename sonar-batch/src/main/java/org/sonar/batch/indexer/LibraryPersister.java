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

import javax.persistence.Query;
import java.util.Date;
import java.util.List;

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
    Snapshot snapshot = findProject(resourceModel.getId(), bucket.getResource().getVersion());
    if (snapshot == null) {
      snapshot = findLibrary(resourceModel.getId(), bucket.getResource().getVersion());
    }

    if (snapshot == null) {
      snapshot = new Snapshot(resourceModel, null);
      snapshot.setCreatedAt(now);
      snapshot.setVersion(bucket.getResource().getVersion());
      snapshot.setStatus(Snapshot.STATUS_PROCESSED);

      // see http://jira.codehaus.org/browse/SONAR-1850
      // The qualifier must be LIB, even if the resource is TRK, because this snapshot has no measures.
      snapshot.setQualifier(Resource.QUALIFIER_LIB);
    }
    return snapshot;
  }

  private Snapshot findLibrary(Integer resourceId, String version) {
    List<Snapshot> snapshots = getSession().getResults(Snapshot.class,
        "resourceId", resourceId,
        "version", version,
        "scope", Resource.SCOPE_SET,
        "qualifier", Resource.QUALIFIER_LIB);
    if (snapshots.isEmpty()) {
      return null;
    }
    return snapshots.get(0);
  }

  private Snapshot findProject(Integer resourceId, String version) {
    Query query = getSession().createQuery("from " + Snapshot.class.getSimpleName() + " s WHERE s.resourceId=:resourceId AND s.version=:version AND s.scope=:scope AND s.qualifier<>:qualifier AND s.last=:last");
    query.setParameter("resourceId", resourceId);
    query.setParameter("version", version);
    query.setParameter("scope", Resource.SCOPE_SET);
    query.setParameter("qualifier", Resource.QUALIFIER_LIB);
    query.setParameter("last", Boolean.TRUE);
    List<Snapshot> snapshots = query.getResultList();
    if (snapshots.isEmpty()) {
      return null;
    }
    return snapshots.get(0);
  }
}
