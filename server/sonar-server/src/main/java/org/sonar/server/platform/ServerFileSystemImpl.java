/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform;

import java.io.File;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.app.TomcatContexts;

import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class ServerFileSystemImpl implements ServerFileSystem, org.sonar.api.platform.ServerFileSystem, Startable {

  private static final Logger LOGGER = Loggers.get(ServerFileSystemImpl.class);

  private final File homeDir;
  private final File tempDir;
  private final File dataDir;
  private final File deployDir;
  private final File uninstallDir;
  private final File editionUninstallDir;

  public ServerFileSystemImpl(Configuration config) {
    this.homeDir = new File(config.get(PATH_HOME.getKey()).get());
    this.tempDir = new File(config.get(PATH_TEMP.getKey()).get());
    this.dataDir = new File(config.get(PATH_DATA.getKey()).get());
    this.deployDir = new File(this.dataDir, TomcatContexts.WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR);
    this.uninstallDir = new File(getTempDir(), "uninstalled-plugins");
    this.editionUninstallDir = new File(getTempDir(), "uninstalled-edition-plugins");
  }

  @Override
  public void start() {
    LOGGER.info("SonarQube home: " + homeDir.getAbsolutePath());
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

  @Override
  public File getDataDir() {
    return dataDir;
  }

  @Override
  public File getDeployDir() {
    return deployDir;
  }

  @Override
  public File getDeployedPluginsDir() {
    return new File(getDeployDir(), "plugins");
  }

  @Override
  public File getDownloadedPluginsDir() {
    return new File(getHomeDir(), "extensions/downloads");
  }

  @Override
  public File getEditionDownloadedPluginsDir() {
    return new File(getHomeDir(), "extensions/new-edition");
  }

  @Override
  public File getInstalledPluginsDir() {
    return new File(getHomeDir(), "extensions/plugins");
  }

  @Override
  public File getBundledPluginsDir() {
    return new File(getHomeDir(), "lib/bundled-plugins");
  }

  @Override
  public File getPluginIndex() {
    return new File(getDeployDir(), "plugins/index.txt");
  }

  @Override
  public File getUninstalledPluginsDir() {
    return uninstallDir;
  }

  @Override
  public File getEditionUninstalledPluginsDir() {
    return editionUninstallDir;
  }

}
