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
package org.sonar.plugins.core.timemachine;

import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;

import java.util.Date;
import java.util.List;

public class PastSnapshotFinderByDays implements BatchExtension {

  public static final String MODE = "days";

  private Snapshot projectSnapshot; // TODO replace by PersistenceManager
  private DatabaseSession session;

  public PastSnapshotFinderByDays(Snapshot projectSnapshot, DatabaseSession session) {
    this.projectSnapshot = projectSnapshot;
    this.session = session;
  }

  PastSnapshot findFromDays(int days) {
    Date targetDate = DateUtils.addDays(projectSnapshot.getCreatedAt(), -days);
    String hql = "from " + Snapshot.class.getSimpleName() + " where resourceId=:resourceId AND status=:status AND createdAt>=:from AND createdAt<:to order by createdAt asc";
    List<Snapshot> snapshots = session.createQuery(hql)
        .setParameter("from", targetDate)
        .setParameter("to", projectSnapshot.getCreatedAt())
        .setParameter("resourceId", projectSnapshot.getResourceId())
        .setParameter("status", Snapshot.STATUS_PROCESSED)
        .setMaxResults(1)
        .getResultList();

    if (snapshots.isEmpty()) {
      return null;
    }
    return new PastSnapshot(MODE, targetDate, snapshots.get(0)).setModeParameter(String.valueOf(days));
  }
}
