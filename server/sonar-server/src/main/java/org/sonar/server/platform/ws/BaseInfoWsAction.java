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
import org.sonar.process.systeminfo.SystemInfoUtils;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.health.Health;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.user.UserSession;

import static org.sonar.server.telemetry.TelemetryDataJsonWriter.writeTelemetryData;

public abstract class BaseInfoWsAction implements SystemWsAction {

  private static final String[] ORDERED_SECTION_NAMES = {
    "System", "Database", "Plugins",
    "Web JVM State", "Web Logging", "Web JVM Properties",
    "Search State", "Search Statistics",
    "Compute Engine Database Connection", "Compute Engine JVM State", "Compute Engine Logging", "Compute Engine Tasks", "Compute Engine JVM Properties"};

  private final UserSession userSession;
  private final TelemetryDataLoader telemetry;

  public BaseInfoWsAction(UserSession userSession, TelemetryDataLoader telemetry) {
    this.userSession = userSession;
    this.telemetry = telemetry;
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
    doHandle(request, response);
  }

  protected abstract void doHandle(Request request, Response response);

  protected void writeSections(Collection<ProtobufSystemInfo.Section> sections, JsonWriter json) {
    SystemInfoUtils
      .order(sections, ORDERED_SECTION_NAMES)
      .forEach(section -> writeSection(section, json));
  }

  private void writeSection(ProtobufSystemInfo.Section section, JsonWriter json) {
    json.name(section.getName());
    json.beginObject();
    for (ProtobufSystemInfo.Attribute attribute : section.getAttributesList()) {
      writeAttribute(attribute, json);
    }
    json.endObject();
  }

  private void writeAttribute(ProtobufSystemInfo.Attribute attribute, JsonWriter json) {
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

  protected void writeHealth(Health health, JsonWriter json) {
    json.prop("Health", health.getStatus().name());
    json.name("Health Causes").beginArray().values(health.getCauses()).endArray();
  }

  protected void writeTelemetry(JsonWriter json) {
    json.name("Statistics");
    writeTelemetryData(json, telemetry.load());
  }
}
