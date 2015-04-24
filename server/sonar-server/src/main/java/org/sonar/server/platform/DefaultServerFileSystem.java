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
package org.sonar.server.platform;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Introspect the filesystem and the classloader to get extension files at startup.
 *
 * @since 2.2
 */
public class DefaultServerFileSystem implements ServerFileSystem, Startable {

  private static final Logger LOGGER = Loggers.get(DefaultServerFileSystem.class);

  private final Server server;
  private final File homeDir, tempDir;

  public DefaultServerFileSystem(Settings settings, Server server) {
    this.server = server;
    this.homeDir = new File(settings.getString(ProcessProperties.PATH_HOME));
    this.tempDir = new File(settings.getString(ProcessProperties.PATH_TEMP));
  }

  /**
   * for unit tests
   */
  public DefaultServerFileSystem(File homeDir, File tempDir, Server server) {
    this.homeDir = homeDir;
    this.tempDir = tempDir;
    this.server = server;
  }

  @Override
  public void start() {
    LOGGER.info("SonarQube home: " + homeDir.getAbsolutePath());

    File deployDir = getDeployDir();
    if (deployDir == null) {
      throw new IllegalArgumentException("Web app directory does not exist");
    }
    try {
      FileUtils.forceMkdir(deployDir);
      FileFilter fileFilter = FileFilterUtils.directoryFileFilter();
      File[] files = deployDir.listFiles(fileFilter);
      if (files != null) {
        for (File subDirectory : files) {
          FileUtils.cleanDirectory(subDirectory);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("The following directory can not be created: " + deployDir.getAbsolutePath(), e);
    }

    File deprecated = getDeprecatedPluginsDir();
    try {
      FileUtils.forceMkdir(deprecated);
      FileUtils.cleanDirectory(deprecated);
    } catch (IOException e) {
      throw new IllegalStateException("The following directory can not be created: " + deprecated.getAbsolutePath(), e);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  @Override
  public File getHomeDir() {
    return homeDir;
  }

  @Override
  public File getTempDir() {
    return tempDir;
  }

  @CheckForNull
  public File getDeployDir() {
    return server.getDeployDir();
  }

  public File getDeployedJdbcDriverIndex() {
    return new File(getDeployDir(), "jdbc-driver.txt");
  }

  public File getDeployedPluginsDir() {
    return new File(getDeployDir(), "plugins");
  }

  public File getDownloadedPluginsDir() {
    return new File(getHomeDir(), "extensions/downloads");
  }

  public File getInstalledPluginsDir() {
    return new File(getHomeDir(), "extensions/plugins");
  }

  public File getBundledPluginsDir() {
    return new File(getHomeDir(), "lib/bundled-plugins");
  }

  public File getCorePluginsDir() {
    return new File(getHomeDir(), "lib/core-plugins");
  }

  public File getDeprecatedPluginsDir() {
    return new File(getHomeDir(), "extensions/deprecated");
  }

  public File getPluginIndex() {
    return new File(getDeployDir(), "plugins/index.txt");
  }

  /**
   * @deprecated since 4.1
   */
  @Override
  @Deprecated
  public List<File> getExtensions(String dirName, String... suffixes) {
    File dir = new File(getHomeDir(), "extensions/rules/" + dirName);
    if (dir.exists() && dir.isDirectory()) {
      return getFiles(dir, suffixes);
    }
    return Collections.emptyList();
  }

  private List<File> getFiles(File dir, String... fileSuffixes) {
    List<File> files = new ArrayList<>();
    if (dir != null && dir.exists()) {
      if (fileSuffixes != null && fileSuffixes.length > 0) {
        files.addAll(FileUtils.listFiles(dir, fileSuffixes, false));
      } else {
        files.addAll(FileUtils.listFiles(dir, null, false));
      }
    }
    return files;
  }
}
