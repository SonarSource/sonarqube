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
package org.sonar.server.platform;

import java.io.File;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.server.app.TomcatContexts;

import static java.util.Objects.requireNonNull;

public class ServerFileSystemImpl implements ServerFileSystem, org.sonar.api.platform.ServerFileSystem, Startable {

  private static final Logger LOGGER = Loggers.get(ServerFileSystemImpl.class);

  private final File homeDir;
  private final File tempDir;
  private final File dataDir;
  private final File deployDir;

  public ServerFileSystemImpl(Settings settings) {
    this.homeDir = new File(requireNonNull(settings.getString(ProcessProperties.PATH_HOME)));
    this.tempDir = new File(requireNonNull(settings.getString(ProcessProperties.PATH_TEMP)));
    this.dataDir = new File(requireNonNull(settings.getString(ProcessProperties.PATH_DATA)));
    this.deployDir = new File(this.dataDir, TomcatContexts.WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR);
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

}
