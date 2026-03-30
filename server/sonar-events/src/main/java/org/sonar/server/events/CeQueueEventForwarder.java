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
import java.util.Map;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.server.CrossProcessEventForwarder;

/**
 * Forwards events from the Web process to the Compute Engine by submitting
 * them as CE tasks via {@link CeQueue}.
 *
 * <p>The event payload and metadata are serialized to JSON and stored as CE task
 * characteristics. {@link UnifiedEventCeTaskProcessor} deserializes them on the CE side.
 */
public class CeQueueEventForwarder implements CrossProcessEventForwarder {

  static final String PAYLOAD_KEY = "eventPayloadJson";
  static final String METADATA_KEY = "eventMetadataJson";

  private final CeQueue ceQueue;
  private final ObjectMapper objectMapper = EventsObjectMapper.create();

  public CeQueueEventForwarder(CeQueue ceQueue) {
    this.ceQueue = ceQueue;
  }

  @Override
  public void forward(Event<?> event) {
    try {
      String payloadJson = objectMapper.writeValueAsString(event.payload());
      String metadataJson = objectMapper.writeValueAsString(event.metadata());
      CeTaskSubmit submission = ceQueue.prepareSubmit()
        .setType(event.metadata().eventType())
        .setCharacteristics(Map.of(PAYLOAD_KEY, payloadJson, METADATA_KEY, metadataJson))
        .build();
      ceQueue.submit(submission);
    } catch (JsonProcessingException e) {
      throw new EventProcessingException("Failed to forward event to CE queue", e);
    }
  }
}
