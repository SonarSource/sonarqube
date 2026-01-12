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
package org.sonar.ce.task.projectanalysis.purge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.core.util.UuidFactory;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.core.schema.ProjectMetric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

@ExtendWith(MockitoExtension.class)
class TelemetryQGOnMergedPRProviderTest {
  private static final long TEST_METRIC_VALUE = 42L;
  private static final String TEST_METRIC_KEY = "test_metric_key";
  private static final String PROJECT_UUID = "test-project-uuid";
  private static final ProjectMetric TEST_METRIC = new ProjectMetric(
    TEST_METRIC_KEY,
    TEST_METRIC_VALUE,
    PROJECT_UUID,
    TelemetryDataType.INTEGER,
    Granularity.ADHOC
  );
  private static final ProjectMetric TEST_METRIC_2 = new ProjectMetric(
    "some_other_metric_key",
    123,
    PROJECT_UUID,
    TelemetryDataType.INTEGER,
    Granularity.ADHOC
  );
  @Mock
  private TelemetryClient telemetryClient;
  @Mock
  private Server server;
  @Mock
  private UuidFactory uuidFactory;
  @Mock
  private Configuration configuration;
  @Mock
  private TelemetryQGOnMergedPRDataLoader telemetryQGOnMergedPRDataLoader;

  @InjectMocks
  private TelemetryQGOnMergedPRProvider underTest;

  @Test
  void sendTelemetry_whenTelemetryEnabled_sendsMessageWithCorrectStructure() throws JsonProcessingException {
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.of(true));
    when(server.getId()).thenReturn("test-server-id");
    when(uuidFactory.create()).thenReturn("test-message-uuid");
    when(telemetryQGOnMergedPRDataLoader.getMetrics()).thenReturn(Set.of(TEST_METRIC));

    underTest.sendTelemetry();

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());

    String jsonPayload = argumentCaptor.getValue();
    JsonNode message = new ObjectMapper().readValue(jsonPayload, JsonNode.class);

    assertThat(message.get("message_uuid").asText()).isEqualTo("test-message-uuid");
    assertThat(message.get("installation_id").asText()).isEqualTo("test-server-id");
    assertThat(message.get("dimension").asText()).isEqualTo("project");
    assertThat(message.get("metric_values")).hasSize(1);

    JsonNode metricValue = message.get("metric_values").get(0);
    assertThat(metricValue.get("key").asText()).isEqualTo(TEST_METRIC_KEY);
    assertThat(metricValue.get("value").asLong()).isEqualTo(TEST_METRIC_VALUE);
    assertThat(metricValue.get("project_uuid").asText()).isEqualTo(PROJECT_UUID);

    verify(telemetryQGOnMergedPRDataLoader).resetMetrics();
  }

  @ParameterizedTest
  @MethodSource("sonarTelemetryDisabled")
  void sendTelemetry_whenTelemetryDisabled_doesNotSendMessage(Boolean disabledPropertyValue) {
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.ofNullable(disabledPropertyValue));

    underTest.sendTelemetry();

    verifyNoInteractions(telemetryClient);
    verify(telemetryQGOnMergedPRDataLoader).resetMetrics();
  }

  private static Stream<Boolean> sonarTelemetryDisabled() {
    return Stream.of(false, null);
  }

  @Test
  void sendTelemetry_whenTelemetryEnabledWithEmptyMetrics_doesNotSendMessage() {
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.of(true));
    when(telemetryQGOnMergedPRDataLoader.getMetrics()).thenReturn(Collections.emptySet());

    underTest.sendTelemetry();

    verifyNoInteractions(telemetryClient);
    verify(telemetryQGOnMergedPRDataLoader).resetMetrics();
  }

  @Test
  void sendTelemetry_whenTelemetryEnabledWithMultipleMetrics_includesAllMetrics() throws JsonProcessingException {
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.of(true));
    when(server.getId()).thenReturn("test-server-id");
    when(uuidFactory.create()).thenReturn("test-message-uuid");

    when(telemetryQGOnMergedPRDataLoader.getMetrics()).thenReturn(Set.of(TEST_METRIC, TEST_METRIC_2));

    underTest.sendTelemetry();

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());

    String jsonPayload = argumentCaptor.getValue();
    JsonNode message = new ObjectMapper().readValue(jsonPayload, JsonNode.class);

    assertThat(message.get("metric_values")).hasSize(2);

    verify(telemetryQGOnMergedPRDataLoader).resetMetrics();
  }
}
