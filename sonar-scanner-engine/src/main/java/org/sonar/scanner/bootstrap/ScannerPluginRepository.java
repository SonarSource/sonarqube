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
package org.sonar.scanner.bootstrap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.plugin.PluginType;

import static java.util.stream.Collectors.toMap;
import static org.sonar.api.utils.Preconditions.checkState;

/**
 * Orchestrates the installation and loading of plugins
 */
public class ScannerPluginRepository implements PluginRepository, Startable {
  private static final Logger LOG = LoggerFactory.getLogger(ScannerPluginRepository.class);

  private final PluginInstaller installer;
  private final PluginJarExploder pluginJarExploder;
  private final PluginClassLoader loader;

  private final Configuration properties;

  private Map<String, Plugin> pluginInstancesByKeys;
  private Map<String, ScannerPlugin> pluginsByKeys;
  private Map<ClassLoader, String> keysByClassLoader;
  private boolean shouldLoadAllPluginsOnStart;

  public ScannerPluginRepository(PluginInstaller installer, PluginJarExploder pluginJarExploder, PluginClassLoader loader, Configuration properties) {
    this.installer = installer;
    this.pluginJarExploder = pluginJarExploder;
    this.loader = loader;
    this.properties = properties;
  }

  @Override
  public void start() {
    shouldLoadAllPluginsOnStart = properties.getBoolean("sonar.plugins.loadAll").orElse(false);
    if (shouldLoadAllPluginsOnStart) {
      LOG.warn("sonar.plugins.loadAll is true, so ALL available plugins will be downloaded");
      pluginsByKeys = new HashMap<>(installer.installAllPlugins());
    } else {
      pluginsByKeys = new HashMap<>(installer.installRequiredPlugins());
    }

    Map<String, ExplodedPlugin> explodedPluginsByKey = pluginsByKeys.entrySet().stream()
      .collect(toMap(Map.Entry::getKey, e -> pluginJarExploder.explode(e.getValue().getInfo())));
    pluginInstancesByKeys = new HashMap<>(loader.load(explodedPluginsByKey));

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

    logPlugins(pluginsByKeys.values());
  }

  public Collection<PluginInfo> installPluginsForLanguages(Set<String> languageKeys) {
    if (shouldLoadAllPluginsOnStart) {
      return Collections.emptySet();
    }

    var languagePluginsByKeys = new HashMap<>(installer.installPluginsForLanguages(languageKeys));

    pluginsByKeys.putAll(languagePluginsByKeys);

    Map<String, ExplodedPlugin> explodedPluginsByKey = languagePluginsByKeys.entrySet().stream()
      .collect(toMap(Map.Entry::getKey, e -> pluginJarExploder.explode(e.getValue().getInfo())));
    pluginInstancesByKeys.putAll(new HashMap<>(loader.load(explodedPluginsByKey)));

    keysByClassLoader = new HashMap<>();
    for (Map.Entry<String, Plugin> e : pluginInstancesByKeys.entrySet()) {
      keysByClassLoader.put(e.getValue().getClass().getClassLoader(), e.getKey());
    }

    logPlugins(languagePluginsByKeys.values());
    return languagePluginsByKeys.values().stream().map(ScannerPlugin::getInfo).toList();
  }

  @CheckForNull
  public String getPluginKey(ClassLoader cl) {
    return keysByClassLoader.get(cl);
  }

  private static void logPlugins(Collection<ScannerPlugin> plugins) {
    if (plugins.isEmpty()) {
      LOG.debug("No plugins loaded");
    } else {
      LOG.debug("Plugins loaded:");
      for (ScannerPlugin p : plugins) {
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
    return pluginsByKeys.values().stream().map(ScannerPlugin::getInfo).toList();
  }

  public Collection<PluginInfo> getExternalPluginsInfos() {
    return pluginsByKeys.values().stream().filter(p -> p.getType() == PluginType.EXTERNAL).map(ScannerPlugin::getInfo).toList();
  }

  public Collection<PluginInfo> getBundledPluginsInfos() {
    return pluginsByKeys.values().stream().filter(p -> p.getType() == PluginType.BUNDLED).map(ScannerPlugin::getInfo).toList();
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
