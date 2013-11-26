/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.bootstrap;

import org.sonar.api.BatchComponent;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.core.plugins.PluginInstaller;
import org.sonar.home.cache.FileCache;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;

public class BatchPluginInstaller extends PluginInstaller implements BatchComponent {

  private FileCache cache;

  public BatchPluginInstaller(FileCache cache) {
    this.cache = cache;
  }

  public DefaultPluginMetadata installToCache(File pluginFile, boolean isCore) {
    DefaultPluginMetadata metadata = extractMetadata(pluginFile, isCore);
    install(metadata, null, pluginFile);
    return metadata;
  }

  @Override
  protected File extractPluginDependencies(File pluginFile, File pluginBasedir) throws IOException {
    return cache.unzip(pluginFile);
  }

  private void copyDependencies(DefaultPluginMetadata metadata, File pluginFile, File pluginBasedir) throws IOException {
    if (!metadata.getPathsToInternalDeps().isEmpty()) {
      // needs to unzip the jar
      File baseDir;
      if (pluginBasedir == null) {
        baseDir = cache.unzip(pluginFile);
      } else {
        ZipUtils.unzip(pluginFile, pluginBasedir, new LibFilter());
        baseDir = pluginBasedir;
      }
      for (String depPath : metadata.getPathsToInternalDeps()) {
        File dependency = new File(baseDir, depPath);
        if (!dependency.isFile() || !dependency.exists()) {
          throw new IllegalArgumentException("Dependency " + depPath + " can not be found in " + pluginFile.getName());
        }
        metadata.addDeployedFile(dependency);
      }
    }
  }

  private static final class LibFilter implements ZipUtils.ZipEntryFilter {
    public boolean accept(ZipEntry entry) {
      return entry.getName().startsWith("META-INF/lib");
    }
  }
}
