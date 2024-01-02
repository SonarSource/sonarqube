/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Deque;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.pushevent.PushEventDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.test.JsonAssert.assertJson;

public class HotspotChangeEventServiceImplTest {

  @Rule
  public DbTester db = DbTester.create();

  public final HotspotChangeEventServiceImpl underTest = new HotspotChangeEventServiceImpl(db.getDbClient());

  @Test
  public void distributeHotspotChangedEvent_whenCalled_shouldPersistCorrectEventData() {
    HotspotChangedEvent hotspotChangedEvent = new HotspotChangedEvent.Builder()
      .setKey("key")
      .setProjectKey("project-key")
      .setUpdateDate(new Date(1L))
      .setStatus("REVIEWED")
      .setResolution("ACKNOWLEDGED")
      .setAssignee("assignee")
      .setFilePath("path/to/file")
      .build();
    assertPushEventIsPersisted(hotspotChangedEvent);
  }

  private void assertPushEventIsPersisted(HotspotChangedEvent hotspotChangedEvent) {
    underTest.distributeHotspotChangedEvent("project-uuid", hotspotChangedEvent);

    Deque<PushEventDto> events = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of("project-uuid"), 1L, null, 1);
    assertThat(events).isNotEmpty();
    assertThat(events).extracting(PushEventDto::getName, PushEventDto::getProjectUuid)
      .contains(tuple("SecurityHotspotChanged", "project-uuid"));

    String payload = new String(events.getLast().getPayload(), StandardCharsets.UTF_8);
    assertJson(payload).isSimilarTo("{" +
      "\"key\": \"" + hotspotChangedEvent.getKey() + "\"," +
      "\"projectKey\": \"" + hotspotChangedEvent.getProjectKey() + "\"," +
      "\"updateDate\": " + hotspotChangedEvent.getUpdateDate() + "," +
      "\"status\": \"" + hotspotChangedEvent.getStatus() + "\"," +
      "\"filePath\": \"" + hotspotChangedEvent.getFilePath() + "\"," +
      "\"assignee\": \"" + hotspotChangedEvent.getAssignee() + "\"," +
      "\"resolution\": \"" + hotspotChangedEvent.getResolution() + "\"" +
      "}");
  }

}
