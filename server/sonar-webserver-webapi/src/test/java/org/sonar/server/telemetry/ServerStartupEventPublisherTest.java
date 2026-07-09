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
package org.sonar.server.telemetry;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventAsyncClient;
import org.sonarsource.sonarqube.events.api.EventSource;
import org.sonarsource.sonarqube.events.api.EventSourceBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerStartupEventPublisherTest {

  private final EventAsyncClient eventAsyncClient = mock(EventAsyncClient.class);
  private final EventSourceBuilder eventSourceBuilder = mock(EventSourceBuilder.class);
  private final Server server = mock(Server.class);

  private final ServerStartupEventPublisher underTest = new ServerStartupEventPublisher(eventAsyncClient, eventSourceBuilder, server);

  @Test
  void start_publishesAnalyticsServerStartedEventOnce() {
    when(server.getId()).thenReturn("server-id");
    when(server.getVersion()).thenReturn("2026.4");
    when(eventSourceBuilder.build(ServerStartupEventPublisher.SOURCE_DOMAIN, ServerStartupEventPublisher.SOURCE_SERVICE))
      .thenReturn(new EventSource(ServerStartupEventPublisher.SOURCE_DOMAIN, ServerStartupEventPublisher.SOURCE_SERVICE, null, null, null));

    underTest.start();

    ArgumentCaptor<Event<?>> captor = ArgumentCaptor.forClass(Event.class);
    verify(eventAsyncClient).publishCrossDomainEvent(captor.capture());

    Event<?> event = captor.getValue();
    // The Analytics. prefix is what routes the event to the telemetry handler.
    assertThat(event.metadata().eventType()).isEqualTo("Analytics.Server.Started");
    assertThat(event.metadata().eventType()).startsWith("Analytics.");
    assertThat(event.metadata().eventVersion()).isEqualTo(ServerStartupEventPublisher.EVENT_VERSION);
    assertThat(event.payload()).isInstanceOfSatisfying(java.util.Map.class, payload -> assertThat(payload)
      .containsEntry("serverId", "server-id")
      .containsEntry("version", "2026.4"));
  }

  @Test
  void start_doesNotFailWhenPublishingThrows() {
    when(server.getId()).thenReturn("server-id");
    when(server.getVersion()).thenReturn("2026.4");
    when(eventSourceBuilder.build(any(), any())).thenReturn(new EventSource("d", "s", null, null, null));
    when(eventAsyncClient.publishCrossDomainEvent(any())).thenThrow(new RuntimeException("boom"));

    // Startup must not be broken by a telemetry failure.
    underTest.start();

    verify(eventAsyncClient).publishCrossDomainEvent(any());
  }

  @Test
  void stop_doesNothing() {
    underTest.stop();

    verify(eventAsyncClient, never()).publishCrossDomainEvent(any());
  }
}
