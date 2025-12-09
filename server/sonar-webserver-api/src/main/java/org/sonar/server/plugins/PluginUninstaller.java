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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.platform.ServerFileSystem;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.moveFileToDirectory;
import static org.sonar.core.plugin.PluginType.EXTERNAL;

public class PluginUninstaller implements Startable {
  private static final Logger LOG = LoggerFactory.getLogger(PluginUninstaller.class);
  private static final String PLUGIN_EXTENSION = "jar";

  private final ServerFileSystem fs;
  private final ServerPluginRepository pluginRepository;

  public PluginUninstaller(ServerFileSystem fs, ServerPluginRepository pluginRepository) {
    this.fs = fs;
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void start() {
    try {
      forceMkdir(fs.getUninstalledPluginsDir());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory: " + fs.getUninstalledPluginsDir(), e);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  /**
   * Uninstall a plugin and its dependents
   */
  public void uninstall(String pluginKey) {
    if (!pluginRepository.hasPlugin(pluginKey) || pluginRepository.getPlugin(pluginKey).getType() != EXTERNAL) {
      throw new IllegalArgumentException(format("Plugin [%s] is not installed", pluginKey));
    }

    Set<String> uninstallKeys = new HashSet<>();
    uninstallKeys.add(pluginKey);
    appendDependentPluginKeys(pluginKey, uninstallKeys);

    for (String uninstallKey : uninstallKeys) {
      PluginInfo info = pluginRepository.getPluginInfo(uninstallKey);
      // we don't check type because the dependent of an external plugin should never be a bundled plugin!
      uninstall(info.getKey(), info.getName(), info.getNonNullJarFile().getName());
    }
  }

  public void cancelUninstalls() {
    for (File file : listJarFiles(fs.getUninstalledPluginsDir())) {
      try {
        moveFileToDirectory(file, fs.getInstalledExternalPluginsDir(), false);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to cancel plugin uninstalls", e);
      }
    }
  }

  /**
   * @return the list of plugins to be uninstalled as {@link PluginInfo} instances
   */
  public Collection<PluginInfo> getUninstalledPlugins() {
    return listJarFiles(fs.getUninstalledPluginsDir()).stream()
      .map(PluginInfo::create)
      .toList();
  }

  private static Collection<File> listJarFiles(File dir) {
    if (dir.exists()) {
      return FileUtils.listFiles(dir, new String[] {PLUGIN_EXTENSION}, false);
    }
    return Collections.emptyList();
  }

  private void uninstall(String key, String name, String fileName) {
    try {
      if (!getPluginFile(fileName).exists()) {
        LOG.info("Plugin already uninstalled: {} [{}]", name, key);
        return;
      }

      LOG.info("Uninstalling plugin {} [{}]", name, key);

      File masterFile = getPluginFile(fileName);
      moveFileToDirectory(masterFile, fs.getUninstalledPluginsDir(), true);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to uninstall plugin %s [%s]", name, key), e);
    }
  }

  private File getPluginFile(String fileName) {
    // just to be sure that file is located in from extensions/plugins
    return new File(fs.getInstalledExternalPluginsDir(), fileName);
  }

  private void appendDependentPluginKeys(String pluginKey, Set<String> appendTo) {
    for (PluginInfo otherPlugin : pluginRepository.getPluginInfos()) {
      if (otherPlugin.getKey().equals(pluginKey)) {
        continue;
      }

      for (PluginInfo.RequiredPlugin requirement : otherPlugin.getRequiredPlugins()) {
        if (requirement.getKey().equals(pluginKey)) {
          appendTo.add(otherPlugin.getKey());
          appendDependentPluginKeys(otherPlugin.getKey(), appendTo);
        }
      }
    }
  }
}
