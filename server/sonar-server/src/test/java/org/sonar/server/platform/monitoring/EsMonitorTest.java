/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessId;
import org.sonar.process.jmx.EsSettingsMBean;
import org.sonar.process.jmx.JmxConnectionFactory;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.NewIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsMonitorTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  JmxConnectionFactory jmxConnectionFactory = mock(JmxConnectionFactory.class, Mockito.RETURNS_DEEP_STUBS);
  EsSettingsMBean settingsMBean = mock(EsSettingsMBean.class);
  EsMonitor underTest = new EsMonitor(jmxConnectionFactory, esTester.client());

  @Before
  public void setUp() throws Exception {
    when(jmxConnectionFactory.create(ProcessId.ELASTICSEARCH).getMBean(EsSettingsMBean.OBJECT_NAME, EsSettingsMBean.class)).thenReturn(settingsMBean);
  }

  @Test
  public void name() {
    assertThat(underTest.name()).isEqualTo("Elasticsearch");
  }

  @Test
  public void cluster_attributes() {
    LinkedHashMap<String, Object> attributes = underTest.attributes();
    assertThat(underTest.getState()).isEqualTo(ClusterHealthStatus.GREEN.name());
    assertThat(attributes.get("State")).isEqualTo(ClusterHealthStatus.GREEN);
    assertThat(attributes.get("Number of Nodes")).isEqualTo(1);
  }

  @Test
  public void node_attributes() {
    LinkedHashMap<String, Object> attributes = underTest.attributes();
    Map nodesAttributes = (Map) attributes.get("Nodes");

    // one node
    assertThat(nodesAttributes).hasSize(1);
    Map nodeAttributes = (Map) nodesAttributes.values().iterator().next();
    assertThat(nodeAttributes.get("Type")).isEqualTo("Master");
    assertThat(nodeAttributes.get("Store Size")).isNotNull();
  }

  @Test
  public void index_attributes() {
    LinkedHashMap<String, Object> attributes = underTest.attributes();
    Map indicesAttributes = (Map) attributes.get("Indices");

    // one index "issues"
    assertThat(indicesAttributes).hasSize(1);
    Map indexAttributes = (Map) indicesAttributes.values().iterator().next();
    assertThat(indexAttributes.get("Docs")).isEqualTo(0L);
    assertThat(indexAttributes.get("Shards")).isEqualTo(NewIndex.DEFAULT_NUMBER_OF_SHARDS);
    assertThat(indexAttributes.get("Store Size")).isNotNull();
  }
}
