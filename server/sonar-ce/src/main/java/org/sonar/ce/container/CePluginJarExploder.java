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
package org.sonar.ce.container;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;

/**
 * Explodes the plugin JARs of extensions/plugins/ into a temporary directory
 * dedicated to compute engine.
 */
public class CePluginJarExploder extends PluginJarExploder {

  private static final String TEMP_RELATIVE_PATH = "ce-exploded-plugins";
  private final ServerFileSystem fs;

  public CePluginJarExploder(ServerFileSystem fs) {
    this.fs = fs;
  }

  @Override
  public ExplodedPlugin explode(PluginInfo pluginInfo) {
    File tempDir = new File(fs.getTempDir(), TEMP_RELATIVE_PATH);
    File toDir = new File(tempDir, pluginInfo.getKey());
    try {
      org.sonar.core.util.FileUtils.cleanDirectory(toDir);

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
