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
package org.sonar.batch.report;

import org.sonar.api.BatchSide;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.Constants.EventCategory;
import org.sonar.batch.protocol.output.BatchReport.Event;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BatchSide
public class EventCache {

  private final Map<Integer, List<Event>> eventsByComponentBatchId = new HashMap<>();
  private final ResourceCache resourceCache;

  public EventCache(ResourceCache resourceCache) {
    this.resourceCache = resourceCache;
  }

  public void createEvent(Resource resource, String name, @Nullable String description, EventCategory category, @Nullable String data) {
    org.sonar.batch.protocol.output.BatchReport.Event.Builder eventBuilder = org.sonar.batch.protocol.output.BatchReport.Event.newBuilder();
    eventBuilder.setName(name);
    if (description != null) {
      eventBuilder.setDescription(description);
    }
    eventBuilder.setCategory(category);
    if (data != null) {
      eventBuilder.setEventData(data);
    }
    int componentBatchId = resourceCache.get(resource).batchId();
    eventBuilder.setComponentRef(componentBatchId);
    addEvent(componentBatchId, eventBuilder.build());
  }

  private void addEvent(int componentBatchId, Event e) {
    if (!eventsByComponentBatchId.containsKey(componentBatchId)) {
      eventsByComponentBatchId.put(componentBatchId, new ArrayList<Event>());
    }
    eventsByComponentBatchId.get(componentBatchId).add(e);
  }

  public List<Event> getEvents(int componentBatchId) {
    if (eventsByComponentBatchId.containsKey(componentBatchId)) {
      return eventsByComponentBatchId.get(componentBatchId);
    }
    return Collections.emptyList();
  }
}
