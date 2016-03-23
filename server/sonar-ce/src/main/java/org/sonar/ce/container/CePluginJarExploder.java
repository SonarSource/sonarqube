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
package org.sonar.ce.container;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;

import static org.apache.commons.io.FileUtils.forceMkdir;

/**
 * Explode plugin JARs into a temporary directory
 */
public class CePluginJarExploder extends PluginJarExploder implements Startable {

  private final TempFolder tempFolder;
  private File rootDir;

  public CePluginJarExploder(TempFolder tempFolder) {
    this.tempFolder = tempFolder;
  }

  @Override
  public void start() {
    this.rootDir = tempFolder.newDir("ce-exploded-plugins");
    try {
      org.sonar.core.util.FileUtils.cleanDirectory(rootDir);
    } catch (IOException e) {
      throw new IllegalStateException("Can not clean " + rootDir, e);
    }
  }

  @Override
  public void stop() {

  }

  @Override
  public ExplodedPlugin explode(PluginInfo pluginInfo) {
    File toDir = new File(rootDir, pluginInfo.getKey());
    try {
      forceMkdir(toDir);

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
