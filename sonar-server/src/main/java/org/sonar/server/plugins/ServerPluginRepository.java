/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import java.util.List;

import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.sonar.api.Plugin;
import org.sonar.api.ServerExtension;
import org.sonar.api.utils.SonarException;
import org.sonar.core.plugin.AbstractPluginRepository;

/**
 * @since 2.2
 */
public class ServerPluginRepository extends AbstractPluginRepository {

  private PluginClassLoaders classloaders;

  public ServerPluginRepository(PluginClassLoaders classloaders) {
    this.classloaders = classloaders;
  }

  /**
   * Only for unit tests
   */
  ServerPluginRepository() {
  }

  public void registerPlugins(MutablePicoContainer pico) {
    // Create ClassLoaders
    List<PluginMetadata> register = classloaders.completeCreation();
    // Register plugins
    for (PluginMetadata pluginMetadata : register) {
      try {
        Class pluginClass = classloaders.getClassLoader(pluginMetadata.getKey()).loadClass(pluginMetadata.getMainClass());
        pico.as(Characteristics.CACHE).addComponent(pluginClass);
        Plugin plugin = (Plugin) pico.getComponent(pluginClass);
        registerPlugin(pico, plugin, pluginMetadata.getKey());

      } catch (ClassNotFoundException e) {
        throw new SonarException(
            "Please check the plugin manifest. The main plugin class does not exist: " + pluginMetadata.getMainClass(), e);
      }
    }
    invokeExtensionProviders(pico);
  }

  @Override
  protected boolean shouldRegisterExtension(PicoContainer container, String pluginKey, Object extension) {
    return isType(extension, ServerExtension.class);
  }
}
