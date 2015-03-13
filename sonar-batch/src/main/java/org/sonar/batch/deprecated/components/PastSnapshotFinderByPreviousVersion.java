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

import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Event;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.components.PastSnapshot;

import java.util.List;

import static org.sonar.api.utils.DateUtils.longToDate;

public class PastSnapshotFinderByPreviousVersion implements BatchExtension {

  private final DatabaseSession session;

  public PastSnapshotFinderByPreviousVersion(DatabaseSession session) {
    this.session = session;
  }

  public PastSnapshot findByPreviousVersion(Snapshot projectSnapshot) {
    String currentVersion = projectSnapshot.getVersion();
    Integer resourceId = projectSnapshot.getResourceId();

    String hql = "select e from " + Event.class.getSimpleName() + " e " +
      " join e.resource component where e.name<>:version AND e.category='Version' AND component.id=:resourceId ORDER BY e.date DESC";

    List<Event> events = session.createQuery(hql)
      .setParameter("version", currentVersion)
      .setParameter("resourceId", resourceId)
      .setMaxResults(1)
      .getResultList();

    if (events.isEmpty()) {
      return new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    }

    Event previousVersionEvent = events.get(0);
    Snapshot snapshot = session.getSingleResult(Snapshot.class, "id", previousVersionEvent.getSnapshot().getId());

    return new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, longToDate(snapshot.getCreatedAtMs()), snapshot).setModeParameter(snapshot.getVersion());
  }

}
