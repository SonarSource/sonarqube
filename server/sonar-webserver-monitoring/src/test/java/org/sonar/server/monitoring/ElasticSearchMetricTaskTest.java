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
package org.sonar.server.monitoring;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStatsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ElasticSearchMetricTaskTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final ServerMonitoringMetrics serverMonitoringMetrics = mock(ServerMonitoringMetrics.class);
  private final EsClient esClient = mock(EsClient.class);
  private final Configuration configuration = new MapSettings().asConfig();

  private final ElasticSearchMetricTask underTest = new ElasticSearchMetricTask(serverMonitoringMetrics, esClient, configuration);

  @BeforeEach
  void before() {
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  void when_elasticsearch_up_status_is_updated_to_green() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }



  @Test
  void elasticsearch_free_disk_space_is_updated() throws IOException {
    URL esNodeResponseUrl = getClass().getResource("es-node-response.json");
    String jsonPayload = StringUtils.trim(IOUtils.toString(esNodeResponseUrl, StandardCharsets.UTF_8));

    JsonObject jsonObject = new Gson().fromJson(jsonPayload, JsonObject.class);
    NodeStatsResponse nodeStats = NodeStatsResponse.toNodeStatsResponse(jsonObject);

    when(esClient.nodesStats()).thenReturn(nodeStats);

    underTest.run();

    String nodeName = nodeStats.getNodeStats().get(0).getName();
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceFreeBytes(nodeName, 136144027648L);
    verify(serverMonitoringMetrics, times(1)).setElasticSearchDiskSpaceTotalBytes(nodeName, 250685575168L);

    // elasticsearch health status is not mocked in this test, so this part raise an exception
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.ERROR)).containsOnly("Failed to query ES status");
  }

  @Test
  void when_elasticsearch_yellow_status_is_updated_to_green() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Yellow);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToGreen();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void when_elasticsearch_down_status_is_updated_to_red() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(HealthStatus.Red);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void when_no_es_status_null_status_is_updated_to_red() {
    HealthResponse healthResponse = Mockito.mock(HealthResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(healthResponse.status()).thenReturn(null);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);
  }

  @Test
  void when_es_status_throw_exception_status_is_updated_to_red() {
    when(esClient.clusterHealthV2(any())).thenThrow(new IllegalStateException("exception in cluster health"));

    underTest.run();

    verify(serverMonitoringMetrics, times(1)).setElasticSearchStatusToRed();
    verifyNoMoreInteractions(serverMonitoringMetrics);

    assertThat(logTester.logs()).hasSize(2);
    assertThat(logTester.logs(Level.ERROR)).containsOnly("Failed to query ES status");
  }

  @Test
  void task_has_default_delay(){
    Assertions.assertThat(underTest.getDelay()).isPositive();
    Assertions.assertThat(underTest.getPeriod()).isPositive();
  }

}
