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
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.core.plugins.PluginJarInstaller;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;

public class ServerPluginJarInstaller extends PluginJarInstaller {

  public void installToDir(DefaultPluginMetadata metadata, File pluginBasedir) {
    try {
      File pluginFile = metadata.getFile();
      File deployedPlugin = copyPlugin(pluginBasedir, pluginFile);
      install(metadata, pluginBasedir, deployedPlugin);
    } catch (IOException e) {
      throw new IllegalStateException(FAIL_TO_INSTALL_PLUGIN + metadata, e);
    }
  }

  private File copyPlugin(File pluginBasedir, File pluginFile) throws IOException {
    FileUtils.forceMkdir(pluginBasedir);
    File targetFile = new File(pluginBasedir, pluginFile.getName());
    FileUtils.copyFile(pluginFile, targetFile);
    return targetFile;
  }

  @Override
  protected File extractPluginDependencies(File pluginFile, File pluginBasedir) throws IOException {
    ZipUtils.unzip(pluginFile, pluginBasedir, new LibFilter());
    return pluginBasedir;
  }

  private static final class LibFilter implements ZipUtils.ZipEntryFilter {
    @Override
    public boolean accept(ZipEntry entry) {
      return entry.getName().startsWith("META-INF/lib");
    }
  }
}
