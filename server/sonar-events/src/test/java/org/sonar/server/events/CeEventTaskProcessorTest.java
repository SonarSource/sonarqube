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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.ce.task.CeTask;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSource;
import org.sonarsource.sonarqube.events.api.ExecutingProcess;
import org.sonarsource.sonarqube.events.api.TaskHandler;
import org.sonarsource.sonarqube.events.server.EventDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CeEventTaskProcessorTest {

  @Mock
  private EventDispatcher dispatcher;

  @Mock
  private CeTask ceTask;

  // -- constructor / getHandledCeTaskTypes --

  @Test
  void getHandledCeTaskTypes_returnsOnlyCeHandlerEventTypes() {
    TaskHandler<?> ceHandler = ceHandlerFor("ce.event", String.class);
    TaskHandler<?> serverHandler = serverHandlerFor();

    CeEventTaskProcessor underTest = new CeEventTaskProcessor(dispatcher, List.of(ceHandler, serverHandler));

    assertThat(underTest.getHandledCeTaskTypes())
      .containsExactly("ce.event")
      .doesNotContain("server.event");
  }

  @Test
  void constructor_doesNotThrow_whenTwoCeHandlersSameEventNameAndSamePayloadType() {
    TaskHandler<?> handler1 = ceHandlerFor("my.event", String.class);
    TaskHandler<?> handler2 = ceHandlerFor("my.event", String.class);

    assertThatNoException()
      .isThrownBy(() -> new CeEventTaskProcessor(dispatcher, List.of(handler1, handler2)));
  }

  @Test
  void constructor_throwsIllegalStateException_whenTwoCeHandlersSameEventNameWithDifferentPayloadTypes() {
    TaskHandler<?> handler1 = ceHandlerFor("my.event", String.class);
    TaskHandler<?> handler2 = ceHandlerFor("my.event", Integer.class);

    assertThatThrownBy(() -> new CeEventTaskProcessor(dispatcher, List.of(handler1, handler2)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Conflicting payload types");
  }

  // -- process --

  @Test
  void process_deserializesPayloadAndMetadataThenDispatchesLocally() throws JsonProcessingException {
    TaskHandler<?> ceHandler = ceHandlerFor("my.event", String.class);
    CeEventTaskProcessor underTest = new CeEventTaskProcessor(dispatcher, List.of(ceHandler));

    ObjectMapper mapper = EventsObjectMapper.create();
    EventSource source = new EventSource("Analysis", "IssueService", null, null, null);
    EventMetadata metadata = new EventMetadata("event-id-1", source, "my.event", 123456L, "1.0");
    String payloadJson = mapper.writeValueAsString("hello world");
    String metadataJson = mapper.writeValueAsString(metadata);

    when(ceTask.getType()).thenReturn("my.event");
    when(ceTask.getCharacteristics()).thenReturn(Map.of(
      CeQueueEventForwarder.PAYLOAD_KEY, payloadJson,
      CeQueueEventForwarder.METADATA_KEY, metadataJson
    ));

    underTest.process(ceTask);

    ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(dispatcher).dispatchLocally(eventCaptor.capture());

    Event<?> dispatched = eventCaptor.getValue();
    assertThat(dispatched.payload()).isEqualTo("hello world");
    assertThat(dispatched.metadata().eventType()).isEqualTo("my.event");
    assertThat(dispatched.metadata().eventId()).isEqualTo("event-id-1");
  }

  @Test
  void process_returnsNull() throws JsonProcessingException {
    TaskHandler<?> ceHandler = ceHandlerFor("my.event", String.class);
    CeEventTaskProcessor underTest = new CeEventTaskProcessor(dispatcher, List.of(ceHandler));

    ObjectMapper mapper = EventsObjectMapper.create();
    EventSource source = new EventSource("Analysis", "IssueService", null, null, null);
    EventMetadata metadata = new EventMetadata("event-id-2", source, "my.event", 0L, "1.0");

    when(ceTask.getType()).thenReturn("my.event");
    when(ceTask.getCharacteristics()).thenReturn(Map.of(
      CeQueueEventForwarder.PAYLOAD_KEY, mapper.writeValueAsString("payload"),
      CeQueueEventForwarder.METADATA_KEY, mapper.writeValueAsString(metadata)
    ));

    assertThat(underTest.process(ceTask)).isNull();
  }

  @Test
  void process_throwsEventProcessingException_whenPayloadJsonIsInvalid() {
    TaskHandler<?> ceHandler = ceHandlerFor("my.event", String.class);
    CeEventTaskProcessor underTest = new CeEventTaskProcessor(dispatcher, List.of(ceHandler));

    when(ceTask.getType()).thenReturn("my.event");
    when(ceTask.getCharacteristics()).thenReturn(Map.of(
      CeQueueEventForwarder.PAYLOAD_KEY, "not-valid-json-for-string",
      CeQueueEventForwarder.METADATA_KEY, "{}"
    ));

    assertThatThrownBy(() -> underTest.process(ceTask))
      .isInstanceOf(EventProcessingException.class)
      .hasMessageContaining("Failed to process unified event task");
  }

  @Test
  void process_throwsEventProcessingException_whenCharacteristicsAreMissing() {
    TaskHandler<?> ceHandler = ceHandlerFor("my.event", String.class);
    CeEventTaskProcessor underTest = new CeEventTaskProcessor(dispatcher, List.of(ceHandler));

    when(ceTask.getType()).thenReturn("my.event");
    when(ceTask.getCharacteristics()).thenReturn(Map.of());

    assertThatThrownBy(() -> underTest.process(ceTask))
      .isInstanceOf(EventProcessingException.class)
      .hasMessageContaining("Missing characteristics for event task of type: my.event");
  }

  // -- helpers --

  @SuppressWarnings("unchecked")
  private static TaskHandler<?> ceHandlerFor(String eventName, Class<?> payloadType) {
    TaskHandler<?> handler = mock(TaskHandler.class);
    when(handler.getServerExecutingProcess()).thenReturn(ExecutingProcess.COMPUTE_ENGINE);
    when(handler.getHandledEventName()).thenReturn(eventName);
    when(handler.getPayloadType()).thenReturn((Class) payloadType);
    return handler;
  }

  private static TaskHandler<?> serverHandlerFor() {
    TaskHandler<?> handler = mock(TaskHandler.class);
    when(handler.getServerExecutingProcess()).thenReturn(ExecutingProcess.SERVER);
    return handler;
  }
}
