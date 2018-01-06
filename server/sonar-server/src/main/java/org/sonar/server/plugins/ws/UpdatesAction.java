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
package org.sonar.server.plugins.ws;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.plugins.ws.PluginUpdateAggregator.PluginUpdateAggregate;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;

/**
 * Implementation of the {@code updates} action for the Plugins WebService.
 */
public class UpdatesAction implements PluginsWsAction {

  private static final boolean DO_NOT_FORCE_REFRESH = false;
  private static final String ARRAY_PLUGINS = "plugins";
  private static final String ARRAY_UPDATES = "updates";

  private static final Ordering<PluginUpdateAggregate> NAME_KEY_PLUGIN_UPGRADE_AGGREGATE_ORDERING = Ordering.from(PluginWSCommons.NAME_KEY_PLUGIN_ORDERING)
    .onResultOf(PluginUpdateAggregateToPlugin.INSTANCE);
  private static final Ordering<PluginUpdate> PLUGIN_UPDATE_BY_VERSION_ORDERING = Ordering.natural()
    .onResultOf(input -> input.getRelease().getVersion().toString());

  private final UserSession userSession;
  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final PluginWSCommons pluginWSCommons;
  private final PluginUpdateAggregator aggregator;

  public UpdatesAction(UserSession userSession, UpdateCenterMatrixFactory updateCenterMatrixFactory,
    PluginWSCommons pluginWSCommons,
    PluginUpdateAggregator aggregator) {
    this.userSession = userSession;
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.pluginWSCommons = pluginWSCommons;
    this.aggregator = aggregator;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("updates")
      .setDescription("Lists plugins installed on the SonarQube instance for which at least one newer version is available, sorted by plugin name." +
        "<br/>" +
        "Each newer version is listed, ordered from the oldest to the newest, with its own update/compatibility status." +
        "<br/>" +
        "Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response." +
        "<br/>" +
        "Update status values are: [COMPATIBLE, INCOMPATIBLE, REQUIRES_UPGRADE, DEPS_REQUIRE_UPGRADE].<br/>" +
        "Require 'Administer System' permission.")
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-updates_plugins.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject();

    Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(DO_NOT_FORCE_REFRESH);

    writePlugins(jsonWriter, updateCenter);

    pluginWSCommons.writeUpdateCenterProperties(jsonWriter, updateCenter);

    jsonWriter.endObject();
    jsonWriter.close();
  }

  private void writePlugins(JsonWriter jsonWriter, Optional<UpdateCenter> updateCenter) {
    jsonWriter.name(ARRAY_PLUGINS);
    jsonWriter.beginArray();
    if (updateCenter.isPresent()) {
      for (PluginUpdateAggregate aggregate : retrieveUpdatablePlugins(updateCenter.get())) {
        writePluginUpdateAggregate(jsonWriter, aggregate);
      }
    }
    jsonWriter.endArray();
  }

  private void writePluginUpdateAggregate(JsonWriter jsonWriter, PluginUpdateAggregate aggregate) {
    jsonWriter.beginObject();
    Plugin plugin = aggregate.getPlugin();

    pluginWSCommons.writePlugin(jsonWriter, plugin);

    writeUpdates(jsonWriter, aggregate.getUpdates());

    jsonWriter.endObject();
  }

  private void writeUpdates(JsonWriter jsonWriter, Collection<PluginUpdate> pluginUpdates) {
    jsonWriter.name(ARRAY_UPDATES).beginArray();
    for (PluginUpdate pluginUpdate : ImmutableSortedSet.copyOf(PLUGIN_UPDATE_BY_VERSION_ORDERING, pluginUpdates)) {
      jsonWriter.beginObject();
      pluginWSCommons.writeRelease(jsonWriter, pluginUpdate.getRelease());
      pluginWSCommons.writeUpdateProperties(jsonWriter, pluginUpdate);
      jsonWriter.endObject();
    }
    jsonWriter.endArray();
  }

  private Collection<PluginUpdateAggregate> retrieveUpdatablePlugins(UpdateCenter updateCenter) {
    List<PluginUpdate> pluginUpdates = updateCenter.findPluginUpdates();
    // aggregates updates of the same plugin to a single object and sort these objects by plugin name then key
    return ImmutableSortedSet.copyOf(
      NAME_KEY_PLUGIN_UPGRADE_AGGREGATE_ORDERING,
      aggregator.aggregate(pluginUpdates));
  }

  private enum PluginUpdateAggregateToPlugin implements Function<PluginUpdateAggregate, Plugin> {
    INSTANCE;

    @Override
    public Plugin apply(@Nonnull PluginUpdateAggregate input) {
      return input.getPlugin();
    }
  }
}
