/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.plugins;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.server.platform.ServerFileSystem;

import static org.apache.commons.io.FileUtils.forceMkdir;

@ServerSide
public class ServerPluginJarExploder extends PluginJarExploder {
  private final ServerFileSystem fs;

  public ServerPluginJarExploder(ServerFileSystem fs) {
    this.fs = fs;
  }

  /**
   * JAR files of directory extensions/plugins can be moved when server is up and plugins are uninstalled.
   * For this reason these files must not be locked by classloaders. They are copied to the directory
   * web/deploy/plugins in order to be loaded by {@link PluginClassLoader}.
   */
  @Override
  public ExplodedPlugin explode(PluginInfo plugin) {
    File toDir = new File(fs.getDeployedPluginsDir(), plugin.getKey());
    try {
      forceMkdir(toDir);
      org.sonar.core.util.FileUtils.cleanDirectory(toDir);

      File jarTarget = new File(toDir, plugin.getNonNullJarFile().getName());

      FileUtils.copyFile(plugin.getNonNullJarFile(), jarTarget);
      ZipUtils.unzip(plugin.getNonNullJarFile(), toDir, newLibFilter());
      return explodeFromUnzippedDir(plugin, jarTarget, toDir);
    } catch (Exception e) {
      throw new IllegalStateException(String.format(
        "Fail to unzip plugin [%s] %s to %s", plugin.getKey(), plugin.getNonNullJarFile().getAbsolutePath(), toDir.getAbsolutePath()), e);
    }
  }
}
