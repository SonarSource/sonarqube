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
package org.sonar.api.platform;

import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @since 2.2
 */
public abstract class PluginRepository implements BatchComponent, ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PluginRepository.class);

  private Map<String, Plugin> pluginByKey = new HashMap<String, Plugin>();
  private Map<Object, Plugin> pluginByExtension = new IdentityHashMap<Object, Plugin>();

  public void registerPlugin(MutablePicoContainer container, Plugin plugin, Class<? extends Extension> extensionClass) {
    LOG.debug("Registering the plugin {}", plugin.getKey());
    pluginByKey.put(plugin.getKey(), plugin);
    for (Object extension : plugin.getExtensions()) {
      if (isExtension(extension, extensionClass)) {
        LOG.debug("Registering the extension: {}", extension);
        container.as(Characteristics.CACHE).addComponent(getExtensionKey(extension), extension);
        pluginByExtension.put(extension, plugin);
      }
    }
  }

  public Collection<Plugin> getPlugins() {
    return pluginByKey.values();
  }

  public Plugin getPlugin(String key) {
    return pluginByKey.get(key);
  }

  /**
   * Returns the list of properties of a plugin
   */
  public Property[] getProperties(Plugin plugin) {
    if (plugin != null) {
      Class<? extends Plugin> classInstance = plugin.getClass();
      if (classInstance.isAnnotationPresent(Properties.class)) {
        return classInstance.getAnnotation(Properties.class).value();
      }
    }
    return new Property[0];
  }

  public Property[] getProperties(String pluginKey) {
    return getProperties(pluginByKey.get(pluginKey));
  }

  public Plugin getPluginForExtension(Object extension) {
    Plugin plugin = pluginByExtension.get(extension);
    if (plugin == null && !(extension instanceof Class)) {
      plugin = pluginByExtension.get(extension.getClass());
    }
    return plugin;
  }

  public String getPluginKeyForExtension(Object extension) {
    Plugin plugin = getPluginForExtension(extension);
    if (plugin != null) {
      return plugin.getKey();
    }
    return null;
  }

  private boolean isExtension(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    return extensionClass.isAssignableFrom(clazz);
  }

  public void registerExtension(MutablePicoContainer container, Plugin plugin, Object extension) {
    container.as(Characteristics.CACHE).addComponent(getExtensionKey(extension), extension);
    pluginByExtension.put(extension, plugin);
  }

  protected Object getExtensionKey(Object component) {
    if (component instanceof Class) {
      return component;
    }
    return component.getClass().getCanonicalName() + "-" + component.toString();
  }

}
