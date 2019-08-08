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
package org.sonar.server.platform.ws;

import java.util.Collection;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.NodeInfo;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoader;
import org.sonar.server.telemetry.TelemetryDataLoader;

public class ClusterSystemInfoWriter extends SystemInfoWriter {
  private final GlobalInfoLoader globalInfoLoader;
  private final AppNodesInfoLoader appNodesInfoLoader;
  private final SearchNodesInfoLoader searchNodesInfoLoader;
  private final HealthChecker healthChecker;

  public ClusterSystemInfoWriter(GlobalInfoLoader globalInfoLoader, AppNodesInfoLoader appNodesInfoLoader, SearchNodesInfoLoader searchNodesInfoLoader,
    HealthChecker healthChecker, TelemetryDataLoader telemetry) {
    super(telemetry);
    this.globalInfoLoader = globalInfoLoader;
    this.appNodesInfoLoader = appNodesInfoLoader;
    this.searchNodesInfoLoader = searchNodesInfoLoader;
    this.healthChecker = healthChecker;
  }

  @Override
  public void write(JsonWriter json) throws InterruptedException {
    ClusterHealth clusterHealth = healthChecker.checkCluster();
    writeHealth(clusterHealth.getHealth(), json);
    writeGlobalSections(json);
    writeApplicationNodes(json, clusterHealth);
    writeSearchNodes(json, clusterHealth);
    writeTelemetry(json);
  }

  private void writeGlobalSections(JsonWriter json) {
    writeSections(globalInfoLoader.load(), json);
  }

  private void writeApplicationNodes(JsonWriter json, ClusterHealth clusterHealth) throws InterruptedException {
    json.name("Application Nodes").beginArray();

    Collection<NodeInfo> appNodes = appNodesInfoLoader.load();
    for (NodeInfo applicationNode : appNodes) {
      writeNodeInfo(applicationNode, clusterHealth, json);
    }
    json.endArray();
  }

  private void writeSearchNodes(JsonWriter json, ClusterHealth clusterHealth) {
    json.name("Search Nodes").beginArray();

    Collection<NodeInfo> searchNodes = searchNodesInfoLoader.load();
    searchNodes.forEach(node -> writeNodeInfo(node, clusterHealth, json));
    json.endArray();
  }

  private void writeNodeInfo(NodeInfo nodeInfo, ClusterHealth clusterHealth, JsonWriter json) {
    json.beginObject();
    json.prop("Name", nodeInfo.getName());
    json.prop("Error", nodeInfo.getErrorMessage().orElse(null));
    json.prop("Host", nodeInfo.getHost().orElse(null));
    json.prop("Started At", nodeInfo.getStartedAt().orElse(null));

    clusterHealth.getNodeHealth(nodeInfo.getName()).ifPresent(h -> {
      json.prop("Health", h.getStatus().name());
      json.name("Health Causes").beginArray().values(h.getCauses()).endArray();
    });

    writeSections(nodeInfo.getSections(), json);
    json.endObject();
  }

}
