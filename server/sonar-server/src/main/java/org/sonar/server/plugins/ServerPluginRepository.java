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

import org.picocontainer.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.core.plugins.PluginClassloaders;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.Map;

/**
 * This class loads JAR files and initializes classloaders of plugins
 */
public class ServerPluginRepository implements PluginRepository, Startable {

  private final ServerPluginJarsInstaller jarsInstaller;
  private final PluginClassloaders classloaders;
  private Map<String, Plugin> pluginsByKey;

  public ServerPluginRepository(ServerPluginJarsInstaller jarsInstaller) {
    this.classloaders = new PluginClassloaders(getClass().getClassLoader());
    this.jarsInstaller = jarsInstaller;
  }

  @Override
  public void start() {
    jarsInstaller.install();
    Collection<PluginMetadata> metadata = jarsInstaller.getMetadata();
    pluginsByKey = classloaders.init(metadata);
  }

  @Override
  public void stop() {
    classloaders.clean();
  }

  @Override
  public Plugin getPlugin(String key) {
    return pluginsByKey.get(key);
  }

  @CheckForNull
  public ClassLoader getClassLoader(String pluginKey) {
    return classloaders.get(pluginKey);
  }

  @CheckForNull
  public Class getClass(String pluginKey, String classname) {
    Class clazz = null;
    ClassLoader classloader = getClassLoader(pluginKey);
    if (classloader != null) {
      try {
        clazz = classloader.loadClass(classname);

      } catch (ClassNotFoundException e) {
        Loggers.get(getClass()).warn("Class not found in plugin " + pluginKey + ": " + classname, e);
      }
    }
    return clazz;
  }

  @Override
  public Collection<PluginMetadata> getMetadata() {
    return jarsInstaller.getMetadata();
  }

  @Override
  public PluginMetadata getMetadata(String pluginKey) {
    return jarsInstaller.getMetadata(pluginKey);
  }

}
