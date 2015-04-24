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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.PluginReferential;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.apache.commons.io.FileUtils.moveFileToDirectory;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class ServerPluginJarsInstaller {

  private static final Logger LOG = Loggers.get(ServerPluginJarsInstaller.class);
  private static final String FILE_EXTENSION_JAR = "jar";
  private static final Joiner SLASH_JOINER = Joiner.on(" / ").skipNulls();

  private final Server server;
  private final DefaultServerFileSystem fs;
  private final ServerPluginJarInstaller installer;
  private final Map<String, PluginMetadata> pluginByKeys = Maps.newHashMap();
  private final ServerUpgradeStatus serverUpgradeStatus;
  private static final Set<String> BLACKLISTED_PLUGINS = new HashSet<String>(Arrays.asList("scmactivity", "issuesreport"));

  public ServerPluginJarsInstaller(Server server, ServerUpgradeStatus serverUpgradeStatus,
    DefaultServerFileSystem fs, ServerPluginJarInstaller installer) {
    this.server = server;
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.fs = fs;
    this.installer = installer;
  }

  public void install() {
    Profiler profiler = Profiler.create(LOG).startInfo("Install plugins");
    deleteTrash();
    loadInstalledPlugins();
    copyBundledPlugins();
    moveDownloadedPlugins();
    loadCorePlugins();
    deployPlugins();
    profiler.stopDebug();
  }

  private void deleteTrash() {
    File trashDir = fs.getTrashPluginsDir();
    try {
      if (trashDir.exists()) {
        deleteDirectory(trashDir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean the plugin trash directory: " + trashDir, e);
    }
  }

  private void loadInstalledPlugins() {
    for (File file : fs.getUserPlugins()) {
      PluginMetadata metadata = installer.fileToPlugin().apply(file);
      if (isNotBlank(metadata.getKey())) {
        loadInstalledPlugin(metadata);
      }
    }
  }

  private void loadInstalledPlugin(PluginMetadata metadata) {
    if (BLACKLISTED_PLUGINS.contains(metadata.getKey())) {
      LOG.warn("Plugin {} is blacklisted. Please uninstall it.", metadata.getName());
    } else {
      PluginMetadata existing = pluginByKeys.put(metadata.getKey(), metadata);
      if (existing != null) {
        throw MessageException.of(format("Found two files for the same plugin '%s': %s and %s",
          metadata.getKey(), metadata.getFile().getName(), existing.getFile().getName()));
      }
    }
  }

  private void moveDownloadedPlugins() {
    if (fs.getDownloadedPluginsDir().exists()) {
      for (File sourceFile : listJarFiles(fs.getDownloadedPluginsDir())) {
        overridePlugin(sourceFile, true);
      }
    }
  }

  private void copyBundledPlugins() {
    if (serverUpgradeStatus.isFreshInstall()) {
      for (File sourceFile : fs.getBundledPlugins()) {
        PluginMetadata metadata = installer.fileToPlugin().apply(sourceFile);
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
      deleteQuietly(destFile);
    }

    try {
      if (deleteSource) {
        moveFile(sourceFile, destFile);
      } else {
        copyFile(sourceFile, destFile, true);
      }
    } catch (IOException e) {
      LOG.error(format("Fail to move or copy plugin: %s to %s",
        sourceFile.getAbsolutePath(), destFile.getAbsolutePath()), e);
    }

    PluginMetadata metadata = installer.fileToPlugin().apply(destFile);
    if (isNotBlank(metadata.getKey())) {
      PluginMetadata existing = pluginByKeys.put(metadata.getKey(), metadata);
      if (existing != null) {
        if (!existing.getFile().getName().equals(destFile.getName())) {
          deleteQuietly(existing.getFile());
        }
        LOG.info("Plugin " + metadata.getKey() + " replaced by new version");
      }
    }
  }

  private void loadCorePlugins() {
    for (File file : fs.getCorePlugins()) {
      PluginMetadata metadata = installer.fileToCorePlugin().apply(file);
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
        moveFileToDirectory(masterFile, fs.getTrashPluginsDir(), true);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to uninstall plugin: " + pluginKey, e);
      }
    }
  }

  public List<String> getUninstalledPluginFilenames() {
    if (!fs.getTrashPluginsDir().exists()) {
      return Collections.emptyList();
    }

    return newArrayList(transform(listJarFiles(fs.getTrashPluginsDir()), FileToName.INSTANCE));
  }

  /**
   * @return the list of plugins to be uninstalled as {@link DefaultPluginMetadata} instances
   */
  public Collection<DefaultPluginMetadata> getUninstalledPlugins() {
    if (!fs.getTrashPluginsDir().exists()) {
      return Collections.emptyList();
    }

    return newArrayList(transform(listJarFiles(fs.getTrashPluginsDir()), installer.fileToPlugin()));
  }

  public void cancelUninstalls() {
    if (fs.getTrashPluginsDir().exists()) {
      for (File file : listJarFiles(fs.getTrashPluginsDir())) {
        try {
          moveFileToDirectory(file, fs.getUserPluginsDir(), false);
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
    LOG.info("Deploy plugin {}", SLASH_JOINER.join(plugin.getName(), plugin.getVersion(), plugin.getImplementationBuild()));

    if (!plugin.isCompatibleWith(server.getVersion())) {
      throw MessageException.of(format(
        "Plugin %s needs a more recent version of SonarQube than %s. At least %s is expected",
        plugin.getKey(), server.getVersion(), plugin.getSonarVersion()));
    }

    try {
      File pluginDeployDir = new File(fs.getDeployedPluginsDir(), plugin.getKey());
      forceMkdir(pluginDeployDir);
      cleanDirectory(pluginDeployDir);

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

  private static Collection<File> listJarFiles(File pluginDir) {
    return listFiles(pluginDir, new String[] {FILE_EXTENSION_JAR}, false);
  }

  private enum FileToName implements Function<File, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull File file) {
      return file.getName();
    }
  }
}
