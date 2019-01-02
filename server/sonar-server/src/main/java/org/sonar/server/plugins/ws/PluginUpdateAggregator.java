/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static java.lang.String.format;

public class PluginUpdateAggregator {

  public Collection<PluginUpdateAggregate> aggregate(@Nullable Collection<PluginUpdate> pluginUpdates) {
    if (pluginUpdates == null || pluginUpdates.isEmpty()) {
      return Collections.emptyList();
    }

    Map<Plugin, PluginUpdateAggregateBuilder> builders = Maps.newHashMap();
    for (PluginUpdate pluginUpdate : pluginUpdates) {
      Plugin plugin = pluginUpdate.getPlugin();
      PluginUpdateAggregateBuilder builder = builders.get(plugin);
      if (builder == null) {
        builder = PluginUpdateAggregateBuilder.builderFor(plugin);
        builders.put(plugin, builder);
      }
      builder.add(pluginUpdate);
    }

    return Lists.newArrayList(transform(builders.values(), PluginUpdateAggregateBuilder::build));
  }

  @VisibleForTesting
  static class PluginUpdateAggregateBuilder {
    private final Plugin plugin;

    private final List<PluginUpdate> updates = Lists.newArrayListWithExpectedSize(1);

    // use static method
    private PluginUpdateAggregateBuilder(Plugin plugin) {
      this.plugin = plugin;
    }

    public static PluginUpdateAggregateBuilder builderFor(Plugin plugin) {
      return new PluginUpdateAggregateBuilder(checkNotNull(plugin));
    }

    public PluginUpdateAggregateBuilder add(PluginUpdate pluginUpdate) {
      checkArgument(
        this.plugin.equals(pluginUpdate.getPlugin()),
        format("This builder only accepts PluginUpdate instances for plugin %s", plugin));
      this.updates.add(pluginUpdate);
      return this;
    }

    public PluginUpdateAggregate build() {
      return new PluginUpdateAggregate(this);
    }
  }

  public static class PluginUpdateAggregate {
    private final Plugin plugin;

    private final Collection<PluginUpdate> updates;

    protected PluginUpdateAggregate(PluginUpdateAggregateBuilder builder) {
      this.plugin = builder.plugin;
      this.updates = ImmutableList.copyOf(builder.updates);
    }

    public Plugin getPlugin() {
      return plugin;
    }

    public Collection<PluginUpdate> getUpdates() {
      return updates;
    }

  }
}
