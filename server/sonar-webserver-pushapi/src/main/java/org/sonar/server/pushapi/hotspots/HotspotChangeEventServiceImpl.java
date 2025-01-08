/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.pushapi.hotspots;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.pushevent.PushEventDto;

import static java.nio.charset.StandardCharsets.UTF_8;

@ServerSide
public class HotspotChangeEventServiceImpl implements HotspotChangeEventService {
  private static final Gson GSON = new GsonBuilder().create();

  private static final String EVENT_NAME = "SecurityHotspotChanged";

  private final DbClient dbClient;

  public HotspotChangeEventServiceImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void distributeHotspotChangedEvent(String projectUuid, HotspotChangedEvent hotspotEvent) {
    persistEvent(projectUuid, hotspotEvent);
  }

  private void persistEvent(String projectUuid, HotspotChangedEvent hotspotEvent) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PushEventDto eventDto = new PushEventDto()
        .setName(EVENT_NAME)
        .setProjectUuid(projectUuid)
        .setPayload(serializeIssueToPushEvent(hotspotEvent));
      dbClient.pushEventDao().insert(dbSession, eventDto);
      dbSession.commit();
    }
  }

  private static byte[] serializeIssueToPushEvent(HotspotChangedEvent event) {
    return GSON.toJson(event).getBytes(UTF_8);
  }


}
