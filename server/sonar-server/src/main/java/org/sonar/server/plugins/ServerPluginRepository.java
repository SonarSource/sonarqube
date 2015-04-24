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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.Plugin;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.DefaultServerFileSystem;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.apache.commons.io.FileUtils.moveFileToDirectory;

/**
 * Manages installation and loading of plugins:
 * <ul>
 *   <li>installation of bundled plugins on first server startup</li>
 *   <li>installation of new plugins (effective after server startup)</li>
 *   <li>un-installation of plugins (effective after server startup)</li>
 *   <li>cancel pending installations/un-installations</li>
 *   <li>load plugin bytecode</li>
 * </ul>
 */
public class ServerPluginRepository implements PluginRepository, Startable {

  private static final Logger LOG = Loggers.get(ServerPluginRepository.class);
  private static final String FILE_EXTENSION_JAR = "jar";
  private static final Set<String> DEFAULT_BLACKLISTED_PLUGINS = ImmutableSet.of("scmactivity", "issuesreport");
  private static final Joiner SLASH_JOINER = Joiner.on(" / ").skipNulls();

  private final Server server;
  private final DefaultServerFileSystem fs;
  private final ServerUpgradeStatus upgradeStatus;
  private final PluginLoader loader;
  private Set<String> blacklistedPluginKeys = DEFAULT_BLACKLISTED_PLUGINS;

  // following fields are available after startup
  private final Map<String, PluginInfo> pluginInfosByKeys = new HashMap<>();
  private final Map<String, Plugin> pluginInstancesByKeys = new HashMap<>();

  public ServerPluginRepository(Server server, ServerUpgradeStatus upgradeStatus,
    DefaultServerFileSystem fs, PluginLoader loader) {
    this.server = server;
    this.upgradeStatus = upgradeStatus;
    this.fs = fs;
    this.loader = loader;
  }

  @VisibleForTesting
  void setBlacklistedPluginKeys(Set<String> keys) {
    this.blacklistedPluginKeys = keys;
  }

  @Override
  public void start() {
    loadPreInstalledPlugins();
    copyBundledPlugins();
    moveDownloadedPlugins();
    loadCorePlugins();
    unloadIncompatiblePlugins();
    logInstalledPlugins();
    loadInstances();
  }

  @Override
  public void stop() {
    // close classloaders
    loader.unload(pluginInstancesByKeys.values());

    pluginInstancesByKeys.clear();
    pluginInfosByKeys.clear();
  }

  /**
   * Load the plugins that are located in extensions/plugins. Blacklisted plugins are
   * deleted.
   */
  private void loadPreInstalledPlugins() {
    for (File file : listJarFiles(fs.getInstalledPluginsDir())) {
      PluginInfo info = PluginInfo.create(file);
      registerPluginInfo(info);
    }
  }

  /**
   * Move the plugins recently downloaded to extensions/plugins.
   */
  private void moveDownloadedPlugins() {
    if (fs.getDownloadedPluginsDir().exists()) {
      for (File sourceFile : listJarFiles(fs.getDownloadedPluginsDir())) {
        overrideAndRegisterPlugin(sourceFile, true);
      }
    }
  }

  /**
   * Copies the plugins bundled with SonarQube distribution to directory extensions/plugins.
   * Does nothing if not a fresh installation.
   */
  private void copyBundledPlugins() {
    if (upgradeStatus.isFreshInstall()) {
      for (File sourceFile : listJarFiles(fs.getBundledPluginsDir())) {
        PluginInfo info = PluginInfo.create(sourceFile);
        // lib/bundled-plugins should be copied only if the plugin is not already
        // available in extensions/plugins
        if (!pluginInfosByKeys.containsKey(info.getKey())) {
          overrideAndRegisterPlugin(sourceFile, false);
        }
      }
    }
  }

  private void registerPluginInfo(PluginInfo info) {
    if (blacklistedPluginKeys.contains(info.getKey())) {
      LOG.warn("Plugin {} [{}] is blacklisted and is being uninstalled.", info.getName(), info.getKey());
      deleteQuietly(info.getFile());
      return;
    }
    PluginInfo existing = pluginInfosByKeys.put(info.getKey(), info);
    if (existing != null) {
      throw MessageException.of(format("Found two files for the same plugin [%s]: %s and %s",
        info.getKey(), info.getFile().getName(), existing.getFile().getName()));
    }

  }

  /**
   * Move or copy plugin to directory extensions/plugins. If a version of this plugin
   * already exists then it's deleted.
   */
  private void overrideAndRegisterPlugin(File sourceFile, boolean deleteSource) {
    File destDir = fs.getInstalledPluginsDir();
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

    PluginInfo info = PluginInfo.create(destFile);
    PluginInfo existing = pluginInfosByKeys.put(info.getKey(), info);
    if (existing != null) {
      if (!existing.getFile().getName().equals(destFile.getName())) {
        deleteQuietly(existing.getFile());
      }
      LOG.info("Plugin {} [{}] updated to version {}", info.getName(), info.getKey(), info.getVersion());
    } else {
      LOG.info("Plugin {} [{}] installed", info.getName(), info.getKey());
    }
  }

  private void loadCorePlugins() {
    for (File file : listJarFiles(fs.getCorePluginsDir())) {
      PluginInfo info = PluginInfo.create(file).setCore(true);
      registerPluginInfo(info);
    }
  }

