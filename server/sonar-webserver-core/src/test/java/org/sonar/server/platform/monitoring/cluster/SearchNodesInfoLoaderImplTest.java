/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.monitoring.cluster;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.newindex.FakeIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchNodesInfoLoaderImplTest {

  @Rule
  public EsTester es = EsTester.createCustom(new FakeIndexDefinition());

  private SearchNodesInfoLoaderImpl underTest = new SearchNodesInfoLoaderImpl(es.client());

  @Test
  public void return_info_from_elasticsearch_api() {
    Collection<NodeInfo> nodes = underTest.load();

    assertThat(nodes).hasSize(1);
    NodeInfo node = nodes.iterator().next();
    assertThat(node.getName()).isNotEmpty();
    assertThat(node.getHost()).isNotEmpty();
    assertThat(node.getSections()).hasSize(1);
    ProtobufSystemInfo.Section stateSection = node.getSections().get(0);

    assertThat(stateSection.getAttributesList())
      .extracting(ProtobufSystemInfo.Attribute::getKey)
      .contains(
        "Disk Available", "Store Size",
        "JVM Heap Usage", "JVM Heap Used", "JVM Heap Max", "JVM Non Heap Used",
        "JVM Threads",
        "Field Data Memory", "Field Data Circuit Breaker Limit", "Field Data Circuit Breaker Estimation",
        "Request Circuit Breaker Limit", "Request Circuit Breaker Estimation",
        "Query Cache Memory", "Request Cache Memory");
  }
}
