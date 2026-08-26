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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleBacklog;
import org.sonarsource.gessie.server.telemetry.EventIngestor;
import org.sonarsource.gessie.server.telemetry.GessieTelemetryCeHandler;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.ExecutingProcess;
import org.sonarsource.sonarqube.events.server.EventDispatcher;
import org.sonarsource.sonarqube.events.server.ServerEventAsyncClient;
import org.sonarsource.sonarqube.events.server.ServerEventSourceBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves a published event is actually routed to the Gessie handler through the real
 * dispatch pipeline, not just mocked at the {@code EventAsyncClient} boundary. Copied from
 * {@code ScaDependencyRiskFixedEventPublisherImplIT} (core-extension-sca).
 */
class AnalyticsEventPublisherRoundTripTest {

  @Test
  void publish_whenDispatchedInComputeEngine_routesSerializedPayloadToGessieHandler() {
    var ingestor = mock(EventIngestor.class);
    var gessieHandler = new GessieTelemetryCeHandler(ingestor, true);
    var dispatcher = new EventDispatcher(ExecutingProcess.COMPUTE_ENGINE, List.of(gessieHandler), null);
    var eventClient = new ServerEventAsyncClient(dispatcher);
    var configuration = mock(Configuration.class);
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));

    var underTest = new AnalyticsEventPublisher(eventClient, new ServerEventSourceBuilder(), configuration);
    var payload = new IssueBacklogTelemetryEvent(
      "project-uuid", "branch-uuid", "BRANCH", 1_700_000_000_000L, List.of(new RuleBacklog("java:S1234", 3, 3, 0, 0, 0, 0)));

    underTest.publish(IssueBacklogTelemetryEvent.TYPE, payload);

    ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(ingestor).ingest(eventCaptor.capture());
    var event = eventCaptor.getValue();
    assertThat(event.metadata().eventType()).isEqualTo(IssueBacklogTelemetryEvent.TYPE.eventType());
    assertThat(event.metadata().eventVersion()).isEqualTo(IssueBacklogTelemetryEvent.TYPE.eventVersion());
    assertThat(event.metadata().source().envName()).isNull();
    assertThat(event.metadata().source().envType()).isNull();
    assertThat(event.metadata().source().region()).isNull();

    JsonNode payloadJson = new ObjectMapper().valueToTree(event.payload());
    assertThat(payloadJson.get("project_uuid").asText()).isEqualTo("project-uuid");
    assertThat(payloadJson.get("branch_uuid").asText()).isEqualTo("branch-uuid");
    assertThat(payloadJson.get("branch_type").asText()).isEqualTo("BRANCH");
    assertThat(payloadJson.get("rules").get(0).get("plugin_rule_key").asText()).isEqualTo("java:S1234");
    assertThat(payloadJson.get("rules").get(0).get("total_count").asInt()).isEqualTo(3);
    assertThat(payloadJson.get("rules").get(0).get("open_count").asInt()).isEqualTo(3);
  }
}
