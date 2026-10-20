/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;

public class ServerPlugin {
  private final PluginInfo pluginInfo;
  private final PluginType type;
  private final Plugin instance;
  private final FileAndMd5 jar;
  private final ClassLoader classloader;
  private final File resourcesArchive;

  public ServerPlugin(PluginInfo pluginInfo, PluginType type, Plugin instance, FileAndMd5 jar) {
    this(pluginInfo, type, instance, jar, instance.getClass().getClassLoader(), null);
  }

  public ServerPlugin(PluginInfo pluginInfo, PluginType type, Plugin instance, FileAndMd5 jar, ClassLoader classloader) {
    this(pluginInfo, type, instance, jar, classloader, null);
  }

  public ServerPlugin(PluginInfo pluginInfo, PluginType type, Plugin instance, FileAndMd5 jar, ClassLoader classloader, @Nullable File resourcesArchive) {
    this.pluginInfo = pluginInfo;
    this.type = type;
    this.instance = instance;
    this.jar = jar;
    this.classloader = classloader;
    this.resourcesArchive = resourcesArchive;
  }

  public PluginInfo getPluginInfo() {
    return pluginInfo;
  }

  public Plugin getInstance() {
    return instance;
  }

  public PluginType getType() {
    return type;
  }

  public FileAndMd5 getJar() {
    return jar;
  }

  public ClassLoader getClassloader() {
    return classloader;
  }

  /**
   * A deploy-dir copy of the originally-installed archive, set only for a ZIP-packaged plugin.
   * See {@link org.sonar.core.platform.ExplodedPlugin#getResourcesArchive()}.
   */
  @CheckForNull
  public File getResourcesArchive() {
    return resourcesArchive;
  }
}
