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
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.System2;

import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class EventPersister {
  private final System2 system2;
  private DatabaseSession session;
  private ResourceCache resourceCache;

  public EventPersister(DatabaseSession session, ResourceCache resourceCache, System2 system2) {
    this.session = session;
    this.resourceCache = resourceCache;
    this.system2 = system2;
  }

  public List<Event> getEvents(Resource resource) {
    return session.getResults(Event.class, "componentUuid", resource.getUuid());
  }

  public void deleteEvent(Event event) {
    session.removeWithoutFlush(event);
    session.commit();
  }

  public void saveEvent(Resource resource, Event event) {
    BatchResource batchResource = resourceCache.get(resource.getEffectiveKey());
    checkState(batchResource != null, "Unknown component: " + resource);

    event.setCreatedAt(new Date(system2.now()));
    if (event.getDate() == null) {
      event.setSnapshot(batchResource.snapshot());
      event.setComponentUuid(batchResource.resource().getUuid());
    } else {
      event.setComponentUuid(batchResource.resource().getUuid());
    }

    session.save(event);
    session.commit();
  }
}
