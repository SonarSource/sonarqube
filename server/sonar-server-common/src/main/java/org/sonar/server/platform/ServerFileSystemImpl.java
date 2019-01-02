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
package org.sonar.server.platform;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class ServerFileSystemImpl implements ServerFileSystem, org.sonar.api.platform.ServerFileSystem, Startable {

  private static final Logger LOGGER = Loggers.get(ServerFileSystemImpl.class);

  private final File homeDir;
  private final File tempDir;
  private final File deployDir;
  private final File uninstallDir;

  public ServerFileSystemImpl(Configuration config) {
    this.homeDir = createDir(new File(config.get(PATH_HOME.getKey()).get()));
    this.tempDir = createDir(new File(config.get(PATH_TEMP.getKey()).get()));
    File dataDir = createDir(new File(config.get(PATH_DATA.getKey()).get()));
    this.deployDir = new File(dataDir, "web/deploy");
    this.uninstallDir = new File(getTempDir(), "uninstalled-plugins");
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
  public File getDeployedPluginsDir() {
    return new File(deployDir, "plugins");
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
  public File getPluginIndex() {
    return new File(deployDir, "plugins/index.txt");
  }

  @Override
  public File getUninstalledPluginsDir() {
    return uninstallDir;
  }

  private static File createDir(File dir) {
    try {
      FileUtils.forceMkdir(dir);
      return dir;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create directory " + dir, e);
    }
  }
}
