/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.ServerComponent;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.platform.ServerStartException;

import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class PluginDeployer implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PluginDeployer.class);

  private Server server;
  private DefaultServerFileSystem fileSystem;
  private JpaPluginDao dao;
  private PluginClassLoaders classloaders;
  private Map<String, PluginMetadata> pluginByKeys = Maps.newHashMap();
  private Map<String, PluginMetadata> deprecatedPlugins = Maps.newHashMap();

  public PluginDeployer(Server server, DefaultServerFileSystem fileSystem, JpaPluginDao dao, PluginClassLoaders classloaders) {
    this.server = server;
    this.fileSystem = fileSystem;
    this.dao = dao;
    this.classloaders = classloaders;
  }

  public void start() throws IOException {
    TimeProfiler profiler = new TimeProfiler().start("Install plugins");

    loadUserPlugins();
    moveAndLoadDownloadedPlugins();
    loadCorePlugins();

    deployPlugins();
    deployDeprecatedPlugins();

    persistPlugins();
    profiler.stop();
  }

  private void persistPlugins() {
    List<JpaPlugin> previousPlugins = dao.getPlugins();
    List<JpaPlugin> installedPlugins = new ArrayList<JpaPlugin>();
    for (PluginMetadata plugin : pluginByKeys.values()) {
      JpaPlugin installed = searchPlugin(plugin, previousPlugins);
      if (installed == null) {
        installed = JpaPlugin.create(plugin.getKey());
        installed.setInstallationDate(server.getStartedAt());
      }
      plugin.copyTo(installed);
      installedPlugins.add(installed);
      Logs.INFO.info("Plugin: " + plugin.getName() + " " + StringUtils.defaultString(plugin.getVersion(), "-"));
    }
    dao.register(installedPlugins);
  }

  private JpaPlugin searchPlugin(PluginMetadata plugin, List<JpaPlugin> preinstalledList) {
    if (preinstalledList != null) {
      for (JpaPlugin p : preinstalledList) {
        if (StringUtils.equals(p.getKey(), plugin.getKey())) {
          return p;
        }
      }
    }
    return null;
  }

  private void deployPlugins() {
    for (PluginMetadata plugin : pluginByKeys.values()) {
      deploy(plugin);
    }
  }

  private void deployDeprecatedPlugins() throws IOException {
    for (PluginMetadata deprecatedPlugin : deprecatedPlugins.values()) {
      PluginMetadata metadata = pluginByKeys.get(deprecatedPlugin.getKey());
      if (metadata != null) {
        FileUtils.deleteQuietly(deprecatedPlugin.getSourceFile());
        Logs.INFO.info("Old plugin " + deprecatedPlugin.getFilename() + " replaced by new " + metadata.getFilename());
      } else {
        pluginByKeys.put(deprecatedPlugin.getKey(), deprecatedPlugin);
        deploy(deprecatedPlugin);
      }
    }
  }

  private void deploy(PluginMetadata plugin) {
    try {
      LOG.debug("Deploy plugin " + plugin);

      File deployDir = new File(fileSystem.getDeployedPluginsDir(), plugin.getKey());
      FileUtils.forceMkdir(deployDir);
      FileUtils.cleanDirectory(deployDir);

      File target = new File(deployDir, plugin.getFilename());
      FileUtils.copyFile(plugin.getSourceFile(), target);
      plugin.addDeployedFile(target);

      for (File extension : fileSystem.getExtensions(plugin.getKey())) {
        target = new File(deployDir, extension.getName());
        FileUtils.copyFile(extension, target);
        plugin.addDeployedFile(target);
      }

      if (plugin.getDependencyPaths().length > 0) {
        // needs to unzip the jar
        File tempDir = ZipUtils.unzipToTempDir(plugin.getSourceFile());
        for (String depPath : plugin.getDependencyPaths()) {
          File file = new File(tempDir, depPath);
          target = new File(deployDir, file.getName());
          FileUtils.copyFile(file, target);
          plugin.addDeployedFile(target);
        }
      }
      classloaders.create(plugin);

    } catch (IOException e) {
      throw new RuntimeException("Fail to deploy the plugin " + plugin, e);
    }
  }

  private void loadCorePlugins() throws IOException {
    for (File file : fileSystem.getCorePlugins()) {
      registerPluginMetadata(file, true, false);
    }
  }

  private void loadUserPlugins() throws IOException {
    for (File file : fileSystem.getUserPlugins()) {
      registerPluginMetadata(file, false, false);
    }
  }

  private void moveAndLoadDownloadedPlugins() throws IOException {
    if (fileSystem.getDownloadedPluginsDir().exists()) {
      Collection<File> jars = FileUtils.listFiles(fileSystem.getDownloadedPluginsDir(), new String[] { "jar" }, false);
      for (File jar : jars) {
        File movedJar = moveDownloadedFile(jar);
        registerPluginMetadata(movedJar, false, true);
      }
    }
  }

  private File moveDownloadedFile(File jar) {
    try {
      FileUtils.moveFileToDirectory(jar, fileSystem.getUserPluginsDir(), true);
      return new File(fileSystem.getUserPluginsDir(), jar.getName());

    } catch (IOException e) {
      LOG.error("Fail to move the downloaded file: " + jar.getAbsolutePath(), e);
      return null;
    }
  }

  private void registerPluginMetadata(File file, boolean corePlugin, boolean canDeleteOld) throws IOException {
    PluginMetadata metadata = PluginMetadata.createFromJar(file, corePlugin);
    String pluginKey = metadata.getKey();
    if (pluginKey != null) {
      registerPluginMetadata(pluginByKeys, file, metadata, canDeleteOld);
    } else if (metadata.isOldManifest()) {
      loadDeprecatedPlugin(metadata);
      registerPluginMetadata(deprecatedPlugins, file, metadata, canDeleteOld);
    }
  }

  private void registerPluginMetadata(Map<String, PluginMetadata> map, File file, PluginMetadata metadata, boolean canDeleteOld) {
    String pluginKey = metadata.getKey();
    PluginMetadata existing = map.get(pluginKey);
    if (existing != null) {
      if (canDeleteOld) {
        FileUtils.deleteQuietly(existing.getSourceFile());
        map.remove(pluginKey);
        Logs.INFO.info("Old plugin " + existing.getFilename() + " replaced by new " + metadata.getFilename());
      } else {
        throw new ServerStartException("Found two plugins with the same key '" + pluginKey + "': "
            + metadata.getFilename() + " and "
            + existing.getFilename());
      }
    }
    map.put(metadata.getKey(), metadata);
  }

  private void loadDeprecatedPlugin(PluginMetadata plugin) throws IOException {
    // URLClassLoader locks files on Windows
    // => copy the file before in a temp directory
    File tempFile = new File(fileSystem.getDeprecatedPluginsDir(), plugin.getFilename());
    FileUtils.copyFile(plugin.getSourceFile(), tempFile);

    String mainClass = plugin.getMainClass();
    try {
      URLClassLoader pluginClassLoader = URLClassLoader.newInstance(new URL[] { tempFile.toURI().toURL() }, getClass().getClassLoader());
      Plugin pluginInstance = (Plugin) pluginClassLoader.loadClass(mainClass).newInstance();
      plugin.setKey(pluginInstance.getKey());
      plugin.setDescription(pluginInstance.getDescription());
      plugin.setName(pluginInstance.getName());

    } catch (Exception e) {
      throw new RuntimeException("The plugin main class can not be created: plugin=" + plugin.getFilename() + ", class=" + mainClass, e);
    }

    if (StringUtils.isBlank(plugin.getKey())) {
      throw new ServerStartException("Found plugin with empty key: " + plugin.getFilename());
    }
  }
}
