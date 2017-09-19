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
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.NodeInfo;
import org.sonar.server.user.UserSession;

public class ClusterInfoAction implements SystemWsAction {

  private final UserSession userSession;
  private final GlobalInfoLoader globalInfoLoader;
  private final AppNodesInfoLoader appNodesInfoLoader;

  public ClusterInfoAction(UserSession userSession, GlobalInfoLoader globalInfoLoader, AppNodesInfoLoader appNodesInfoLoader) {
    this.userSession = userSession;
    this.globalInfoLoader = globalInfoLoader;
    this.appNodesInfoLoader = appNodesInfoLoader;
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

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      writeGlobal(json);
      writeApplicationNodes(json);
      json.endObject();
    }
  }

  private void writeGlobal(JsonWriter json) {
    globalInfoLoader.load().forEach(section -> sectionToJson(section, json));
  }

  private void writeApplicationNodes(JsonWriter json) {
    json.name("Application Nodes").beginArray();

    Collection<NodeInfo> appNodes = appNodesInfoLoader.load();
    for (NodeInfo applicationNode : appNodes) {
      writeApplicationNode(json, applicationNode);
    }
    json.endArray();
  }

  private void writeApplicationNode(JsonWriter json, NodeInfo applicationNode) {
    json.beginObject();
    json.prop("Name", applicationNode.getName());
    applicationNode.getSections().forEach(section -> sectionToJson(section, json));
    json.endObject();
  }

  private static void sectionToJson(ProtobufSystemInfo.Section section, JsonWriter json) {
    json.name(section.getName());
    json.beginObject();
    for (ProtobufSystemInfo.Attribute attribute : section.getAttributesList()) {
      attributeToJson(json, attribute);
    }
    json.endObject();
  }

  private static void attributeToJson(JsonWriter json, ProtobufSystemInfo.Attribute attribute) {
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
