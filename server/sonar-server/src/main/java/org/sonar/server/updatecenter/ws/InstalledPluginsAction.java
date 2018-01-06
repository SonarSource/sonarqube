/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.updatecenter.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.updatecenter.common.Version;

/**
 * This web service is used by SonarLint and SonarRunner.
 */
public class InstalledPluginsAction implements UpdateCenterWsAction {

  private final ServerPluginRepository pluginRepository;

  public InstalledPluginsAction(ServerPluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("installed_plugins")
      .setDescription("Get the list of all the plugins installed on the SonarQube instance")
      .setSince("2.10")
      .setDeprecatedSince("6.3")
      .setInternal(true)
      .setResponseExample(getClass().getResource("installed_plugins-example.json"))
      .setHandler(this);
    action.createParam("format")
      .setDescription("Only json response format is available")
      .setPossibleValues("json");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginArray();
      for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
        Version version = pluginInfo.getVersion();
        json.beginObject()
          .prop("key", pluginInfo.getKey())
          .prop("name", pluginInfo.getName());
        if (version != null) {
          json.prop("version", version.getName());
        }
        json.endObject();
      }
      json.endArray().close();
    }
  }
}
