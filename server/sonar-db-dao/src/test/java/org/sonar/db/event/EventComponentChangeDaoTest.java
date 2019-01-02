/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.event.EventComponentChangeDto.ChangeCategory;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventComponentChangeDaoTest {
  private static Random random = new Random();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();

  private System2 system2 = mock(System2.class);
  private EventComponentChangeDao underTest = new EventComponentChangeDao(system2);

  @Test
  public void selectByEventUuid_on_empty_table() {
    assertThat(underTest.selectByEventUuid(dbSession, randomAlphabetic(10)))
      .isEmpty();
  }

  @Test
  public void selectByEventUuid_maps_columns_correctly() {
    String eventBase = randomAlphabetic(5);
    String rowBase = randomAlphabetic(6);
    String eventUuid = eventBase + "_event_uuid";
    String uuid = rowBase + "_uuid";
    EventComponentChangeDto dto = new EventComponentChangeDto()
      .setCategory(randomChangeCategory())
      .setUuid(uuid)
      .setEventUuid(eventUuid)
      .setComponentUuid(rowBase + "_component_uuid")
      .setComponentKey(rowBase + "_component_key")
      .setComponentName(rowBase + "_component_name")
      .setComponentBranchKey(rowBase + "_component_branch_key");
    EventPurgeData purgeData = new EventPurgeData(eventBase + "_component_uuid", eventBase + "_analysis_uuid");
    long now = random.nextLong();
    when(system2.now()).thenReturn(now);

    underTest.insert(dbSession, dto, purgeData);

    assertThat(underTest.selectByEventUuid(dbSession, uuid))
      .isEmpty();
    assertThat(underTest.selectByEventUuid(dbSession, eventUuid))
      .extracting(
        EventComponentChangeDto::getUuid,
        EventComponentChangeDto::getCategory,
        EventComponentChangeDto::getEventUuid,
        EventComponentChangeDto::getComponentUuid,
        EventComponentChangeDto::getComponentKey,
        EventComponentChangeDto::getComponentName,
        EventComponentChangeDto::getComponentBranchKey,
        EventComponentChangeDto::getCreatedAt)
      .containsOnly(tuple(
        dto.getUuid(),
        dto.getCategory(),
        dto.getEventUuid(),
        dto.getComponentUuid(),
        dto.getComponentKey(),
        dto.getComponentName(),
        dto.getComponentBranchKey(),
        now));
  }

  @Test
  public void selectByAnalysisUuids_maps_columns_correctly() {
    String eventBase = randomAlphabetic(5);
    String rowBase = randomAlphabetic(6);
    String eventUuid = eventBase + "_event_uuid";
    String uuid = rowBase + "_uuid";
    EventComponentChangeDto dto = new EventComponentChangeDto()
      .setCategory(randomChangeCategory())
      .setUuid(uuid)
      .setEventUuid(eventUuid)
      .setComponentUuid(rowBase + "_component_uuid")
      .setComponentKey(rowBase + "_component_key")
      .setComponentName(rowBase + "_component_name")
      .setComponentBranchKey(rowBase + "_component_branch_key");
    EventPurgeData purgeData = new EventPurgeData(eventBase + "_component_uuid", eventBase + "_analysis_uuid");
    long now = random.nextLong();
    when(system2.now()).thenReturn(now);

    underTest.insert(dbSession, dto, purgeData);

    assertThat(underTest.selectByAnalysisUuids(dbSession, Collections.emptyList()))
      .isEmpty();
    assertThat(underTest.selectByAnalysisUuids(dbSession, singletonList(eventBase + "_analysis_uuid")))
      .extracting(
        EventComponentChangeDto::getUuid,
        EventComponentChangeDto::getCategory,
        EventComponentChangeDto::getEventUuid,
        EventComponentChangeDto::getComponentUuid,
        EventComponentChangeDto::getComponentKey,
        EventComponentChangeDto::getComponentName,
        EventComponentChangeDto::getComponentBranchKey,
        EventComponentChangeDto::getCreatedAt)
      .containsOnly(tuple(
        dto.getUuid(),
        dto.getCategory(),
        dto.getEventUuid(),
        dto.getComponentUuid(),
        dto.getComponentKey(),
        dto.getComponentName(),
        dto.getComponentBranchKey(),
        now));
  }

  @Test
  public void selectByEventUuid_branchKey_can_be_null() {
    String eventBase = randomAlphabetic(5);
    String rowBase = randomAlphabetic(6);
    String eventUuid = eventBase + "_event_uuid";
    EventComponentChangeDto dto = new EventComponentChangeDto()
      .setCategory(randomChangeCategory())
      .setUuid(rowBase + "_uuid")
      .setEventUuid(eventUuid)
      .setComponentUuid(rowBase + "_component_uuid")
      .setComponentKey(rowBase + "_component_key")
      .setComponentName(rowBase + "_component_name")
      .setComponentBranchKey(null);
    EventPurgeData purgeData = new EventPurgeData(eventBase + "_component_uuid", eventBase + "_analysis_uuid");
    long now = random.nextLong();
    when(system2.now())
      .thenReturn(now)
      .thenThrow(new IllegalStateException("now should not be called twice"));

    underTest.insert(dbSession, dto, purgeData);

    assertThat(underTest.selectByEventUuid(dbSession, eventUuid))
      .extracting(EventComponentChangeDto::getUuid, EventComponentChangeDto::getComponentBranchKey)
      .containsOnly(tuple(dto.getUuid(), null));
  }

  @Test
  public void selectByEventUuid_returns_all_rows_for_specified_event() {
    String eventBase = randomAlphabetic(5);
    String rowBase = randomAlphabetic(6);
    String eventUuid1 = eventBase + "_event_uuid1";
    String eventUuid2 = eventBase + "_event_uuid2";
    EventComponentChangeDto[] event1Dtos = IntStream.range(0, 3)
      .mapToObj(i -> new EventComponentChangeDto()
        .setCategory(randomChangeCategory())
        .setUuid(rowBase + eventUuid1 + i)
        .setEventUuid(eventUuid1)
        .setComponentUuid(rowBase + eventUuid1 + "_component_uuid" + i)
        .setComponentKey(rowBase + "_component_key")
        .setComponentName(rowBase + "_component_name")
        .setComponentBranchKey(null))
      .toArray(EventComponentChangeDto[]::new);
    EventComponentChangeDto[] event2Dtos = IntStream.range(0, 2)
      .mapToObj(i -> new EventComponentChangeDto()
        .setCategory(randomChangeCategory())
        .setUuid(rowBase + eventUuid2 + i)
        .setEventUuid(eventUuid2)
        .setComponentUuid(rowBase + eventUuid2 + "_component_uuid" + i)
        .setComponentKey(rowBase + "_component_key")
        .setComponentName(rowBase + "_component_name")
        .setComponentBranchKey(null))
      .toArray(EventComponentChangeDto[]::new);
    EventPurgeData doesNotMatter = new EventPurgeData(randomAlphabetic(7), randomAlphabetic(8));
    long now = random.nextLong();
    when(system2.now()).thenReturn(now)
      .thenReturn(now + 1)
      .thenReturn(now + 2)
      .thenReturn(now + 3)
      .thenReturn(now + 4)
      .thenThrow(new IllegalStateException("now should not be called 6 times"));

    Arrays.stream(event1Dtos).forEach(dto -> underTest.insert(dbSession, dto, doesNotMatter));
    Arrays.stream(event2Dtos).forEach(dto -> underTest.insert(dbSession, dto, doesNotMatter));

    assertThat(underTest.selectByEventUuid(dbSession, eventUuid1))
      .extracting(
        EventComponentChangeDto::getUuid,
        EventComponentChangeDto::getEventUuid,
        EventComponentChangeDto::getCreatedAt)
      .containsOnly(
        tuple(
          event1Dtos[0].getUuid(),
          eventUuid1,
          now),
        tuple(
          event1Dtos[1].getUuid(),
          eventUuid1,
          now + 1),
        tuple(
          event1Dtos[2].getUuid(),
          eventUuid1,
          now + 2));
    assertThat(underTest.selectByEventUuid(dbSession, eventUuid2))
      .extracting(
        EventComponentChangeDto::getUuid,
        EventComponentChangeDto::getEventUuid,
        EventComponentChangeDto::getCreatedAt)
      .containsOnly(
        tuple(
          event2Dtos[0].getUuid(),
          eventUuid2,
          now + 3),
        tuple(
          event2Dtos[1].getUuid(),
          eventUuid2,
          now + 4));
  }

  @Test
  public void selectByAnalysisUuids_returns_all_rows_for_specified_event() {
    String eventBase = randomAlphabetic(5);
    String rowBase = randomAlphabetic(6);
    String eventUuid1 = eventBase + "_event_uuid1";
    String eventUuid2 = eventBase + "_event_uuid2";
    EventComponentChangeDto[] event1Dtos = IntStream.range(0, 3)
      .mapToObj(i -> new EventComponentChangeDto()
        .setCategory(randomChangeCategory())
        .setUuid(rowBase + eventUuid1 + i)
        .setEventUuid(eventUuid1)
        .setComponentUuid(rowBase + eventUuid1 + "_component_uuid" + i)
        .setComponentKey(rowBase + "_component_key")
        .setComponentName(rowBase + "_component_name")
        .setComponentBranchKey(null))
      .toArray(EventComponentChangeDto[]::new);
    EventComponentChangeDto[] event2Dtos = IntStream.range(0, 2)
      .mapToObj(i -> new EventComponentChangeDto()
        .setCategory(randomChangeCategory())
        .setUuid(rowBase + eventUuid2 + i)
        .setEventUuid(eventUuid2)
        .setComponentUuid(rowBase + eventUuid2 + "_component_uuid" + i)
        .setComponentKey(rowBase + "_component_key")
        .setComponentName(rowBase + "_component_name")
        .setComponentBranchKey(null))
      .toArray(EventComponentChangeDto[]::new);
    EventPurgeData doesNotMatter = new EventPurgeData(randomAlphabetic(7), randomAlphabetic(8));
    long now = random.nextLong();
    when(system2.now()).thenReturn(now)
      .thenReturn(now + 1)
      .thenReturn(now + 2)
      .thenReturn(now + 3)
      .thenReturn(now + 4)
      .thenThrow(new IllegalStateException("now should not be called 6 times"));

    Arrays.stream(event1Dtos).forEach(dto -> underTest.insert(dbSession, dto, doesNotMatter));
    Arrays.stream(event2Dtos).forEach(dto -> underTest.insert(dbSession, dto, doesNotMatter));

    assertThat(underTest.selectByAnalysisUuids(dbSession, singletonList(doesNotMatter.getAnalysisUuid())))
      .extracting(
        EventComponentChangeDto::getUuid,
        EventComponentChangeDto::getEventUuid,
        EventComponentChangeDto::getCreatedAt)
      .containsOnly(
        tuple(
          event1Dtos[0].getUuid(),
          eventUuid1,
          now),
        tuple(
          event1Dtos[1].getUuid(),
          eventUuid1,
          now + 1),
        tuple(
          event1Dtos[2].getUuid(),
          eventUuid1,
          now + 2),
        tuple(
          event2Dtos[0].getUuid(),
          eventUuid2,
          now + 3),
        tuple(
          event2Dtos[1].getUuid(),
          eventUuid2,
          now + 4));
  }

  private static ChangeCategory randomChangeCategory() {
    return ChangeCategory.values()[random.nextInt(ChangeCategory.values().length)];
  }
}
