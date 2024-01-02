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
package org.sonar.ce.container;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.sonar.api.Plugin;
import org.sonar.api.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.plugins.PluginRequirementsValidator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Entry point to load plugins on startup. It assumes that plugins
 * have been correctly installed/uninstalled/updated during web server startup
 */
public class CePluginRepository implements PluginRepository, Startable {

  private static final String[] JAR_FILE_EXTENSIONS = new String[] {"jar"};
  private static final String NOT_STARTED_YET = "not started yet";

  private final ServerFileSystem fs;
  private final PluginClassLoader loader;
  private final CePluginJarExploder cePluginJarExploder;
  private final AtomicBoolean started = new AtomicBoolean(false);

  // following fields are available after startup
  private final Map<String, PluginInfo> pluginInfosByKeys = new HashMap<>();
  private final Map<String, Plugin> pluginInstancesByKeys = new HashMap<>();

  public CePluginRepository(ServerFileSystem fs, PluginClassLoader loader, CePluginJarExploder cePluginJarExploder) {
    this.fs = fs;
    this.loader = loader;
    this.cePluginJarExploder = cePluginJarExploder;
  }

  @Override
  public void start() {
    Loggers.get(getClass()).info("Load plugins");
    registerPluginsFromDir(fs.getInstalledBundledPluginsDir());
    registerPluginsFromDir(fs.getInstalledExternalPluginsDir());

    PluginRequirementsValidator.unloadIncompatiblePlugins(pluginInfosByKeys);

    Map<String, ExplodedPlugin> explodedPluginsByKey = extractPlugins(pluginInfosByKeys);
    pluginInstancesByKeys.putAll(loader.load(explodedPluginsByKey));
    started.set(true);
  }

  private void registerPluginsFromDir(File pluginsDir) {
    for (File file : listJarFiles(pluginsDir)) {
      PluginInfo info = PluginInfo.create(file);
      pluginInfosByKeys.put(info.getKey(), info);
    }
  }

  private Map<String, ExplodedPlugin> extractPlugins(Map<String, PluginInfo> pluginsByKey) {
    return pluginsByKey.values().stream()
      .map(cePluginJarExploder::explode)
      .collect(Collectors.toMap(ExplodedPlugin::getKey, p -> p));
  }

  @Override
  public void stop() {
    // close classloaders
    loader.unload(pluginInstancesByKeys.values());
    pluginInstancesByKeys.clear();
    pluginInfosByKeys.clear();
    started.set(false);
  }

  @Override
  public Collection<PluginInfo> getPluginInfos() {
    checkState(started.get(), NOT_STARTED_YET);
    return Set.copyOf(pluginInfosByKeys.values());
  }

  @Override
  public PluginInfo getPluginInfo(String key) {
    checkState(started.get(), NOT_STARTED_YET);
    PluginInfo info = pluginInfosByKeys.get(key);
    if (info == null) {
      throw new IllegalArgumentException(format("Plugin [%s] does not exist", key));
    }
    return info;
  }

  @Override
  public Plugin getPluginInstance(String key) {
    checkState(started.get(), NOT_STARTED_YET);
    Plugin plugin = pluginInstancesByKeys.get(key);
    checkArgument(plugin != null, "Plugin [%s] does not exist", key);
    return plugin;
  }

  @Override
  public Collection<Plugin> getPluginInstances() {
    return pluginInstancesByKeys.values();
  }

  @Override
  public boolean hasPlugin(String key) {
    checkState(started.get(), NOT_STARTED_YET);
    return pluginInfosByKeys.containsKey(key);
  }

  private static Collection<File> listJarFiles(File dir) {
    if (dir.exists()) {
      return FileUtils.listFiles(dir, JAR_FILE_EXTENSIONS, false);
    }
    return Collections.emptyList();
  }
}
