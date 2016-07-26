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

/**
 * Replaces the incomplete {@link org.sonar.api.platform.ServerFileSystem} as many directories can't be
 * published in API.
 *
 * @since 6.0
 */
public interface ServerFileSystem {

  /**
   * Directory that contains the persisted data to be kept on restarts and upgrades.
   * @return an existing directory
   */
  File getDataDir();

  /**
   * Directory accessible by scanners through web server
   * @return a non-null directory that MAY exist
   */
  File getDeployDir();

  /**
   * Root directory of the server installation
   * @return an existing directory
   */
  File getHomeDir();

  /**
   * Temporary directory, clean up on restarts
   * @return an existing directory
   */
  File getTempDir();

  /**
   * Files of plugins published by web server for scanners
   * @return a non-null directory that MAY exist
   */
  File getDeployedPluginsDir();

  /**
   * Directory of plugins downloaded through update center. Files
   * will be moved to {@link #getInstalledPluginsDir()} on startup.
   * @return an existing directory
   */
  File getDownloadedPluginsDir();

  /**
   * Directory of currently installed plugins. Used at startup.
   * @return an existing directory
   */
  File getInstalledPluginsDir();

  /**
   * Directory of the plugins packaged by default with application. These
   * plugins are installed during the first fresh startup.
   * @return an existing directory
   */
  File getBundledPluginsDir();

  /**
   * The file listing all the installed plugins. Used by scanner only.
   * @return an existing file
   */
  File getPluginIndex();
}
