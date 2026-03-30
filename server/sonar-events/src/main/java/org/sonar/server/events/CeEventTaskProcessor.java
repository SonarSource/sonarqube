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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;
import org.sonarsource.sonarqube.events.api.BaseEvent;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.ExecutingProcess;
import org.sonarsource.sonarqube.events.api.TaskHandler;
import org.sonarsource.sonarqube.events.server.EventDispatcher;

import static java.util.stream.Collectors.toMap;

/**
 * CE-side {@link CeTaskProcessor} that deserializes unified events forwarded from the Web
 * process and dispatches them to CE-targeted {@link TaskHandler} listeners.
 *
 * <p>The event payload and metadata are stored as CE task characteristics by
 * {@link CeQueueEventForwarder}. This processor reads those characteristics, deserializes
 * them using Jackson, and calls {@link EventDispatcher#dispatchLocally(org.sonarsource.sonarqube.events.api.Event)}
 * to avoid re-forwarding.
 *
 * <p>All registered {@link TaskHandler} beans are passed in; only those declaring
 * {@link ExecutingProcess#COMPUTE_ENGINE} are used to determine handled task types.
 */
public class CeEventTaskProcessor implements CeTaskProcessor {

  private final EventDispatcher dispatcher;
  private final Map<String, Class<?>> payloadTypeByEventType;
  private final ObjectMapper objectMapper = EventsObjectMapper.create();

  public CeEventTaskProcessor(
    EventDispatcher dispatcher,
    Collection<? extends TaskHandler<?>> allListeners) {
    this.dispatcher = dispatcher;
    this.payloadTypeByEventType = allListeners.stream()
      .filter(l -> l.getServerExecutingProcess() == ExecutingProcess.COMPUTE_ENGINE)
      .collect(toMap(
        TaskHandler::getHandledEventName,
        TaskHandler::getPayloadType,
        (a, b) -> {
          if (!a.equals(b)) {
            throw new IllegalStateException(
              "Conflicting payload types for the same event name: " + a + " vs " + b);
          }
          return a;
        }
      ));
  }

  @Override
  public Set<String> getHandledCeTaskTypes() {
    return payloadTypeByEventType.keySet();
  }

  @Override
  @CheckForNull
  public CeTaskResult process(CeTask task) {
    Map<String, String> characteristics = task.getCharacteristics();
    try {
      String payloadJson = characteristics.get(CeQueueEventForwarder.PAYLOAD_KEY);
      String metadataJson = characteristics.get(CeQueueEventForwarder.METADATA_KEY);
      Class<?> payloadType = payloadTypeByEventType.get(task.getType());
      if (payloadJson == null || metadataJson == null || payloadType == null) {
        throw new EventProcessingException(
          "Missing characteristics for event task of type: " + task.getType());
      }
      Object payload = objectMapper.readValue(payloadJson, payloadType);
      EventMetadata metadata = objectMapper.readValue(metadataJson, EventMetadata.class);
      dispatcher.dispatchLocally(new BaseEvent<>(metadata, payload));
    } catch (JsonProcessingException e) {
      throw new EventProcessingException("Failed to process unified event task", e);
    }
    return null;
  }
}
