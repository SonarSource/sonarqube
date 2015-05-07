/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.deprecated.components;

import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.components.PastSnapshot;

import javax.annotation.CheckForNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.sonar.api.utils.DateUtils.dateToLong;

@BatchSide
public class PastSnapshotFinderByDate {

  private DatabaseSession session;

  public PastSnapshotFinderByDate(DatabaseSession session) {
    this.session = session;
  }

  public PastSnapshot findByDate(Snapshot projectSnapshot, Date date) {
    Integer projectId = projectSnapshot != null ? projectSnapshot.getResourceId() : null;
    return findByDate(projectId, date);
  }

  PastSnapshot findByDate(Integer projectId, Date date) {
    Snapshot snapshot = null;
    if (projectId != null) {
      snapshot = findSnapshot(projectId, date);
    }
    SimpleDateFormat format = new SimpleDateFormat(DateUtils.DATE_FORMAT);
    return new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_DATE, date, snapshot).setModeParameter(format.format(date));
  }

  @CheckForNull
  private Snapshot findSnapshot(Integer projectId, Date date) {
    String hql = "from " + Snapshot.class.getSimpleName() + " where createdAt>=:date AND resourceId=:resourceId AND status=:status AND qualifier<>:lib order by createdAt asc";
    List<Snapshot> snapshots = session.createQuery(hql)
      .setParameter("date", dateToLong(date))
      .setParameter("resourceId", projectId)
      .setParameter("status", Snapshot.STATUS_PROCESSED)
      .setParameter("lib", Qualifiers.LIBRARY)
      .setMaxResults(1)
      .getResultList();

    return snapshots.isEmpty() ? null : snapshots.get(0);
  }
}
