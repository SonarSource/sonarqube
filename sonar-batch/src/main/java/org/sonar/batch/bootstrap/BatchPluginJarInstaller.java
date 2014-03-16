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
package org.sonar.batch.bootstrap;

import org.sonar.api.BatchComponent;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.core.plugins.PluginJarInstaller;
import org.sonar.home.cache.FileCache;

import java.io.File;
import java.io.IOException;

public class BatchPluginJarInstaller extends PluginJarInstaller implements BatchComponent {

  private FileCache cache;

  public BatchPluginJarInstaller(FileCache cache) {
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

}
