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
package org.sonar.server.platform.ws;

import java.util.Collection;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.NodeInfo;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoader;
import org.sonar.server.user.UserSession;

public class ClusterInfoAction extends BaseInfoWsAction {

  private final GlobalInfoLoader globalInfoLoader;
  private final AppNodesInfoLoader appNodesInfoLoader;
  private final SearchNodesInfoLoader searchNodesInfoLoader;
  private final HealthChecker healthChecker;

  public ClusterInfoAction(UserSession userSession, GlobalInfoLoader globalInfoLoader,
    AppNodesInfoLoader appNodesInfoLoader, SearchNodesInfoLoader searchNodesInfoLoader, HealthChecker healthChecker) {
    super(userSession);
    this.globalInfoLoader = globalInfoLoader;
    this.appNodesInfoLoader = appNodesInfoLoader;
    this.searchNodesInfoLoader = searchNodesInfoLoader;
    this.healthChecker = healthChecker;
  }

  @Override
  protected void doHandle(Request request, Response response) {
    ClusterHealth clusterHealth = healthChecker.checkCluster();
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      writeGlobalSections(json, clusterHealth);
      writeApplicationNodes(json, clusterHealth);
      writeSearchNodes(json, clusterHealth);
      json.endObject();
    }
  }

  private void writeGlobalSections(JsonWriter json, ClusterHealth clusterHealth) {
    globalInfoLoader.load().forEach(section -> writeSectionToJson(section, json));
  }

  private void writeApplicationNodes(JsonWriter json, ClusterHealth clusterHealth) {
    json.name("Application Nodes").beginArray();

    Collection<NodeInfo> appNodes = appNodesInfoLoader.load();
    for (NodeInfo applicationNode : appNodes) {
      writeNodeInfoToJson(applicationNode, clusterHealth, json);
    }
    json.endArray();
  }

  private void writeSearchNodes(JsonWriter json, ClusterHealth clusterHealth) {
    json.name("Search Nodes").beginArray();

    Collection<NodeInfo> searchNodes = searchNodesInfoLoader.load();
    searchNodes.forEach(node -> writeNodeInfoToJson(node, clusterHealth, json));
    json.endArray();
  }

  private void writeNodeInfoToJson(NodeInfo nodeInfo, ClusterHealth clusterHealth, JsonWriter json) {
    json.beginObject();
    json.prop("Name", nodeInfo.getName());
    json.prop("Error", nodeInfo.getErrorMessage().orElse(null));
    json.prop("Host", nodeInfo.getHost().orElse(null));
    json.prop("Started At", nodeInfo.getStartedAt().orElse(null));

    clusterHealth.getNodeHealth(nodeInfo.getName()).ifPresent(h -> {
      json.prop("Health", h.getStatus().name());
      json.name("Health Causes").beginArray().values(h.getCauses()).endArray();
    });

    writeSectionsToJson(nodeInfo.getSections(), json);
    json.endObject();
  }
}
