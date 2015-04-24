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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.ServerPluginRepository;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.SortedSet;

import static com.google.common.collect.Iterables.filter;
import static org.sonar.server.plugins.ws.PluginWSCommons.NAME_KEY_PLUGIN_METADATA_COMPARATOR;

/**
 * Implementation of the {@code installed} action for the Plugins WebService.
 */
public class InstalledPluginsWsAction implements PluginsWsAction {
  private static final String ARRAY_PLUGINS = "plugins";

  private final ServerPluginRepository pluginRepository;
  private final PluginWSCommons pluginWSCommons;

  public InstalledPluginsWsAction(ServerPluginRepository pluginRepository,
    PluginWSCommons pluginWSCommons) {
    this.pluginRepository = pluginRepository;
    this.pluginWSCommons = pluginWSCommons;
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
    Collection<PluginInfo> infos = retrieveAndSortPluginMetadata();

    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.setSerializeEmptys(false);
    jsonWriter.beginObject();

    writeMetadataList(jsonWriter, infos);

    jsonWriter.endObject();
    jsonWriter.close();
  }

  private SortedSet<PluginInfo> retrieveAndSortPluginMetadata() {
    return ImmutableSortedSet.copyOf(
      NAME_KEY_PLUGIN_METADATA_COMPARATOR,
      filter(pluginRepository.getPluginInfos(), NotCorePluginsPredicate.INSTANCE)
      );
  }

  private void writeMetadataList(JsonWriter jsonWriter, Collection<PluginInfo> pluginMetadatas) {
    jsonWriter.name(ARRAY_PLUGINS);
    jsonWriter.beginArray();
    for (PluginInfo pluginMetadata : pluginMetadatas) {
      pluginWSCommons.writePluginMetadata(jsonWriter, pluginMetadata);
    }
    jsonWriter.endArray();
  }

  private enum NotCorePluginsPredicate implements Predicate<PluginInfo> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable PluginInfo input) {
      return input != null && !input.isCore();
    }
  }
}
