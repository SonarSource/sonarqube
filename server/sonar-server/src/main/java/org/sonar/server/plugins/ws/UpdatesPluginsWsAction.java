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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.updatecenter.common.PluginUpdate;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * Implementation of the {@code updates} action for the Plugins WebService.
 */
public class UpdatesPluginsWsAction implements PluginsWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;
  private static final String ARRAY_PLUGINS = "plugins";
  private static final Comparator<PluginUpdate> NAME_KEY_PLUGIN_UPDATE_ORDERING = Ordering.from(CASE_INSENSITIVE_ORDER)
    .onResultOf(PluginUpdateToName.INSTANCE)
    .compound(
      Ordering.from(CASE_INSENSITIVE_ORDER).onResultOf(PluginUpdateToKey.INSTANCE)
    );

  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final PluginWSCommons pluginWSCommons;

  public UpdatesPluginsWsAction(UpdateCenterMatrixFactory updateCenterMatrixFactory,
                                PluginWSCommons pluginWSCommons) {
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.pluginWSCommons = pluginWSCommons;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("updates")
      .setDescription("Lists plugins installed on the SonarQube instance for which at least one newer version is available, sorted by plugin name." +
        "br/>" +
        "Each newer version is a separate entry in the returned list, with its update/compatibility status." +
        "<br/>" +
        "Update status values are: [COMPATIBLE, INCOMPATIBLE, REQUIRES_UPGRADE, DEPS_REQUIRE_UPGRADE]")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-updates_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject();

    writePlugins(jsonWriter);

    jsonWriter.close();
  }

  private void writePlugins(JsonWriter jsonWriter) {
    jsonWriter.name(ARRAY_PLUGINS);
    jsonWriter.beginArray();
    for (PluginUpdate pluginUpdate : retrieveUpdatablePlugins()) {
      pluginWSCommons.writePluginUpdate(jsonWriter, pluginUpdate);
    }
    jsonWriter.endArray();
    jsonWriter.endObject();
  }

  private Collection<PluginUpdate> retrieveUpdatablePlugins() {
    return ImmutableSortedSet.copyOf(
      NAME_KEY_PLUGIN_UPDATE_ORDERING,
      updateCenterMatrixFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH).findPluginUpdates()
      );
  }

  private enum PluginUpdateToKey implements Function<PluginUpdate, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable PluginUpdate input) {
      if (input == null) {
        return null;
      }
      return input.getPlugin().getKey();
    }
  }

  private enum PluginUpdateToName implements Function<PluginUpdate, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable PluginUpdate input) {
      if (input == null) {
        return null;
      }
      return input.getPlugin().getName();
    }
  }
}
