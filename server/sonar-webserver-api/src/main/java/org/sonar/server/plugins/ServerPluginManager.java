/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.Startable;
import org.sonar.api.Plugin;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.core.plugin.PluginType;

/**
 * Entry point to install and load plugins on server startup. It manages
 * <ul>
 *   <li>installation of new plugins (effective after server startup)</li>
 *   <li>un-installation of plugins (effective after server startup)</li>
 *   <li>cancel pending installations/un-installations</li>
 *   <li>instantiation of plugin entry-points</li>
 * </ul>
 */
public class ServerPluginManager implements Startable {
  private static final Logger LOG = Loggers.get(ServerPluginManager.class);

  private final PluginJarLoader pluginJarLoader;
  private final PluginJarExploder pluginJarExploder;
  private final PluginClassLoader pluginClassLoader;
  private final ServerPluginRepository pluginRepository;

  public ServerPluginManager(PluginClassLoader pluginClassLoader, PluginJarExploder pluginJarExploder,
    PluginJarLoader pluginJarLoader, ServerPluginRepository pluginRepository) {
    this.pluginClassLoader = pluginClassLoader;
    this.pluginJarExploder = pluginJarExploder;
    this.pluginJarLoader = pluginJarLoader;
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void start() {
    Collection<ServerPluginInfo> loadedPlugins = pluginJarLoader.loadPlugins();
    logInstalledPlugins(loadedPlugins);
    Collection<ExplodedPlugin> explodedPlugins = extractPlugins(loadedPlugins);
    Map<String, Plugin> instancesByKey = pluginClassLoader.load(explodedPlugins);
    Map<String, PluginType> typesByKey = getTypesByKey(loadedPlugins);
    List<ServerPlugin> plugins = createServerPlugins(explodedPlugins, instancesByKey, typesByKey);
    pluginRepository.addPlugins(plugins);
  }

  private static Map<String, PluginType> getTypesByKey(Collection<ServerPluginInfo> loadedPlugins) {
    return loadedPlugins.stream().collect(Collectors.toMap(ServerPluginInfo::getKey, ServerPluginInfo::getType));
  }

  @Override
  public void stop() {
    pluginClassLoader.unload(pluginRepository.getPluginInstances());
  }

  private static void logInstalledPlugins(Collection<ServerPluginInfo> plugins) {
    plugins.stream().sorted().forEach(plugin -> LOG.info("Deploy {} / {} / {}", plugin.getName(), plugin.getVersion(), plugin.getImplementationBuild()));
  }

  private Collection<ExplodedPlugin> extractPlugins(Collection<ServerPluginInfo> plugins) {
    return plugins.stream().map(pluginJarExploder::explode).toList();
  }

  private static List<ServerPlugin> createServerPlugins(Collection<ExplodedPlugin> explodedPlugins, Map<String, Plugin> instancesByKey, Map<String, PluginType> typesByKey) {
    List<ServerPlugin> plugins = new ArrayList<>();
    for (ExplodedPlugin p : explodedPlugins) {
      plugins.add(new ServerPlugin(p.getPluginInfo(), typesByKey.get(p.getKey()), instancesByKey.get(p.getKey()),
        new PluginFilesAndMd5.FileAndMd5(p.getPluginInfo().getNonNullJarFile())));
    }
    return plugins;
  }
}