  /**
   * Removes the plugins that are not compatible with current environment. In some cases
   * plugin files can be deleted.
   */
  private void unloadIncompatiblePlugins() {
    // loop as long as the previous loop ignored some plugins. That allows to support dependencies
    // on many levels, for example D extends C, which extends B, which requires A. If A is not installed,
    // then B, C and D must be ignored. That's not possible to achieve this algorithm with a single
    // iteration over plugins.
    List<String> removedKeys = new ArrayList<>();
    do {
      removedKeys.clear();
      for (PluginInfo plugin : pluginInfosByKeys.values()) {
        if (!Strings.isNullOrEmpty(plugin.getBasePlugin()) && !pluginInfosByKeys.containsKey(plugin.getBasePlugin())) {
          // this plugin extends a plugin that is not installed
          LOG.warn("Plugin {} [{}] is ignored because its base plugin [{}] is not installed", plugin.getName(), plugin.getKey(), plugin.getBasePlugin());
          removedKeys.add(plugin.getKey());
        }

        if (!plugin.isCompatibleWith(server.getVersion())) {
          LOG.warn("Plugin {} [{}] is ignored because it requires at least SonarQube {}", plugin.getName(), plugin.getKey(), plugin.getMinimalSqVersion());
          removedKeys.add(plugin.getKey());
        }

        for (PluginInfo.RequiredPlugin requiredPlugin : plugin.getRequiredPlugins()) {
          PluginInfo available = pluginInfosByKeys.get(requiredPlugin.getKey());
          if (available == null) {
            // this plugin requires a plugin that is not installed
            LOG.warn("Plugin {} [{}] is ignored because the required plugin [{}] is not installed", plugin.getName(), plugin.getKey(), requiredPlugin.getKey());
            removedKeys.add(plugin.getKey());
          } else if (requiredPlugin.getMinimalVersion().compareToIgnoreQualifier(available.getVersion()) > 0) {
            LOG.warn("Plugin {} [{}] is ignored because the version {} of required plugin [{}] is not supported", plugin.getName(), plugin.getKey(),
              requiredPlugin.getKey(), requiredPlugin.getMinimalVersion());
            removedKeys.add(plugin.getKey());
          }
        }
      }
      for (String removedKey : removedKeys) {
        pluginInfosByKeys.remove(removedKey);
      }
    } while (!removedKeys.isEmpty());
  }

  private void logInstalledPlugins() {
    List<PluginInfo> orderedPlugins = Ordering.natural().sortedCopy(pluginInfosByKeys.values());
    for (PluginInfo plugin : orderedPlugins) {
      LOG.info("Plugin: {}", SLASH_JOINER.join(plugin.getName(), plugin.getVersion(), plugin.getImplementationBuild()));
    }
  }

  private void loadInstances() {
    pluginInstancesByKeys.clear();
    pluginInstancesByKeys.putAll(loader.load(pluginInfosByKeys));
  }

  /**
   * Uninstall a plugin and its dependents (the plugins that require the plugin to be uninstalled)
   */
  public void uninstall(String pluginKey) {
    for (PluginInfo otherPlugin : pluginInfosByKeys.values()) {
      if (!otherPlugin.getKey().equals(pluginKey)) {
        for (PluginInfo.RequiredPlugin requiredPlugin : otherPlugin.getRequiredPlugins()) {
          if (requiredPlugin.getKey().equals(pluginKey)) {
            uninstall(otherPlugin.getKey());
          }
        }
      }
    }

    PluginInfo info = pluginInfosByKeys.get(pluginKey);
    if (!info.isCore()) {
      try {
        // we don't reuse info.getFile() just to be sure that file is located in from extensions/plugins
        File masterFile = new File(fs.getInstalledPluginsDir(), info.getFile().getName());
        moveFileToDirectory(masterFile, uninstalledPluginsDir(), true);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to uninstall plugin [" + pluginKey + "]", e);
      }
    }
  }

  public List<String> getUninstalledPluginFilenames() {
    return newArrayList(transform(listJarFiles(uninstalledPluginsDir()), FileToName.INSTANCE));
  }

  /**
   * @return the list of plugins to be uninstalled as {@link PluginInfo} instances
   */
  public Collection<PluginInfo> getUninstalledPlugins() {
    return newArrayList(transform(listJarFiles(uninstalledPluginsDir()), PluginInfo.JarToPluginInfo.INSTANCE));
  }

  public void cancelUninstalls() {
    for (File file : listJarFiles(uninstalledPluginsDir())) {
      try {
        moveFileToDirectory(file, fs.getInstalledPluginsDir(), false);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to cancel plugin uninstalls", e);
      }
    }
  }

  public Map<String, PluginInfo> getPluginInfosByKeys() {
    return pluginInfosByKeys;
  }

  @Override
  public Collection<PluginInfo> getPluginInfos() {
    return pluginInfosByKeys.values();
  }

  @Override
  public PluginInfo getPluginInfo(String key) {
    PluginInfo info = pluginInfosByKeys.get(key);
    if (info == null) {
      throw new IllegalArgumentException(String.format("Plugin [%s] does not exist", key));
    }
    return info;
  }

  @Override
  public Plugin getPluginInstance(String key) {
    Plugin plugin = pluginInstancesByKeys.get(key);
    if (plugin == null) {
      throw new IllegalArgumentException(String.format("Plugin [%s] does not exist", key));
    }
    return plugin;
  }

  @Override
  public boolean hasPlugin(String key) {
    return pluginInfosByKeys.containsKey(key);
  }

  private enum FileToName implements Function<File, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull File file) {
      return file.getName();
    }

  }

  /**
   * @return existing trash dir
   */
  private File uninstalledPluginsDir() {
    File dir = new File(fs.getTempDir(), "uninstalled-plugins");
    try {
      FileUtils.forceMkdir(dir);
      return dir;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp directory: " + dir.getAbsolutePath(), e);
    }
  }

  private static Collection<File> listJarFiles(File dir) {
    if (dir.exists()) {
      return FileUtils.listFiles(dir, new String[] {FILE_EXTENSION_JAR}, false);
    }
    return Collections.emptyList();
  }
}
