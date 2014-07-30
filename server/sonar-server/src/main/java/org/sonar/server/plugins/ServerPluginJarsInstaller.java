/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.PluginReferential;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ServerPluginJarsInstaller {

  private static final Logger LOG = LoggerFactory.getLogger(ServerPluginJarsInstaller.class);

  private final Server server;
  private final DefaultServerFileSystem fs;
  private final ServerPluginJarInstaller installer;
  private final Map<String, PluginMetadata> pluginByKeys = Maps.newHashMap();
  private final ServerUpgradeStatus serverUpgradeStatus;

  public ServerPluginJarsInstaller(Server server, ServerUpgradeStatus serverUpgradeStatus,
                                   DefaultServerFileSystem fs, ServerPluginJarInstaller installer) {
    this.server = server;
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.fs = fs;
    this.installer = installer;
  }

  public void install() {
    TimeProfiler profiler = new TimeProfiler().start("Install plugins");
    deleteTrash();
    loadInstalledPlugins();
    copyBundledPlugins();
    moveDownloadedPlugins();
    loadCorePlugins();
    deployPlugins();
    profiler.stop();
  }

  private void deleteTrash() {
    File trashDir = fs.getTrashPluginsDir();
    try {
      if (trashDir.exists()) {
        FileUtils.deleteDirectory(trashDir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean the plugin trash directory: " + trashDir, e);
    }
  }

  private void loadInstalledPlugins() {
    for (File file : fs.getUserPlugins()) {
      DefaultPluginMetadata metadata = installer.extractMetadata(file, false);
      if (StringUtils.isNotBlank(metadata.getKey())) {
        PluginMetadata existing = pluginByKeys.put(metadata.getKey(), metadata);
        if (existing != null) {
          throw MessageException.of(String.format("Found two files for the same plugin '%s': %s and %s",
            metadata.getKey(), metadata.getFile().getName(), existing.getFile().getName()));
        }
      }
    }
  }

  private void moveDownloadedPlugins() {
    if (fs.getDownloadedPluginsDir().exists()) {
      Collection<File> sourceFiles = FileUtils.listFiles(fs.getDownloadedPluginsDir(), new String[]{"jar"}, false);
      for (File sourceFile : sourceFiles) {
        overridePlugin(sourceFile, true);
      }
    }
  }

  private void copyBundledPlugins() {
    if (serverUpgradeStatus.isFreshInstall()) {
      for (File sourceFile : fs.getBundledPlugins()) {
        DefaultPluginMetadata metadata = installer.extractMetadata(sourceFile, false);
        // lib/bundled-plugins should be copied only if the plugin is not already
        // available in extensions/plugins
        if (!pluginByKeys.containsKey(metadata.getKey())) {
          overridePlugin(sourceFile, false);
        }
      }
    }
  }


  private void overridePlugin(File sourceFile, boolean deleteSource) {
    File destDir = fs.getUserPluginsDir();
    File destFile = new File(destDir, sourceFile.getName());
    if (destFile.exists()) {
      // plugin with same filename already installed
      FileUtils.deleteQuietly(destFile);
    }

    try {
      if (deleteSource) {
        FileUtils.moveFile(sourceFile, destFile);
      } else {
        FileUtils.copyFile(sourceFile, destFile, true);
      }
    } catch (IOException e) {
      LOG.error(String.format("Fail to move or copy plugin: %s to %s",
        sourceFile.getAbsolutePath(), destFile.getAbsolutePath()), e);
    }

    DefaultPluginMetadata metadata = installer.extractMetadata(destFile, false);
    if (StringUtils.isNotBlank(metadata.getKey())) {
      PluginMetadata existing = pluginByKeys.put(metadata.getKey(), metadata);
      if (existing != null) {
        if (!existing.getFile().getName().equals(destFile.getName())) {
          FileUtils.deleteQuietly(existing.getFile());
        }
        LOG.info("Plugin " + metadata.getKey() + " replaced by new version");
      }
    }
  }

  private void loadCorePlugins() {
    for (File file : fs.getCorePlugins()) {
      DefaultPluginMetadata metadata = installer.extractMetadata(file, true);
      PluginMetadata existing = pluginByKeys.put(metadata.getKey(), metadata);
      if (existing != null) {
        throw new IllegalStateException("Found two plugins with the same key '" + metadata.getKey() + "': " + metadata.getFile().getName() + " and "
          + existing.getFile().getName());
      }
    }
  }

  public void uninstall(String pluginKey) {
    for (String key : getPluginReferential().findLastReleasesWithDependencies(pluginKey)) {
      uninstallPlugin(key);
    }
  }

  private void uninstallPlugin(String pluginKey) {
    PluginMetadata metadata = pluginByKeys.get(pluginKey);
    if (metadata != null && !metadata.isCore()) {
      try {
        File masterFile = new File(fs.getUserPluginsDir(), metadata.getFile().getName());
        FileUtils.moveFileToDirectory(masterFile, fs.getTrashPluginsDir(), true);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to uninstall plugin: " + pluginKey, e);
      }
    }
  }

  public List<String> getUninstalls() {
    List<String> names = Lists.newArrayList();
    if (fs.getTrashPluginsDir().exists()) {
      List<File> files = (List<File>) FileUtils.listFiles(fs.getTrashPluginsDir(), new String[]{"jar"}, false);
      for (File file : files) {
        names.add(file.getName());
      }
    }
    return names;
  }

  public void cancelUninstalls() {
    if (fs.getTrashPluginsDir().exists()) {
      List<File> files = (List<File>) FileUtils.listFiles(fs.getTrashPluginsDir(), new String[]{"jar"}, false);
      for (File file : files) {
        try {
          FileUtils.moveFileToDirectory(file, fs.getUserPluginsDir(), false);
        } catch (IOException e) {
          throw new IllegalStateException("Fail to cancel plugin uninstalls", e);
        }
      }
    }
  }

  private void deployPlugins() {
    for (PluginMetadata metadata : pluginByKeys.values()) {
      deploy((DefaultPluginMetadata) metadata);
    }
  }

  private void deploy(DefaultPluginMetadata plugin) {
    LOG.info("Deploy plugin {}", Joiner.on(" / ").skipNulls().join(plugin.getName(), plugin.getVersion(), plugin.getImplementationBuild()));

    Preconditions.checkState(plugin.isCompatibleWith(server.getVersion()),
      "Plugin %s needs a more recent version of SonarQube than %s. At least %s is expected",
      plugin.getKey(), server.getVersion(), plugin.getSonarVersion());

    try {
      File pluginDeployDir = new File(fs.getDeployedPluginsDir(), plugin.getKey());
      FileUtils.forceMkdir(pluginDeployDir);
      FileUtils.cleanDirectory(pluginDeployDir);

      installer.installToDir(plugin, pluginDeployDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to deploy the plugin " + plugin, e);
    }
  }

  public Collection<PluginMetadata> getMetadata() {
    return pluginByKeys.values();
  }

  public PluginMetadata getMetadata(String pluginKey) {
    return pluginByKeys.get(pluginKey);
  }

  private PluginReferential getPluginReferential() {
    return PluginReferentialMetadataConverter.getInstalledPluginReferential(getMetadata());
  }
}
