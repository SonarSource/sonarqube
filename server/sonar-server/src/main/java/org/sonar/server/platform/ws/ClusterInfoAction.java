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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.NodeInfo;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoader;
import org.sonar.server.user.UserSession;

public class ClusterInfoAction implements SystemWsAction {

  private final UserSession userSession;
  private final GlobalInfoLoader globalInfoLoader;
  private final AppNodesInfoLoader appNodesInfoLoader;
  private final SearchNodesInfoLoader searchNodesInfoLoader;
  private final HealthChecker healthChecker;

  public ClusterInfoAction(UserSession userSession, GlobalInfoLoader globalInfoLoader,
    AppNodesInfoLoader appNodesInfoLoader, SearchNodesInfoLoader searchNodesInfoLoader, HealthChecker healthChecker) {
    this.userSession = userSession;
    this.globalInfoLoader = globalInfoLoader;
    this.appNodesInfoLoader = appNodesInfoLoader;
    this.searchNodesInfoLoader = searchNodesInfoLoader;
    this.healthChecker = healthChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("cluster_info")
      .setDescription("WIP")
      .setSince("6.6")
      .setInternal(true)
      .setResponseExample(getClass().getResource("/org/sonar/server/platform/ws/info-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();

    ClusterHealth clusterHealth = healthChecker.checkCluster();
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      writeGlobalSections(json, clusterHealth);
      writeApplicationNodes(json, clusterHealth);
      writeSearchNodes(json, clusterHealth);
      json.endObject();
    } catch (Throwable throwable) {
      Loggers.get(getClass()).error("fff", throwable);
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

  private static void writeNodeInfoToJson(NodeInfo nodeInfo, ClusterHealth clusterHealth, JsonWriter json) {
    json.beginObject();
    json.prop("Name", nodeInfo.getName());
    json.prop("Error", nodeInfo.getErrorMessage().orElse(null));
    json.prop("Host", nodeInfo.getHost().orElse(null));
    json.prop("Started At", nodeInfo.getStartedAt().orElse(null));

    clusterHealth.getNodeHealth(nodeInfo.getName()).ifPresent(h -> {
      json.prop("Health", h.getStatus().name());
      json.name("Health Causes").beginArray().values(h.getCauses()).endArray();
    });

    nodeInfo.getSections().forEach(section -> writeSectionToJson(section, json));
    json.endObject();
  }

  private static void writeSectionToJson(ProtobufSystemInfo.Section section, JsonWriter json) {
    json.name(section.getName());
    json.beginObject();
    section.getAttributesList().forEach(attribute -> writeAttributeToJson(json, attribute));
    json.endObject();
  }

  private static void writeAttributeToJson(JsonWriter json, ProtobufSystemInfo.Attribute attribute) {
    switch (attribute.getValueCase()) {
      case BOOLEAN_VALUE:
        json.prop(attribute.getKey(), attribute.getBooleanValue());
        break;
      case LONG_VALUE:
        json.prop(attribute.getKey(), attribute.getLongValue());
        break;
      case DOUBLE_VALUE:
        json.prop(attribute.getKey(), attribute.getDoubleValue());
        break;
      case STRING_VALUE:
        json.prop(attribute.getKey(), attribute.getStringValue());
        break;
      case VALUE_NOT_SET:
        json.name(attribute.getKey()).beginArray().values(attribute.getStringValuesList()).endArray();
        break;
      default:
        throw new IllegalArgumentException("Unsupported type: " + attribute.getValueCase());
    }
  }
}
