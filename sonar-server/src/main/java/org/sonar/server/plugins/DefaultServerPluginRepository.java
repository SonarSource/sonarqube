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

import com.google.common.collect.Sets;
import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.LoggerFactory;
import org.sonar.api.*;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.ServerPluginRepository;
import org.sonar.core.plugins.PluginClassloaders;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 2.2
 */
public class DefaultServerPluginRepository implements ServerPluginRepository {

  private PluginClassloaders classloaders;
  private PluginDeployer deployer;
  private Map<String, Plugin> pluginsByKey;
  private Set<String> disabledPlugins = Sets.newHashSet();

  public DefaultServerPluginRepository(PluginDeployer deployer) {
    this.classloaders = new PluginClassloaders(getClass().getClassLoader());
    this.deployer = deployer;
  }

  public void start() {
    pluginsByKey = classloaders.init(deployer.getMetadata());
  }

  public void stop() {
    if (classloaders != null) {
      classloaders.clean();
      classloaders = null;
    }
  }

  public void disable(String pluginKey) {
    disabledPlugins.add(pluginKey);
    for (PluginMetadata metadata : getMetadata()) {
      if (pluginKey.equals(metadata.getBasePlugin())) {
        disable(metadata.getKey());
      }
    }
  }

  public boolean isDisabled(String pluginKey) {
    return disabledPlugins.contains(pluginKey);
  }

  public Collection<Plugin> getPlugins() {
    return pluginsByKey.values();
  }

  public Plugin getPlugin(String key) {
    return pluginsByKey.get(key);
  }

  public ClassLoader getClassloader(String pluginKey) {
    return classloaders.get(pluginKey);
  }

  public Class getClass(String pluginKey, String classname) {
    Class clazz = null;
    ClassLoader classloader = getClassloader(pluginKey);
    if (classloader != null) {
      try {
        clazz = classloader.loadClass(classname);

      } catch (ClassNotFoundException e) {
        LoggerFactory.getLogger(getClass()).warn("Class not found in plugin " + pluginKey + ": " + classname, e);
      }
    }
    return clazz;
  }


  public Property[] getProperties(Plugin plugin) {
    if (plugin != null) {
      Class<? extends Plugin> classInstance = plugin.getClass();
      if (classInstance.isAnnotationPresent(Properties.class)) {
        return classInstance.getAnnotation(Properties.class).value();
      }
    }
    return new Property[0];
  }

  public Collection<PluginMetadata> getMetadata() {
    return deployer.getMetadata();
  }

  public PluginMetadata getMetadata(String pluginKey) {
    return deployer.getMetadata(pluginKey);
  }

  public void registerExtensions(MutablePicoContainer container) {
    registerExtensions(container, getPlugins());
  }

  void registerExtensions(MutablePicoContainer container, Collection<Plugin> plugins) {
    for (Plugin plugin : plugins) {
      container.as(Characteristics.CACHE).addComponent(plugin);
      for (Object extension : plugin.getExtensions()) {
        installExtension(container, extension, true);
      }
    }
    installExtensionProviders(container);
  }

  void installExtensionProviders(MutablePicoContainer container) {
    List<ExtensionProvider> providers = container.getComponents(ExtensionProvider.class);
    for (ExtensionProvider provider : providers) {
      Object obj = provider.provide();
      if (obj != null) {
        if (obj instanceof Iterable) {
          for (Object extension : (Iterable) obj) {
            installExtension(container, extension, false);
          }
        } else {
          installExtension(container, obj, false);
        }
      }
    }
  }

  void installExtension(MutablePicoContainer container, Object extension, boolean acceptProvider) {
    if (isType(extension, ServerExtension.class)) {
      if (!acceptProvider && (isType(extension, ExtensionProvider.class) || extension instanceof ExtensionProvider)) {
        LoggerFactory.getLogger(getClass()).error("ExtensionProvider can not include providers itself: " + extension);
      } else {
        container.as(Characteristics.CACHE).addComponent(getExtensionKey(extension), extension);
      }
    }
  }

  static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    return extensionClass.isAssignableFrom(clazz);
  }

  static Object getExtensionKey(Object component) {
    if (component instanceof Class) {
      return component;
    }
    return component.getClass().getCanonicalName() + "-" + component.toString();
  }
}
