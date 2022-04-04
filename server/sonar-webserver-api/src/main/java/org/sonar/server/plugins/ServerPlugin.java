/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;

public class ServerPlugin {
  private final PluginInfo pluginInfo;
  private final PluginType type;
  private final Plugin instance;
  private final FileAndMd5 jar;
  private final FileAndMd5 compressed;
  private final ClassLoader classloader;

  public ServerPlugin(PluginInfo pluginInfo, PluginType type, Plugin instance, FileAndMd5 jar, @Nullable FileAndMd5 compressed) {
    this(pluginInfo, type, instance, jar, compressed, instance.getClass().getClassLoader());
  }

  public ServerPlugin(PluginInfo pluginInfo, PluginType type, Plugin instance, FileAndMd5 jar, @Nullable FileAndMd5 compressed, ClassLoader classloader) {
    this.pluginInfo = pluginInfo;
    this.type = type;
    this.instance = instance;
    this.jar = jar;
    this.compressed = compressed;
    this.classloader = classloader;
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

  @CheckForNull
  public FileAndMd5 getCompressed() {
    return compressed;
  }

  public ClassLoader getClassloader() {
    return classloader;
  }
}
