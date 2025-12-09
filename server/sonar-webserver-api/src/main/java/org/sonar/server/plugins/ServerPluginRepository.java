/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.plugin.PluginType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class ServerPluginRepository implements PluginRepository {
  private final Map<String, ServerPlugin> pluginByKey = new HashMap<>();
  private final Map<ClassLoader, String> keysByClassLoader = new HashMap<>();

  public void addPlugins(List<ServerPlugin> plugins) {
    pluginByKey.putAll(plugins.stream().collect(Collectors.toMap(p -> p.getPluginInfo().getKey(), p -> p)));
    for (ServerPlugin p : plugins) {
      keysByClassLoader.put(p.getClassloader(), p.getPluginInfo().getKey());
    }
  }

  public void addPlugin(ServerPlugin plugin) {
    pluginByKey.put(plugin.getPluginInfo().getKey(), plugin);
    if (plugin.getInstance() != null) {
      keysByClassLoader.put(plugin.getInstance().getClass().getClassLoader(), plugin.getPluginInfo().getKey());
    }
  }

  @CheckForNull
  public String getPluginKey(Object extension) {
    return keysByClassLoader.get(extension.getClass().getClassLoader());
  }

  @Override
  public Collection<PluginInfo> getPluginInfos() {
    return pluginByKey.values().stream().map(ServerPlugin::getPluginInfo).toList();
  }

  @Override
  public PluginInfo getPluginInfo(String key) {
    return getPlugin(key).getPluginInfo();
  }

  public ServerPlugin getPlugin(String key) {
    ServerPlugin plugin = pluginByKey.get(key);
    if (plugin == null) {
      throw new IllegalArgumentException(format("Plugin [%s] does not exist", key));
    }
    return plugin;
  }

  public Collection<ServerPlugin> getPlugins() {
    return Collections.unmodifiableCollection(pluginByKey.values());
  }

  public Optional<ServerPlugin> findPlugin(String key) {
    return Optional.ofNullable(pluginByKey.get(key));
  }

  public Collection<PluginInfo> getPluginsInfoByType(PluginType type){
    return pluginByKey.values()
      .stream()
      .filter(p -> p.getType() == type)
      .map(ServerPlugin::getPluginInfo)
      .toList();
  }

  @Override
  public Plugin getPluginInstance(String key) {
    ServerPlugin plugin = pluginByKey.get(key);
    checkArgument(plugin != null, "Plugin [%s] does not exist", key);
    return plugin.getInstance();
  }

  @Override
  public Collection<Plugin> getPluginInstances() {
    return pluginByKey.values().stream()
      .map(ServerPlugin::getInstance)
      .toList();
  }

  @Override
  public boolean hasPlugin(String key) {
    return pluginByKey.containsKey(key);
  }
}
