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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.server.platform.NodeInformation;
import org.sonarsource.sonarqube.events.api.BaseEvent;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSource;
import org.sonarsource.sonarqube.events.api.ExecutingProcess;
import org.sonarsource.sonarqube.events.api.Task;
import org.sonarsource.sonarqube.events.api.TaskHandler;
import org.sonarsource.sonarqube.events.server.EventDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventsIT {

  private record TestPayload(String message, int count) {}

  private final TestSystem2 system2 = new TestSystem2().setNow(1_000_000L);

  @RegisterExtension
  public DbTester db = DbTester.create(system2);

  /**
   * An event published in the Web process is handled by a Web-side TaskHandler in the same process.
   */
  @Test
  void webHandler_receivesEventPublishedInWebProcess() {
    RecordingHandler<TestPayload> webHandler = new RecordingHandler<>("test.event", TestPayload.class, ExecutingProcess.SERVER);
    EventDispatcher dispatcher = new EventDispatcher(ExecutingProcess.SERVER, List.of(webHandler), null);

    dispatcher.dispatch(testEvent("test.event", new TestPayload("hello from web", 1)));

    assertThat(webHandler.receivedPayloads()).containsExactly(new TestPayload("hello from web", 1));
  }

  /**
   * An event published in the Web process is forwarded to CE via CeQueue, then deserialized and
   * dispatched to a CE-side TaskHandler — testing the full cross-process round-trip in a single JVM.
   */
  @Test
  void ceHandler_receivesEventPublishedInWebProcess() {
    RecordingHandler<TestPayload> ceHandler = new RecordingHandler<>("test.event", TestPayload.class, ExecutingProcess.COMPUTE_ENGINE);

    // Web side: dispatcher with forwarder
    CeQueue ceQueue = mock(CeQueue.class);
    when(ceQueue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder("task-uuid"));
    CeQueueEventForwarder forwarder = new CeQueueEventForwarder(ceQueue);
    EventDispatcher webDispatcher = new EventDispatcher(ExecutingProcess.SERVER, List.of(ceHandler), forwarder);

    // CE side: dispatcher + processor
    EventDispatcher ceDispatcher = new EventDispatcher(ExecutingProcess.COMPUTE_ENGINE, List.of(ceHandler), null);
    CeEventTaskProcessor processor = new CeEventTaskProcessor(ceDispatcher, List.of(ceHandler));

    // Publish from Web
    webDispatcher.dispatch(testEvent("test.event", new TestPayload("hello from web", 42)));

    // Simulate CE picking up the queued task
    ArgumentCaptor<CeTaskSubmit> captor = ArgumentCaptor.forClass(CeTaskSubmit.class);
    verify(ceQueue).submit(captor.capture());
    processor.process(toCeTask(captor.getValue()));

    assertThat(ceHandler.receivedPayloads()).containsExactly(new TestPayload("hello from web", 42));
  }

  /**
   * An event published in the Web process targeting a CE handler is correctly persisted in the
   * CE queue DB table with the right task type and serialized event characteristics.
   */
  @Test
  void publishingEventInWebProcess_storesTaskInCeQueueWithCorrectTypeAndCharacteristics() {
    CeQueue ceQueue = new CeQueueImpl(system2, db.getDbClient(), new SequenceUuidFactory(), mock(NodeInformation.class));
    CeQueueEventForwarder forwarder = new CeQueueEventForwarder(ceQueue);
    TaskHandler<TestPayload> ceHandler = stubCeHandlerFor("my.event.type", TestPayload.class);
    EventDispatcher webDispatcher = new EventDispatcher(ExecutingProcess.SERVER, List.of(ceHandler), forwarder);

    EventSource source = new EventSource("Analysis", "IssueService", null, null, null);
    EventMetadata metadata = new EventMetadata("event-id-1", source, "my.event.type", 123456L, "1.0");
    webDispatcher.dispatch(new BaseEvent<>(metadata, new TestPayload("hello world", 7)));

    List<CeQueueDto> tasks = db.getDbClient().ceQueueDao().selectAllInAscOrder(db.getSession());
    assertThat(tasks).hasSize(1);
    CeQueueDto task = tasks.getFirst();
    assertThat(task.getTaskType()).isEqualTo("my.event.type");

    Map<String, String> characteristics = db.getDbClient().ceTaskCharacteristicsDao()
      .selectByTaskUuids(db.getSession(), List.of(task.getUuid()))
      .stream()
      .collect(Collectors.toMap(CeTaskCharacteristicDto::getKey, CeTaskCharacteristicDto::getValue));

    assertThat(characteristics.get(CeQueueEventForwarder.PAYLOAD_KEY))
      .contains("\"message\":\"hello world\"")
      .contains("\"count\":7");
    assertThat(characteristics.get(CeQueueEventForwarder.METADATA_KEY))
      .contains("my.event.type")
      .contains("event-id-1");
  }

  // -- helpers --

  private static <T> Event<T> testEvent(String eventType, T payload) {
    EventSource source = new EventSource("Analysis", "TestService", null, null, null);
    EventMetadata metadata = new EventMetadata(source, eventType, "1.0");
    return new BaseEvent<>(metadata, payload);
  }

  private static CeTask toCeTask(CeTaskSubmit submission) {
    return new CeTask.Builder()
      .setUuid(submission.getUuid())
      .setType(submission.getType())
      .setCharacteristics(submission.getCharacteristics())
      .build();
  }

  private static <T> TaskHandler<T> stubCeHandlerFor(String eventName, Class<T> payloadType) {
    return new TaskHandler<>() {
      @Override public String getHandledEventName() { return eventName; }
      @Override public Class<T> getPayloadType() { return payloadType; }
      @Override public ExecutingProcess getServerExecutingProcess() { return ExecutingProcess.COMPUTE_ENGINE; }
      @Override public Task<T> getTask() {
        return new Task<>() {
          @Override public String getTaskType() { return eventName; }
          @Override public void execute(T payload) { /* stub — not invoked in this test */ }
        };
      }
    };
  }

  private static class RecordingHandler<T> implements TaskHandler<T> {
    private final String eventName;
    private final Class<T> payloadType;
    private final ExecutingProcess process;
    private final List<T> payloads = new ArrayList<>();

    RecordingHandler(String eventName, Class<T> payloadType, ExecutingProcess process) {
      this.eventName = eventName;
      this.payloadType = payloadType;
      this.process = process;
    }

    @Override
    public String getHandledEventName() {
      return eventName;
    }

    @Override
    public Class<T> getPayloadType() {
      return payloadType;
    }

    @Override
    public ExecutingProcess getServerExecutingProcess() {
      return process;
    }

    @Override
    public Task<T> getTask() {
      return new Task<>() {
        @Override
        public String getTaskType() {
          return eventName;
        }

        @Override
        public void execute(T payload) {
          payloads.add(payload);
        }
      };
    }

    List<T> receivedPayloads() {
      return payloads;
    }
  }
}
