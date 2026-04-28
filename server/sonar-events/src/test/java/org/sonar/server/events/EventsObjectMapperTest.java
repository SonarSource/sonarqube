/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSource;

import static org.assertj.core.api.Assertions.assertThat;

class EventsObjectMapperTest {

  @Test
  void create_returnsNewMapperInstance() {
    ObjectMapper first = EventsObjectMapper.create();
    ObjectMapper second = EventsObjectMapper.create();

    assertThat(first).isNotNull().isNotSameAs(second);
  }

  @Test
  void create_mapperCanRoundTripEventMetadata() throws Exception {
    ObjectMapper mapper = EventsObjectMapper.create();
    EventSource source = new EventSource("Analysis", "IssueService", null, null, null);
    EventMetadata original = new EventMetadata("evt-id", source, "my.event", 123456L, "1.0");

    String json = mapper.writeValueAsString(original);
    EventMetadata deserialized = mapper.readValue(json, EventMetadata.class);

    assertThat(deserialized.eventId()).isEqualTo(original.eventId());
    assertThat(deserialized.eventType()).isEqualTo(original.eventType());
    assertThat(deserialized.eventTimestamp()).isEqualTo(original.eventTimestamp());
    assertThat(deserialized.eventVersion()).isEqualTo(original.eventVersion());
    assertThat(deserialized.source().domain()).isEqualTo(original.source().domain());
    assertThat(deserialized.source().service()).isEqualTo(original.source().service());
  }
}
