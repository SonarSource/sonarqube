/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api;

import org.sonar.api.platform.PluginRepository;

import java.util.Collection;

/**
 * Plugins dictionnary. This class is for internal use
 *
 * @since 1.10
 * @deprecated
 */
public class Plugins {

  private PluginRepository pluginProvider;

  /**
   * Creates the dictionnary of plugins
   */
  public Plugins(PluginRepository pluginProvider) {
    this.pluginProvider = pluginProvider;
  }

  /**
   * Gives a collection of available plugins in the Sonar instance
   */
  public Collection<Plugin> getPlugins() {
    return pluginProvider.getPlugins();
  }

  /**
   * Returns a plugin based on its key
   */
  public Plugin getPlugin(String key) {
    return pluginProvider.getPlugin(key);
  }

  /**
   * Returns a plugin based on its extension
   */
  public Plugin getPluginByExtension(Extension extension) {
    return pluginProvider.getPluginForExtension(extension);
  }

  /**
   * Returns the list of properties of a plugin
   */
  public Property[] getProperties(Plugin plugin) {
    return pluginProvider.getProperties(plugin);
  }
}