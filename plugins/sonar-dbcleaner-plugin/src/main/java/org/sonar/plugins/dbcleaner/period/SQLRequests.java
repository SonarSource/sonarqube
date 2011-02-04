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
package org.sonar.plugins.dbcleaner.period;

import org.sonar.api.batch.Event;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;

import javax.persistence.Query;
import java.util.List;

final class SQLRequests {

  private final DatabaseSession session;

  SQLRequests(DatabaseSession session) {
    this.session = session;
  }

  List<Snapshot> getProjectSnapshotsOrderedByCreatedAt(int oneProjectSnapshotId) {
    Query query = session.createQuery("FROM " + Snapshot.class.getSimpleName()
        + " sp1 WHERE sp1.resourceId  = (select sp2.resourceId FROM " + Snapshot.class.getSimpleName()
        + " sp2 WHERE sp2.id = :id) and sp1.rootId= null and not exists (from " + Event.class.getSimpleName() + " e where e.snapshot=sp1) order by sp1.createdAt");
    query.setParameter("id", oneProjectSnapshotId);
    return query.getResultList();
  }

  List<Integer> getChildIds(Snapshot parentSnapshot) {
    Query query = session.createQuery("select sp.id FROM " + Snapshot.class.getSimpleName()
        + " sp WHERE sp.rootId  = :rootId or id = :rootId");
    query.setParameter("rootId", parentSnapshot.getId());
    return query.getResultList();
  }
}
