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

import java.util.Arrays;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.user.UserSession;

import static org.sonar.server.telemetry.TelemetryDataJsonWriter.writeTelemetryData;

/**
 * Implementation of the {@code info} action for the System WebService.
 */
public class InfoAction implements SystemWsAction {

  private final UserSession userSession;
  private final CeHttpClient ceHttpClient;
  private final SystemInfoSection[] systemInfoSections;
  private final TelemetryDataLoader statistics;

  public InfoAction(UserSession userSession, CeHttpClient ceHttpClient, TelemetryDataLoader statistics, SystemInfoSection... systemInfoSections) {
    this.userSession = userSession;
    this.ceHttpClient = ceHttpClient;
    this.statistics = statistics;
    this.systemInfoSections = systemInfoSections;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("info")
      .setDescription("Get detailed information about system configuration.<br/>" +
        "Requires 'Administer' permissions.<br/>" +
        "Since 5.5, this web service becomes internal in order to more easily update result.")
      .setSince("5.1")
      .setInternal(true)
      .setResponseExample(getClass().getResource("/org/sonar/server/platform/ws/info-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkIsSystemAdministrator();

    try (JsonWriter json = response.newJsonWriter()) {
      writeJson(json);
    }
  }

  private void writeJson(JsonWriter json) {
    json.beginObject();
    Arrays.stream(systemInfoSections)
      .map(SystemInfoSection::toProtobuf)
      .forEach(section -> sectionToJson(section, json));
    Optional<ProtobufSystemInfo.SystemInfo> ceSysInfo = ceHttpClient.retrieveSystemInfo();
    if (ceSysInfo.isPresent()) {
      ceSysInfo.get().getSectionsList().forEach(section -> sectionToJson(section, json));
    }
    writeStatistics(json);
    json.endObject();
  }

  private void writeStatistics(JsonWriter json) {
    json.name("Statistics");
    writeTelemetryData(json, statistics.load());
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
        break;
      default:
        throw new IllegalArgumentException("Unsupported type: " + attribute.getValueCase());
    }
  }
}
