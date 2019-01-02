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
package org.sonar.server.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.platform.ServerFileSystem;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.forceMkdir;

public class PluginUninstaller implements Startable {
  private static final String PLUGIN_EXTENSION = "jar";
  private final ServerPluginRepository serverPluginRepository;
  private final File uninstallDir;

  public PluginUninstaller(ServerPluginRepository serverPluginRepository, ServerFileSystem fs) {
    this.serverPluginRepository = serverPluginRepository;
    this.uninstallDir = fs.getUninstalledPluginsDir();
  }

  private static Collection<File> listJarFiles(File dir) {
    if (dir.exists()) {
      return FileUtils.listFiles(dir, new String[] {PLUGIN_EXTENSION}, false);
    }
    return Collections.emptyList();
  }

  @Override
  public void start() {
    try {
      forceMkdir(uninstallDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory: " + uninstallDir, e);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  public void uninstall(String pluginKey) {
    ensurePluginIsInstalled(pluginKey);
    serverPluginRepository.uninstall(pluginKey, uninstallDir);
  }

  public void cancelUninstalls() {
    serverPluginRepository.cancelUninstalls(uninstallDir);
  }

  /**
   * @return the list of plugins to be uninstalled as {@link PluginInfo} instances
   */
  public Collection<PluginInfo> getUninstalledPlugins() {
    return listJarFiles(uninstallDir)
      .stream()
      .map(PluginInfo::create)
      .collect(MoreCollectors.toList());
  }

  private void ensurePluginIsInstalled(String key) {
    if (!serverPluginRepository.hasPlugin(key)) {
      throw new IllegalArgumentException(format("Plugin [%s] is not installed", key));
    }
  }
}
