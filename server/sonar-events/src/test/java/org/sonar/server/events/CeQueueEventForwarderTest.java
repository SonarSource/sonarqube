/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonarsource.sonarqube.events.api.BaseEvent;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CeQueueEventForwarderTest {

  @Mock
  private CeQueue ceQueue;

  private CeQueueEventForwarder underTest;

  @BeforeEach
  void setUp() {
    underTest = new CeQueueEventForwarder(ceQueue);
  }

  @Test
  void forward_submitsTaskWithCorrectTypeAndSerializedCharacteristics() {
    when(ceQueue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder("test-uuid"));

    EventSource source = new EventSource("Analysis", "IssueService", null, null, null);
    EventMetadata metadata = new EventMetadata("event-id-1", source, "my.event.type", 123456L, "1.0");
    BaseEvent<String> event = new BaseEvent<>(metadata, "hello world");

    underTest.forward(event);

    ArgumentCaptor<CeTaskSubmit> captor = ArgumentCaptor.forClass(CeTaskSubmit.class);
    verify(ceQueue).submit(captor.capture());

    CeTaskSubmit submission = captor.getValue();
    assertThat(submission.getType()).isEqualTo("my.event.type");
    assertThat(submission.getCharacteristics())
      .containsKey(CeQueueEventForwarder.PAYLOAD_KEY)
      .containsKey(CeQueueEventForwarder.METADATA_KEY);
    assertThat(submission.getCharacteristics()).containsEntry(CeQueueEventForwarder.PAYLOAD_KEY, "\"hello world\"");
    assertThat(submission.getCharacteristics().get(CeQueueEventForwarder.METADATA_KEY))
      .contains("my.event.type")
      .contains("event-id-1");
  }

  @Test
  void forward_throwsEventProcessingException_whenPayloadCannotBeSerialized() {
    EventSource source = new EventSource("Analysis", "IssueService", null, null, null);
    EventMetadata metadata = new EventMetadata("event-id-2", source, "my.event.type", 123456L, "1.0");
    // An object with a circular reference that Jackson cannot serialize
    Object unserializablePayload = new Object() {
      @SuppressWarnings("unused")
      public Object getSelf() {
        return this;
      }
    };
    BaseEvent<Object> event = new BaseEvent<>(metadata, unserializablePayload);

    assertThatThrownBy(() -> underTest.forward(event))
      .isInstanceOf(EventProcessingException.class)
      .hasMessageContaining("Failed to forward event to CE queue");
  }
}
