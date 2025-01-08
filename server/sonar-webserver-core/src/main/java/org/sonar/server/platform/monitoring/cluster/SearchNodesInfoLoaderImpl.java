/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.response.NodeStats;
import org.sonar.server.es.response.NodeStatsResponse;
import org.sonar.server.platform.monitoring.EsStateSection;

@ServerSide
public class SearchNodesInfoLoaderImpl implements SearchNodesInfoLoader {

  private final EsClient esClient;

  public SearchNodesInfoLoaderImpl(EsClient esClient) {
    this.esClient = esClient;
  }

  public Collection<NodeInfo> load() {
    NodeStatsResponse response = esClient.nodesStats();

    List<NodeInfo> result = new ArrayList<>();
    response.getNodeStats().forEach(nodeStat -> result.add(toNodeInfo(nodeStat)));
    return result;
  }

  private static NodeInfo toNodeInfo(NodeStats stat) {
    String nodeName = stat.getName();
    NodeInfo info = new NodeInfo(nodeName);
    info.setHost(stat.getHost());

    ProtobufSystemInfo.Section.Builder section = ProtobufSystemInfo.Section.newBuilder();
    section.setName("Search State");
    EsStateSection.toProtobuf(stat, section);
    info.addSection(section.build());
    return info;
  }

}
