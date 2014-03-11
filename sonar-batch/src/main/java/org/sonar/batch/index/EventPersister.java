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
package org.sonar.batch.index;

import org.sonar.api.batch.Event;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;

import java.util.Collections;
import java.util.List;

public final class EventPersister {
  private DatabaseSession session;
  private ResourcePersister resourcePersister;

  public EventPersister(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public List<Event> getEvents(Resource resource) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    if (snapshot == null) {
      return Collections.emptyList();
    }
    return session.getResults(Event.class, "resourceId", snapshot.getResourceId());
  }

  public void deleteEvent(Event event) {
    session.removeWithoutFlush(event);
    session.commit();
  }

  public void saveEvent(Resource resource, Event event) {
    Snapshot snapshot = resourcePersister.getSnapshotOrFail(resource);
    if (event.getDate() == null) {
      event.setSnapshot(snapshot);
    } else {
      event.setResourceId(snapshot.getResourceId());
    }
    session.save(event);
    session.commit();

  }
}
