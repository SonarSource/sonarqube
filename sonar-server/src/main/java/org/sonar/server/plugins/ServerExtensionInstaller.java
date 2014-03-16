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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.sonar.api.Extension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.ServerExtension;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;

import java.util.Map;

/**
 * This class adds to picocontainer the server extensions provided by plugins
 */
public class ServerExtensionInstaller {
  private PluginRepository pluginRepository;

  public ServerExtensionInstaller(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  public void installExtensions(ComponentContainer container) {
    ListMultimap<PluginMetadata, Object> installedExtensionsByPlugin = ArrayListMultimap.create();

    for (PluginMetadata pluginMetadata : pluginRepository.getMetadata()) {
      Plugin plugin = pluginRepository.getPlugin(pluginMetadata.getKey());
      container.addExtension(pluginMetadata, plugin);

      for (Object extension : plugin.getExtensions()) {
        if (installExtension(container, pluginMetadata, extension, true) != null) {
          installedExtensionsByPlugin.put(pluginMetadata, extension);
        } else {
          container.declareExtension(pluginMetadata, extension);
        }
      }
    }
    for (Map.Entry<PluginMetadata, Object> entry : installedExtensionsByPlugin.entries()) {
      PluginMetadata plugin = entry.getKey();
      Object extension = entry.getValue();
      if (isExtensionProvider(extension)) {
        ExtensionProvider provider = (ExtensionProvider) container.getComponentByKey(extension);
        installProvider(container, plugin, provider);
      }
    }
  }

  private void installProvider(ComponentContainer container, PluginMetadata plugin, ExtensionProvider provider) {
    Object obj = provider.provide();
    if (obj != null) {
      if (obj instanceof Iterable) {
        for (Object ext : (Iterable) obj) {
          installExtension(container, plugin, ext, false);
        }
      } else {
        installExtension(container, plugin, obj, false);
      }
    }
  }

  Object installExtension(ComponentContainer container, PluginMetadata pluginMetadata, Object extension, boolean acceptProvider) {
    if (isType(extension, ServerExtension.class)) {
      if (!acceptProvider && isExtensionProvider(extension)) {
        throw new IllegalStateException("ExtensionProvider can not include providers itself: " + extension);
      }
      container.addExtension(pluginMetadata, extension);
      return extension;
    }
    return null;
  }

  static boolean isExtensionProvider(Object extension) {
    return isType(extension, ExtensionProvider.class) || extension instanceof ExtensionProvider;
  }

  static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = extension instanceof Class ? (Class) extension : extension.getClass();
    return extensionClass.isAssignableFrom(clazz);
  }
}
