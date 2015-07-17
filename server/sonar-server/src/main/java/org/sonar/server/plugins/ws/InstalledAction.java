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
package org.sonar.server.plugins.ws;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.util.Collection;
import java.util.SortedSet;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.Plugin;

import static com.google.common.collect.ImmutableSortedSet.copyOf;
import static org.sonar.server.plugins.ws.PluginWSCommons.NAME_KEY_PLUGIN_METADATA_COMPARATOR;
import static org.sonar.server.plugins.ws.PluginWSCommons.compatiblePluginsByKey;

/**
 * Implementation of the {@code installed} action for the Plugins WebService.
 */
public class InstalledAction implements PluginsWsAction {
  private static final String ARRAY_PLUGINS = "plugins";

  private final ServerPluginRepository pluginRepository;
  private final PluginWSCommons pluginWSCommons;
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;

  public InstalledAction(ServerPluginRepository pluginRepository, PluginWSCommons pluginWSCommons, UpdateCenterMatrixFactory updateCenterMatrixFactory) {
    this.pluginRepository = pluginRepository;
    this.pluginWSCommons = pluginWSCommons;
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("installed")
      .setDescription("Get the list of all the plugins installed on the SonarQube instance, sorted by plugin name")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-installed_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Collection<PluginInfo> pluginInfoList = searchPluginInfoList();

    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.setSerializeEmptys(false);
    jsonWriter.beginObject();

    writePluginInfoList(jsonWriter, pluginInfoList);

    jsonWriter.endObject();
    jsonWriter.close();
  }

  private SortedSet<PluginInfo> searchPluginInfoList() {
    return copyOf(NAME_KEY_PLUGIN_METADATA_COMPARATOR, pluginRepository.getPluginInfos());
  }

  private void writePluginInfoList(JsonWriter jsonWriter, Collection<PluginInfo> pluginInfoList) {
    ImmutableMap<String, Plugin> compatiblesPluginsByKeys = compatiblePluginsByKey(updateCenterMatrixFactory);
    pluginWSCommons.writePluginInfoList(jsonWriter, pluginInfoList, compatiblesPluginsByKeys, ARRAY_PLUGINS);
  }
}
