/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class EsStateSectionTest {

  @Rule
  public EsTester es = EsTester.create();

  private EsStateSection underTest = new EsStateSection(es.client());

  @Test
  public void name() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Search State");
  }

  @Test
  public void es_state() {
    assertThatAttributeIs(underTest.toProtobuf(), "State", ClusterHealthStatus.GREEN.name());
  }

  @Test
  public void node_attributes() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(attribute(section, "CPU Usage (%)")).isNotNull();
    assertThat(attribute(section, "Disk Available")).isNotNull();
    assertThat(attribute(section, "Store Size")).isNotNull();
    assertThat(attribute(section, "Translog Size")).isNotNull();
  }

  @Test
  public void attributes_displays_exception_message_when_cause_null_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsStateSection underTest = new EsStateSection(esClientMock);
    when(esClientMock.clusterHealth(any())).thenThrow(new RuntimeException("RuntimeException with no cause"));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThatAttributeIs(section, "State", "RuntimeException with no cause");
  }

  @Test
  public void attributes_displays_exception_message_when_cause_is_not_ElasticSearchException_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsStateSection underTest = new EsStateSection(esClientMock);
    when(esClientMock.clusterHealth(any())).thenThrow(new RuntimeException("RuntimeException with cause not ES", new IllegalArgumentException("some cause message")));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThatAttributeIs(section, "State", "RuntimeException with cause not ES");
  }

  @Test
  public void attributes_displays_cause_message_when_cause_is_ElasticSearchException_when_client_fails() {
    EsClient esClientMock = mock(EsClient.class);
    EsStateSection underTest = new EsStateSection(esClientMock);
    when(esClientMock.clusterHealth(any())).thenThrow(new RuntimeException("RuntimeException with ES cause", new ElasticsearchException("some cause message")));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThatAttributeIs(section, "State", "some cause message");
  }
}
