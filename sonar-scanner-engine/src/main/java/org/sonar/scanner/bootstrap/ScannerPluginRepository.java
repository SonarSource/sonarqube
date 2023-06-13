/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.Plugin;
import org.sonar.api.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.plugin.PluginType;

import static java.util.stream.Collectors.toList;
import static org.sonar.api.utils.Preconditions.checkState;

/**
 * Orchestrates the installation and loading of plugins
 */
public class ScannerPluginRepository implements PluginRepository, Startable {
  private static final Logger LOG = LoggerFactory.getLogger(ScannerPluginRepository.class);

  private final PluginInstaller installer;
  private final PluginJarExploder pluginJarExploder;
  private final PluginClassLoader loader;

  private Map<String, Plugin> pluginInstancesByKeys;
  private Map<String, ScannerPlugin> pluginsByKeys;
  private Map<ClassLoader, String> keysByClassLoader;

  public ScannerPluginRepository(PluginInstaller installer, PluginJarExploder pluginJarExploder, PluginClassLoader loader) {
    this.installer = installer;
    this.pluginJarExploder = pluginJarExploder;
    this.loader = loader;
  }

  @Override
  public void start() {
    pluginsByKeys = new HashMap<>(installer.installRemotes());
    Map<String, ExplodedPlugin> explodedPLuginsByKey = pluginsByKeys.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> pluginJarExploder.explode(e.getValue().getInfo())));
    pluginInstancesByKeys = new HashMap<>(loader.load(explodedPLuginsByKey));

    // this part is only used by medium tests
    for (Object[] localPlugin : installer.installLocals()) {
      String pluginKey = (String) localPlugin[0];
      PluginInfo pluginInfo = new PluginInfo(pluginKey);
      pluginsByKeys.put(pluginKey, new ScannerPlugin(pluginInfo.getKey(), (long) localPlugin[2], PluginType.BUNDLED, pluginInfo));
      pluginInstancesByKeys.put(pluginKey, (Plugin) localPlugin[1]);
    }

    keysByClassLoader = new HashMap<>();
    for (Map.Entry<String, Plugin> e : pluginInstancesByKeys.entrySet()) {
      keysByClassLoader.put(e.getValue().getClass().getClassLoader(), e.getKey());
    }

    logPlugins();
  }

  @CheckForNull
  public String getPluginKey(ClassLoader cl) {
    return keysByClassLoader.get(cl);
  }

  private void logPlugins() {
    if (pluginsByKeys.isEmpty()) {
      LOG.debug("No plugins loaded");
    } else {
      LOG.debug("Plugins:");
      for (ScannerPlugin p : pluginsByKeys.values()) {
        LOG.debug("  * {} {} ({})", p.getName(), p.getVersion(), p.getKey());
      }
    }
  }

  @Override
  public void stop() {
    // close plugin classloaders
    loader.unload(pluginInstancesByKeys.values());

    pluginInstancesByKeys.clear();
    pluginsByKeys.clear();
    keysByClassLoader.clear();
  }

  public Map<String, ScannerPlugin> getPluginsByKey() {
    return pluginsByKeys;
  }

  @Override
  public Collection<PluginInfo> getPluginInfos() {
    return pluginsByKeys.values().stream().map(ScannerPlugin::getInfo).collect(toList());
  }

  public Collection<PluginInfo> getExternalPluginsInfos() {
    return pluginsByKeys.values().stream().filter(p -> p.getType() == PluginType.EXTERNAL).map(ScannerPlugin::getInfo).collect(toList());
  }

  public Collection<PluginInfo> getBundledPluginsInfos() {
    return pluginsByKeys.values().stream().filter(p -> p.getType() == PluginType.BUNDLED).map(ScannerPlugin::getInfo).collect(toList());
  }

  @Override
  public PluginInfo getPluginInfo(String key) {
    ScannerPlugin info = pluginsByKeys.get(key);
    checkState(info != null, "Plugin [%s] does not exist", key);
    return info.getInfo();
  }

  @Override
  public Plugin getPluginInstance(String key) {
    Plugin instance = pluginInstancesByKeys.get(key);
    checkState(instance != null, "Plugin [%s] does not exist", key);
    return instance;
  }

  @Override
  public Collection<Plugin> getPluginInstances() {
    return pluginInstancesByKeys.values();
  }

  @Override
  public boolean hasPlugin(String key) {
    return pluginsByKeys.containsKey(key);
  }
}
