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
package org.sonar.server.platform;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.utils.Logs;
import org.sonar.jpa.session.DatabaseConnector;
import org.sonar.server.configuration.CoreConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Introspect the filesystem and the classloader to get extension files at startup.
 *
 * @since 2.2
 */
public class DefaultServerFileSystem implements ServerFileSystem {

  private DatabaseConnector databaseConnector;
  private File deployDir;
  private File homeDir;

  public DefaultServerFileSystem(DatabaseConnector databaseConnector, Configuration configuration) {
    this.databaseConnector = databaseConnector;
    this.homeDir = new File(configuration.getString(CoreProperties.SONAR_HOME));

    String deployPath = configuration.getString(CoreConfiguration.DEPLOY_DIR);
    if (StringUtils.isNotBlank(deployPath)) {
      this.deployDir = new File(deployPath);
    }
  }

  /**
   * for unit tests
   */
  public DefaultServerFileSystem(DatabaseConnector databaseConnector, File homeDir, File deployDir) {
    this.databaseConnector = databaseConnector;
    this.deployDir = deployDir;
    this.homeDir = homeDir;
  }

  public void start() {
    Logs.INFO.info("Sonar home:" + homeDir.getAbsolutePath());

    if (deployDir == null) {
      throw new ServerStartException("The target directory to deploy libraries is not set");
    }
    try {
      Logs.INFO.info("Deploy dir:" + deployDir.getAbsolutePath());
      FileUtils.forceMkdir(deployDir);
      FileUtils.cleanDirectory(deployDir);

    } catch (IOException e) {
      throw new ServerStartException("The following directory can not be created: " + deployDir.getAbsolutePath(), e);
    }

    File deprecated = getDeprecatedPluginsDir();
    try {
      FileUtils.forceMkdir(deprecated);
      FileUtils.cleanDirectory(deprecated);

    } catch (IOException e) {
      throw new ServerStartException("The following directory can not be created: " + deprecated.getAbsolutePath(), e);
    }
  }

  public File getHomeDir() {
    return homeDir;
  }

  public File getDeployDir() {
    return deployDir;
  }

  public File getDeployedJdbcDriver() {
    return new File(deployDir, "jdbc-driver.jar");
  }

  public File getDeployedPluginsDir() {
    return new File(deployDir, "plugins");
  }

  public File getDownloadedPluginsDir() {
    return new File(getHomeDir(), "extensions/downloads");
  }

  public File getJdbcDriver() {
    String dialect = databaseConnector.getDialect().getId();
    File dir = new File(getHomeDir(), "/extensions/jdbc-driver/" + dialect + "/");
    List<File> jars = getFiles(dir, "jar");
    if (jars.isEmpty()) {
      throw new ServerStartException("No JDBC driver found in " + dir.getAbsolutePath());
    }
    if (jars.size() > 1) {
      throw new ServerStartException("The directory " + dir.getAbsolutePath() + " accepts only one JAR file");
    }
    return jars.get(0);
  }

  public List<File> getCorePlugins() {
    File corePluginsDir = new File(getHomeDir(), "lib/core-plugins");
    return getFiles(corePluginsDir, "jar");
  }

  public List<File> getUserPlugins() {
    File pluginsDir = getUserPluginsDir();
    return getFiles(pluginsDir, "jar");
  }

  public File getUserPluginsDir() {
    return new File(getHomeDir(), "extensions/plugins");
  }

  public File getDeprecatedPluginsDir() {
    return new File(getHomeDir(), "extensions/deprecated");
  }

  public List<File> getPluginExtensions() {
    return getFiles(getPluginExtensionsDir(), "jar");
  }

  public File getPluginExtensionsDir() {
    return new File(getHomeDir(), "extensions/rules");
  }

  public List<File> getExtensions(String dirName, String... suffixes) {
    File dir = new File(getHomeDir(), "extensions/rules/" + dirName);
    if (dir.exists() && dir.isDirectory()) {
      return getFiles(dir, suffixes);
    }
    return Collections.emptyList();
  }

  public File getPluginExtensionsDir(String pluginKey) {
    return new File(getHomeDir(), "extensions/rules/" + pluginKey);
  }

  public List<File> getPluginExtensionXml(String pluginKey) {
    File dir = new File(getHomeDir(), "extensions/rules/" + pluginKey);
    if (dir.exists() && dir.isDirectory()) {
      return getFiles(dir, "xml");
    }
    return Collections.emptyList();
  }

  public File getMaven2Plugin() {
    File dir = new File(getHomeDir(), "lib/deprecated-maven-plugin/");
    List<File> files = getFiles(dir, "jar");
    if (files.isEmpty()) {
      return null;
    }
    return files.get(0);
  }

  private List<File> getFiles(File dir, String... fileSuffixes) {
    List<File> files = new ArrayList<File>();
    if (dir != null && dir.exists()) {
      if (fileSuffixes != null && fileSuffixes.length>0) {
        files.addAll(FileUtils.listFiles(dir, fileSuffixes, false));
      } else {
        files.addAll(FileUtils.listFiles(dir, null, false));
      }
    }
    return files;
  }
}
