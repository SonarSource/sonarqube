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

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsClient;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * In cluster mode, section "Search" that displays all ES information
 * that are not specific to a node or an index
 */
@ServerSide
public class EsClusterStateSection implements SystemInfoSection, Global {

  private final EsClient esClient;

  public EsClusterStateSection(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Search State");
    ClusterStatsResponse stats = esClient.prepareClusterStats().get();
    setAttribute(protobuf, "State", stats.getStatus().name());
    setAttribute(protobuf, "Nodes", stats.getNodesStats().getCounts().getTotal());
    return protobuf.build();
  }

}
