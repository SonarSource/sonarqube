/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.platform.monitoring.Monitor;
import org.sonar.server.user.UserSession;

import java.util.Map;

public class SystemInfoWsAction implements SystemWsAction {

  private final Monitor[] monitors;

  public SystemInfoWsAction(Monitor... monitors) {
    this.monitors = monitors;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("info")
      .setDescription("Detailed information about system configuration")
      .setSince("5.1")
      .setResponseExample(getClass().getResource("/org/sonar/server/platform/ws/example-system-info.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    JsonWriter json = response.newJsonWriter();
    writeJson(json);
    json.close();
  }

  private void writeJson(JsonWriter json) {
    json.beginObject();
    for (Monitor monitor : monitors) {
      json.name(monitor.name());
      json.beginObject();
      for (Map.Entry<String, Object> attribute : monitor.attributes().entrySet()) {
        json.name(attribute.getKey()).valueObject(attribute.getValue());
      }
      json.endObject();
    }
    json.endObject();
  }
}
