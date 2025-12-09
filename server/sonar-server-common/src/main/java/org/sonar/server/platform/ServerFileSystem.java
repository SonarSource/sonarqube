/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

/**
 * Replaces the incomplete {@link org.sonar.api.platform.ServerFileSystem} as many directories can't be
 * published in API.
 */
public interface ServerFileSystem {

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
   * @return a directory which may or not exist
   */
  File getDeployedPluginsDir();

  /**
   * Directory of plugins downloaded through update center. Files
   * will be moved to {@link #getInstalledExternalPluginsDir()} on startup.
   * @return a directory which may or not exist
   */
  File getDownloadedPluginsDir();

  /**
   * Directory of currently installed external plugins. Used at startup.
   * @return a directory which may or not exist
   */
  File getInstalledExternalPluginsDir();

  /**
   * Directory of currently installed bundled plugins. Used at startup.
   * @return a directory which may or not exist
   */
  File getInstalledBundledPluginsDir();

  /**
   * Directory where plugins to be uninstalled are moved to.
   * @return a directory which may or not exist
   */
  File getUninstalledPluginsDir();

}
