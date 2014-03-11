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
package org.sonar.core.i18n;

import com.google.common.collect.Lists;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

class I18nClassloader extends URLClassLoader {

  private ClassLoader[] pluginClassloaders;

  public I18nClassloader(PluginRepository pluginRepository) {
    super(new URL[0]);
    List<ClassLoader> list = Lists.newArrayList();

    for (PluginMetadata metadata : pluginRepository.getMetadata()) {
      Plugin plugin = pluginRepository.getPlugin(metadata.getKey());
      list.add(plugin.getClass().getClassLoader());
    }

    this.pluginClassloaders = list.toArray(new ClassLoader[list.size()]);
  }

  I18nClassloader(ClassLoader[] pluginClassloaders) {
    super(new URL[0]);
    this.pluginClassloaders = pluginClassloaders;
  }

  public URL getResource(String name) {
    for (ClassLoader pluginClassloader : pluginClassloaders) {
      URL url = pluginClassloader.getResource(name);
      if (url != null) {
        return url;
      }
    }
    return null;
  }

  @Override
  protected synchronized Class loadClass(String s, boolean b) throws ClassNotFoundException {
    throw new UnsupportedOperationException("I18n classloader does support only resources, but not classes");
  }

  @Override
  public String toString() {
    return "i18n-classloader";
  }
}
