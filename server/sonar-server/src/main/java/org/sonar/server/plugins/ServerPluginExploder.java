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

import org.apache.commons.io.FileUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginExploder;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;

import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.forceMkdir;

public class ServerPluginExploder extends PluginExploder implements ServerComponent {

  private final DefaultServerFileSystem fs;

  public ServerPluginExploder(DefaultServerFileSystem fs) {
    this.fs = fs;
  }

  /**
   * JAR files of directory extensions/plugins can be moved when server is up and plugins are uninstalled.
   * For this reason these files must not be locked by classloaders. They are copied to the directory
   * web/deploy/plugins in order to be loaded by {@link org.sonar.core.platform.PluginLoader}.
   */
  @Override
  public ExplodedPlugin explode(PluginInfo pluginInfo) {
    File toDir = new File(fs.getDeployedPluginsDir(), pluginInfo.getKey());
    try {
      forceMkdir(toDir);
      cleanDirectory(toDir);

      File jarSource = pluginInfo.getNonNullJarFile();
      File jarTarget = new File(toDir, jarSource.getName());
      FileUtils.copyFile(jarSource, jarTarget);
      ZipUtils.unzip(jarSource, toDir, newLibFilter());
      return explodeFromUnzippedDir(pluginInfo.getKey(), jarTarget, toDir);
    } catch (Exception e) {
      throw new IllegalStateException(String.format(
        "Fail to unzip plugin [%s] %s to %s", pluginInfo.getKey(), pluginInfo.getNonNullJarFile().getAbsolutePath(), toDir.getAbsolutePath()), e);
    }
  }
}
