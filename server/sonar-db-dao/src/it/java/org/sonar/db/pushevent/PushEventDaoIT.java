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
package org.sonar.db.pushevent;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class PushEventDaoIT {

  private final TestSystem2 system2 = new TestSystem2().setNow(1L);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession session = db.getSession();
  private final PushEventDao underTest = db.getDbClient().pushEventDao();

  @Test
  void insert_events() {
    assertThat(db.countRowsOfTable(session, "push_events")).isZero();

    PushEventDto eventDtoFirst = new PushEventDto()
      .setUuid("test-uuid")
      .setName("Event")
      .setProjectUuid("project-uuid")
      .setPayload("some-event".getBytes(UTF_8));

    PushEventDto eventDtoSecond = new PushEventDto()
      .setProjectUuid("project-uuid")
      .setName("Event")

      .setPayload("some-event".getBytes(UTF_8));

    underTest.insert(session, eventDtoFirst);
    var generatedUuid = underTest.insert(session, eventDtoSecond);

    assertThat(db.countRowsOfTable(session, "push_events"))
      .isEqualTo(2);

    assertThat(underTest.selectByUuid(session, "test-uuid"))
      .extracting(PushEventDto::getUuid, PushEventDto::getProjectUuid, PushEventDto::getPayload, PushEventDto::getCreatedAt)
      .containsExactly(eventDtoFirst.getUuid(), eventDtoFirst.getProjectUuid(), eventDtoFirst.getPayload(), eventDtoFirst.getCreatedAt());

    assertThat(underTest.selectByUuid(session, generatedUuid.getUuid()))
      .extracting(PushEventDto::getUuid, PushEventDto::getProjectUuid, PushEventDto::getPayload, PushEventDto::getCreatedAt)
      .containsExactly(eventDtoSecond.getUuid(), eventDtoSecond.getProjectUuid(), eventDtoSecond.getPayload(),
        eventDtoSecond.getCreatedAt());

  }

  @Test
  void select_expired_events() {
    PushEventDto eventDtoFirst = new PushEventDto()
      .setName("Event")
      .setProjectUuid("project-uuid")
      .setCreatedAt(1000L)
      .setPayload("some-event".getBytes(UTF_8));

    PushEventDto eventDtoSecond = new PushEventDto()
      .setName("Event")
      .setProjectUuid("project-uuid")
      .setCreatedAt(1000L)
      .setPayload("some-event".getBytes(UTF_8));

    PushEventDto eventDtoThird = new PushEventDto()
      .setName("Event")
      .setProjectUuid("project-uuid")
      .setCreatedAt(2000L)
      .setPayload("some-event".getBytes(UTF_8));

    underTest.insert(session, eventDtoFirst);
    underTest.insert(session, eventDtoSecond);
    underTest.insert(session, eventDtoThird);

    assertThat(underTest.selectUuidsOfExpiredEvents(session, 2000L)).hasSize(3);
    assertThat(underTest.selectUuidsOfExpiredEvents(session, 1500L)).hasSize(2);
    assertThat(underTest.selectUuidsOfExpiredEvents(session, 150L)).isEmpty();
  }

  @Test
  void delete_events_in_batches() {
    PushEventDto eventDtoFirst = new PushEventDto()
      .setName("Event")
      .setProjectUuid("project-uuid")
      .setCreatedAt(1000L)
      .setPayload("some-event".getBytes(UTF_8));

    PushEventDto eventDtoSecond = new PushEventDto()
      .setName("Event")
      .setProjectUuid("project-uuid")
      .setCreatedAt(1000L)
      .setPayload("some-event".getBytes(UTF_8));

    PushEventDto event1 = underTest.insert(session, eventDtoFirst);
    PushEventDto event2 = underTest.insert(session, eventDtoSecond);

    assertThat(underTest.selectUuidsOfExpiredEvents(db.getSession(), 2000L)).hasSize(2);
    underTest.deleteByUuids(db.getSession(), Set.of(event1.getUuid(), event2.getUuid()));
    assertThat(underTest.selectUuidsOfExpiredEvents(db.getSession(), 2000L)).isEmpty();
  }

  @Test
  void selectChunkByProjectKeys() {
    system2.setNow(1L);
    generatePushEvent("proj1");
    system2.tick(); // tick=2
    generatePushEvent("proj2");

    system2.tick(); // tick=3
    var eventDto4 = generatePushEvent("proj2");

    var events = underTest.selectChunkByProjectUuids(session, Set.of("proj1", "proj2"), 2L, null, 10);

    // tick=1 and tick=2 skipped
    assertThat(events).extracting(PushEventDto::getUuid).containsExactly(eventDto4.getUuid());

    system2.tick(); // tick=4
    var eventDto5 = generatePushEvent("proj2");
    var eventDto6 = generatePushEvent("proj2");

    system2.tick(); // tick =5
    var eventDto7 = generatePushEvent("proj2");

    events = underTest.selectChunkByProjectUuids(session, Set.of("proj1", "proj2"), eventDto4.getCreatedAt(), eventDto4.getUuid(), 10);
    List<String> sortedUuids = Stream.of(eventDto5, eventDto6, eventDto7)
      .sorted(Comparator.comparing(PushEventDto::getCreatedAt).thenComparing(PushEventDto::getUuid))
      .map(PushEventDto::getUuid)
      .toList();

    assertThat(events).extracting(PushEventDto::getUuid).containsExactlyElementsOf(sortedUuids);
  }

  @Test
  void selectChunkByProjectKeys_pagination() {
    system2.setNow(3L);

    IntStream.range(1, 10)
      .forEach(value -> generatePushEvent("event-" + value, "proj1"));

    var events = underTest.selectChunkByProjectUuids(session, Set.of("proj1"), 1L, null, 3);
    assertThat(events).extracting(PushEventDto::getUuid).containsExactly("event-1", "event-2", "event-3");

    events = underTest.selectChunkByProjectUuids(session, Set.of("proj1"), 3L, "event-3", 3);
    assertThat(events).extracting(PushEventDto::getUuid).containsExactly("event-4", "event-5", "event-6");
  }

  private PushEventDto generatePushEvent(String projectUuid) {
    return generatePushEvent(UuidFactoryFast.getInstance().create(), projectUuid);
  }

  private PushEventDto generatePushEvent(String uuid, String projectUuid) {
    return underTest.insert(session, new PushEventDto()
      .setName("Event")
      .setUuid(uuid)
      .setProjectUuid(projectUuid)
      .setPayload("some-event".getBytes(UTF_8)));
  }

}
