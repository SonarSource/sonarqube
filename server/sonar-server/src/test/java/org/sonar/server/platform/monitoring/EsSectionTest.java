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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class EsSectionTest {

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));

  private EsSection underTest = new EsSection(esTester.client());

  @Test
  public void name() {
    assertThat(underTest.name()).isEqualTo("Elasticsearch");
  }

  @Test
  public void es_state() {
    assertThat(underTest.getState()).isEqualTo(ClusterHealthStatus.GREEN.name());
    assertThatAttributeIs(underTest.toProtobuf(), "State", ClusterHealthStatus.GREEN.name());
  }

  @Test
  public void node_attributes() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(attribute(section, "Store Size")).isNotNull();
  }

  @Test
  public void index_attributes() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    // one index "issues"
    assertThat(attribute(section, "Index issues - Docs").getLongValue()).isEqualTo(0L);
    assertThat(attribute(section, "Index issues - Shards").getLongValue()).isGreaterThan(0);
    assertThat(attribute(section, "Index issues - Store Size").getStringValue()).isNotNull();
  }

  @Test
  public void attributes_displays_exception_message_when_cause_null_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsSection underTest = new EsSection(esClientMock);
    when(esClientMock.prepareClusterStats()).thenThrow(new RuntimeException("RuntimeException with no cause"));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThatAttributeIs(section, "State", "RuntimeException with no cause");
  }

  @Test
  public void attributes_displays_exception_message_when_cause_is_not_ElasticSearchException_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsSection underTest = new EsSection(esClientMock);
    when(esClientMock.prepareClusterStats()).thenThrow(new RuntimeException("RuntimeException with cause not ES", new IllegalArgumentException("some cause message")));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThatAttributeIs(section, "State", "RuntimeException with cause not ES");
  }

  @Test
  public void attributes_displays_cause_message_when_cause_is_ElasticSearchException_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsSection underTest = new EsSection(esClientMock);
    when(esClientMock.prepareClusterStats()).thenThrow(new RuntimeException("RuntimeException with ES cause", new ElasticsearchException("some cause message")));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThatAttributeIs(section, "State", "some cause message");
  }
}
