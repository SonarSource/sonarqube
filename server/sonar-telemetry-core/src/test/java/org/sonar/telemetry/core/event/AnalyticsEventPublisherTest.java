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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventAsyncClient;
import org.sonarsource.sonarqube.events.api.EventSource;
import org.sonarsource.sonarqube.events.api.EventSourceBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalyticsEventPublisherTest {

  private static final AnalyticsEventType TYPE = new AnalyticsEventType("Analytics.Workflow.IssueBacklog", "1.0", "Workflow", "IssueTelemetry");
  private static final EventSource EVENT_SOURCE = new EventSource("Workflow", "IssueTelemetry", null, null, null);

  private final EventAsyncClient eventAsyncClient = mock(EventAsyncClient.class);
  private final EventSourceBuilder eventSourceBuilder = mock(EventSourceBuilder.class);
  private final Configuration configuration = mock(Configuration.class);

  private final AnalyticsEventPublisher underTest = new AnalyticsEventPublisher(eventAsyncClient, eventSourceBuilder, configuration);

  @Test
  void publish_whenTelemetryEnabled_publishesSingleEventWithTypeMetadata() {
    givenTelemetryEnabled();
    when(eventSourceBuilder.build(TYPE.sourceDomain(), TYPE.sourceService())).thenReturn(EVENT_SOURCE);
    when(eventAsyncClient.publishCrossDomainEvent(any())).thenReturn(CompletableFuture.completedFuture(null));

    underTest.publish(TYPE, "payload");

    ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventAsyncClient).publishCrossDomainEvent(eventCaptor.capture());
    Event<?> event = eventCaptor.getValue();
    assertThat(event.metadata().eventType()).isEqualTo(TYPE.eventType());
    assertThat(event.metadata().eventVersion()).isEqualTo(TYPE.eventVersion());
    assertThat(event.metadata().source()).isEqualTo(EVENT_SOURCE);
    assertThat(event.payload()).isEqualTo("payload");
  }

  @Test
  void publish_whenTelemetryDisabled_doesNotPublish() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));

    underTest.publish(TYPE, "payload");

    verifyNoInteractions(eventAsyncClient);
  }

  @Test
  void publish_whenPublicationThrows_doesNotPropagate() {
    givenTelemetryEnabled();
    when(eventSourceBuilder.build(TYPE.sourceDomain(), TYPE.sourceService())).thenReturn(EVENT_SOURCE);
    doThrow(new IllegalStateException("boom")).when(eventAsyncClient).publishCrossDomainEvent(any());

    assertThatCode(() -> underTest.publish(TYPE, "payload")).doesNotThrowAnyException();
  }

  @Test
  void publish_whenPublicationCompletesExceptionally_doesNotPropagate() {
    givenTelemetryEnabled();
    when(eventSourceBuilder.build(TYPE.sourceDomain(), TYPE.sourceService())).thenReturn(EVENT_SOURCE);
    when(eventAsyncClient.publishCrossDomainEvent(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));

    assertThatCode(() -> underTest.publish(TYPE, "payload")).doesNotThrowAnyException();
  }

  @Test
  void publishAll_whenTelemetryEnabled_publishesOneEventPerPayload() {
    givenTelemetryEnabled();
    when(eventSourceBuilder.build(TYPE.sourceDomain(), TYPE.sourceService())).thenReturn(EVENT_SOURCE);
    when(eventAsyncClient.publishCrossDomainEvents(any())).thenReturn(CompletableFuture.completedFuture(null));

    underTest.publishAll(TYPE, List.of("payload1", "payload2", "payload3"));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<Event<?>>> eventsCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(eventAsyncClient).publishCrossDomainEvents(eventsCaptor.capture());
    List<Event<?>> events = eventsCaptor.getValue().stream().toList();
    assertThat(events).hasSize(3);
    assertThat(events.stream().map(event -> (Object) event.payload()).toList()).containsExactly("payload1", "payload2", "payload3");
    assertThat(events).allSatisfy(event -> {
      assertThat(event.metadata().eventType()).isEqualTo(TYPE.eventType());
      assertThat(event.metadata().eventVersion()).isEqualTo(TYPE.eventVersion());
    });
  }

  @Test
  void publishAll_whenPayloadsEmpty_doesNotPublish() {
    underTest.publishAll(TYPE, List.of());

    verifyNoInteractions(eventAsyncClient);
  }

  @Test
  void publishAll_whenTelemetryDisabled_doesNotPublish() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));

    underTest.publishAll(TYPE, List.of("payload"));

    verifyNoInteractions(eventAsyncClient);
  }

  @Test
  void publishAll_whenPublicationThrows_doesNotPropagate() {
    givenTelemetryEnabled();
    when(eventSourceBuilder.build(TYPE.sourceDomain(), TYPE.sourceService())).thenReturn(EVENT_SOURCE);
    doThrow(new IllegalStateException("boom")).when(eventAsyncClient).publishCrossDomainEvents(any());

    assertThatCode(() -> underTest.publishAll(TYPE, List.of("payload"))).doesNotThrowAnyException();
  }

  private void givenTelemetryEnabled() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
  }
}
