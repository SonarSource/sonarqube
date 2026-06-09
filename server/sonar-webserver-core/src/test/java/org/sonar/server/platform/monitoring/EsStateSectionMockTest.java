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
package org.sonar.server.platform.monitoring;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import java.util.List;
import org.junit.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStats;
import org.sonar.server.es.response.NodeStatsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class EsStateSectionMockTest {

  @Test
  public void name_is_Search_State() {
    EsClient esClient = mock(EsClient.class);
    EsStateSection underTest = new EsStateSection(esClient);
    // clusterHealthV2 throws so we exercise the catch branch but still return a Section with the right name
    when(esClient.clusterHealthV2(any())).thenThrow(new RuntimeException("boom"));

    assertThat(underTest.toProtobuf().getName()).isEqualTo("Search State");
  }

  @Test
  public void toProtobuf_sets_state_from_cluster_health_when_no_nodes() {
    EsClient esClient = mock(EsClient.class);
    HealthResponse healthResponse = mock(HealthResponse.class);
    when(healthResponse.status()).thenReturn(HealthStatus.Green);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);
    NodeStatsResponse emptyNodes = mock(NodeStatsResponse.class);
    when(emptyNodes.getNodeStats()).thenReturn(List.of());
    when(esClient.nodesStats()).thenReturn(emptyNodes);

    EsStateSection underTest = new EsStateSection(esClient);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "State", HealthStatus.Green.name().toUpperCase());
  }

  @Test
  public void state_falls_back_to_exception_message_when_cause_is_null() {
    EsClient esClient = mock(EsClient.class);
    when(esClient.clusterHealthV2(any())).thenThrow(new RuntimeException("RuntimeException with no cause"));

    EsStateSection underTest = new EsStateSection(esClient);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "State", "RuntimeException with no cause");
  }

  @Test
  public void state_falls_back_to_exception_message_when_cause_is_not_ES_exception() {
    EsClient esClient = mock(EsClient.class);
    when(esClient.clusterHealthV2(any()))
      .thenThrow(new RuntimeException("RuntimeException with cause not ES", new IllegalArgumentException("inner")));

    EsStateSection underTest = new EsStateSection(esClient);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "State", "RuntimeException with cause not ES");
  }

  @Test
  public void state_uses_cause_message_when_cause_is_ES_exception() {
    EsClient esClient = mock(EsClient.class);
    ElasticsearchException esCause = mock(ElasticsearchException.class);
    when(esCause.getMessage()).thenReturn("ES specific cause");
    when(esClient.clusterHealthV2(any()))
      .thenThrow(new RuntimeException("outer message", esCause));

    EsStateSection underTest = new EsStateSection(esClient);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "State", "ES specific cause");
  }

  @Test
  public void completeNodeAttributes_populates_attributes_from_first_node() {
    EsClient esClient = mock(EsClient.class);
    HealthResponse healthResponse = mock(HealthResponse.class);
    when(healthResponse.status()).thenReturn(HealthStatus.Yellow);
    when(esClient.clusterHealthV2(any())).thenReturn(healthResponse);

    NodeStats node = mock(NodeStats.class, RETURNS_DEEP_STUBS);
    when(node.getCpuUsage()).thenReturn(42L);

    NodeStatsResponse nodesResponse = mock(NodeStatsResponse.class);
    when(nodesResponse.getNodeStats()).thenReturn(List.of(node));
    when(esClient.nodesStats()).thenReturn(nodesResponse);

    EsStateSection underTest = new EsStateSection(esClient);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "State", HealthStatus.Yellow.name().toUpperCase());
    assertThatAttributeIs(section, "CPU Usage (%)", 42L);
  }
}