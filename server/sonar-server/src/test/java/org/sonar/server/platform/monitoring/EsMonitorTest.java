/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Map;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsMonitorTest {

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));

  private EsMonitor underTest = new EsMonitor(esTester.client());

  @Test
  public void name() {
    assertThat(underTest.name()).isEqualTo("Elasticsearch");
  }

  @Test
  public void cluster_attributes() {
    Map<String, Object> attributes = underTest.attributes();
    assertThat(underTest.getState()).isEqualTo(ClusterHealthStatus.GREEN.name());
    assertThat(attributes.get("State")).isEqualTo(ClusterHealthStatus.GREEN);
    assertThat(attributes.get("Number of Nodes")).isEqualTo(1);
  }

  @Test
  public void node_attributes() {
    Map<String, Object> attributes = underTest.attributes();
    Map nodesAttributes = (Map) attributes.get("Nodes");

    // one node
    assertThat(nodesAttributes).hasSize(1);
    Map nodeAttributes = (Map) nodesAttributes.values().iterator().next();
    assertThat(nodeAttributes.get("Type")).isEqualTo("Master");
    assertThat(nodeAttributes.get("Store Size")).isNotNull();
  }

  @Test
  public void index_attributes() {
    Map<String, Object> attributes = underTest.attributes();
    Map indicesAttributes = (Map) attributes.get("Indices");

    // one index "issues"
    assertThat(indicesAttributes).hasSize(1);
    Map indexAttributes = (Map) indicesAttributes.values().iterator().next();
    assertThat(indexAttributes.get("Docs")).isEqualTo(0L);
    assertThat((int) indexAttributes.get("Shards")).isGreaterThan(0);
    assertThat(indexAttributes.get("Store Size")).isNotNull();
  }

  @Test
  public void attributes_displays_exception_message_when_cause_null_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsMonitor underTest = new EsMonitor(esClientMock);
    when(esClientMock.prepareClusterStats()).thenThrow(new RuntimeException("RuntimeException with no cause"));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).hasSize(1);
    assertThat(attributes.get("State")).isEqualTo("RuntimeException with no cause");
  }

  @Test
  public void attributes_displays_exception_message_when_cause_is_not_ElasticSearchException_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsMonitor underTest = new EsMonitor(esClientMock);
    when(esClientMock.prepareClusterStats()).thenThrow(new RuntimeException("RuntimeException with cause not ES", new IllegalArgumentException("some cause message")));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).hasSize(1);
    assertThat(attributes.get("State")).isEqualTo("RuntimeException with cause not ES");
  }

  @Test
  public void attributes_displays_cause_message_when_cause_is_ElasticSearchException_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsMonitor underTest = new EsMonitor(esClientMock);
    when(esClientMock.prepareClusterStats()).thenThrow(new RuntimeException("RuntimeException with ES cause", new ElasticsearchException("some cause message")));

    Map<String, Object> attributes = underTest.attributes();
    assertThat(attributes).hasSize(1);
    assertThat(attributes.get("State")).isEqualTo("some cause message");
  }
}
