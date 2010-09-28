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
package org.sonar.core.plugin;

import com.google.common.collect.Maps;
import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.*;
import org.sonar.api.platform.PluginRepository;

import java.util.Collection;
import java.util.Map;

/**
 * @since 2.2
 */
public abstract class AbstractPluginRepository implements PluginRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractPluginRepository.class);

  private Map<String, Plugin> pluginByKey = Maps.newHashMap();
  private Map<Object, Plugin> pluginByExtension = Maps.newIdentityHashMap();

  protected void registerPlugin(MutablePicoContainer container, Plugin plugin, String pluginKey) {
    LOG.debug("Register the plugin {}", pluginKey);
    pluginByKey.put(pluginKey, plugin);
    for (Object extension : plugin.getExtensions()) {
      registerExtension(container, plugin, pluginKey, extension);
    }
  }

  private void registerExtension(MutablePicoContainer container, Plugin plugin, String pluginKey, Object extension) {
    if (shouldRegisterExtension(pluginKey, extension)) {
      LOG.debug("Register the extension: {}", extension);
      container.as(Characteristics.CACHE).addComponent(getExtensionKey(extension), extension);
      pluginByExtension.put(extension, plugin);
    }
    if (isExtensionProvider(extension)) {
      LOG.debug("Register the extension provider: {}", extension);
      container.as(Characteristics.CACHE).addAdapter(new ExtensionProviderAdapter(extension));
    }
  }

  protected abstract boolean shouldRegisterExtension(String pluginKey, Object extension);

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

  protected static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    return extensionClass.isAssignableFrom(clazz);
  }

  protected static boolean isExtensionProvider(Object extension) {
    return extension instanceof ExtensionProvider;
  }

  protected static Object getExtensionKey(Object component) {
    if (component instanceof Class) {
      return component;
    }
    return component.getClass().getCanonicalName() + "-" + component.toString();
  }

  public static class ExtensionProviderAdapter extends ProviderAdapter {
    public ExtensionProviderAdapter(Object provider) {
      super(provider);
    }
  }
}
