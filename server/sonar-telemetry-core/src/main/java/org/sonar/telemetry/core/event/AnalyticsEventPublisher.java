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
package org.sonar.telemetry.core.event;

import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonarsource.sonarqube.events.api.BaseEvent;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventAsyncClient;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSourceBuilder;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

/**
 * Publishes {@code Analytics.*} cross-domain events onto the unified event bus. Fire-and-forget:
 * publication failures are logged and never propagated to the caller, so telemetry can never break
 * an analysis or a request.
 */
public class AnalyticsEventPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyticsEventPublisher.class);

  private final EventAsyncClient eventAsyncClient;
  private final EventSourceBuilder eventSourceBuilder;
  private final Configuration configuration;

  public AnalyticsEventPublisher(EventAsyncClient eventAsyncClient, EventSourceBuilder eventSourceBuilder, Configuration configuration) {
    this.eventAsyncClient = eventAsyncClient;
    this.eventSourceBuilder = eventSourceBuilder;
    this.configuration = configuration;
  }

  /**
   * Publishes a single event. No-op when telemetry is disabled.
   */
  public void publish(AnalyticsEventType type, Object payload) {
    if (!isTelemetryEnabled()) {
      return;
    }
    try {
      eventAsyncClient.publishCrossDomainEvent(toEvent(type, payload))
        .whenComplete((ignored, throwable) -> {
          if (throwable != null) {
            LOG.warn("Failed to publish {} telemetry event", type.eventType(), throwable);
          }
        });
    } catch (RuntimeException e) {
      LOG.warn("Failed to publish {} telemetry event", type.eventType(), e);
    }
  }

  /**
   * Publishes each payload as its own event, in a single batch. No-op on empty input or when
   * telemetry is disabled.
   */
  public void publishAll(AnalyticsEventType type, Collection<?> payloads) {
    if (payloads.isEmpty() || !isTelemetryEnabled()) {
      return;
    }
    try {
      List<Event<?>> events = payloads.stream()
        .<Event<?>>map(payload -> toEvent(type, payload))
        .toList();
      eventAsyncClient.publishCrossDomainEvents(events)
        .whenComplete((ignored, throwable) -> {
          if (throwable != null) {
            LOG.warn("Failed to publish {} telemetry events", type.eventType(), throwable);
          }
        });
    } catch (RuntimeException e) {
      LOG.warn("Failed to publish {} telemetry events", type.eventType(), e);
    }
  }

  private Event<?> toEvent(AnalyticsEventType type, Object payload) {
    return new BaseEvent<>(
      new EventMetadata(
        eventSourceBuilder.build(type.sourceDomain(), type.sourceService()),
        type.eventType(),
        type.eventVersion()),
      payload);
  }

  private boolean isTelemetryEnabled() {
    return configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false);
  }
}
