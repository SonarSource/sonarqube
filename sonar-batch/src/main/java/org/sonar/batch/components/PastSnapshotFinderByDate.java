/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.components;

import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PastSnapshotFinderByDate implements BatchExtension {

  private DatabaseSession session;

  public PastSnapshotFinderByDate(DatabaseSession session) {
    this.session = session;
  }

  PastSnapshot findByDate(Snapshot projectSnapshot, Date date) {
    Snapshot snapshot = null;
    if (projectSnapshot != null) {
      snapshot = findSnapshot(projectSnapshot, date);
    }
    SimpleDateFormat format = new SimpleDateFormat(DateUtils.DATE_FORMAT);
    return new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_DATE, date, snapshot).setModeParameter(format.format(date));
  }


  private Snapshot findSnapshot(Snapshot projectSnapshot, Date date) {
    String hql = "from " + Snapshot.class.getSimpleName() + " where createdAt>=:date AND resourceId=:resourceId AND status=:status AND qualifier<>:lib order by createdAt asc";
    List<Snapshot> snapshots = session.createQuery(hql)
        .setParameter("date", date)
        .setParameter("resourceId", projectSnapshot.getResourceId())
        .setParameter("status", Snapshot.STATUS_PROCESSED)
        .setParameter("lib", Qualifiers.LIBRARY)
        .setMaxResults(1)
        .getResultList();

    return snapshots.isEmpty() ? null : snapshots.get(0);
  }
}