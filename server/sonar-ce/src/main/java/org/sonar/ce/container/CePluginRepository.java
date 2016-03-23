/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.Plugin;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.DefaultServerFileSystem;

import static java.lang.String.format;

/**
 * Entry point to load plugins on startup. It assumes that plugins
 * have been correctly installed/uninstalled during web server startup
 */
public class CePluginRepository implements PluginRepository, Startable {

  private static final String[] JAR_FILE_EXTENSIONS = new String[] {"jar"};

  private final DefaultServerFileSystem fs;
  private final PluginLoader loader;

  // following fields are available after startup
  private final Map<String, PluginInfo> pluginInfosByKeys = new HashMap<>();
  private final Map<String, Plugin> pluginInstancesByKeys = new HashMap<>();

  public CePluginRepository(DefaultServerFileSystem fs, PluginLoader loader) {
    this.fs = fs;
    this.loader = loader;
  }

  @Override
  public void start() {
    Loggers.get(getClass()).info("Load plugins");
    for (File file : listJarFiles(fs.getInstalledPluginsDir())) {
      PluginInfo info = PluginInfo.create(file);
      pluginInfosByKeys.put(info.getKey(), info);
    }
    pluginInstancesByKeys.putAll(loader.load(pluginInfosByKeys));
  }

  @Override
  public void stop() {
    // close classloaders
    loader.unload(pluginInstancesByKeys.values());
    pluginInstancesByKeys.clear();
    pluginInfosByKeys.clear();
  }

  @Override
  public Collection<PluginInfo> getPluginInfos() {
    return pluginInfosByKeys.values();
  }

  @Override
  public PluginInfo getPluginInfo(String key) {
    PluginInfo info = pluginInfosByKeys.get(key);
    if (info == null) {
      throw new IllegalArgumentException(format("Plugin [%s] does not exist", key));
    }
    return info;
  }

  @Override
  public Plugin getPluginInstance(String key) {
    Plugin plugin = pluginInstancesByKeys.get(key);
    if (plugin == null) {
      throw new IllegalArgumentException(format("Plugin [%s] does not exist", key));
    }
    return plugin;
  }

  @Override
  public boolean hasPlugin(String key) {
    return pluginInfosByKeys.containsKey(key);
  }

  private static Collection<File> listJarFiles(File dir) {
    if (dir.exists()) {
      return FileUtils.listFiles(dir, JAR_FILE_EXTENSIONS, false);
    }
    return Collections.emptyList();
  }
}
