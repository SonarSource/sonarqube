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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.Logs;
import org.sonar.core.classloaders.ClassLoadersCollection;

public class PluginClassLoaders implements ServerComponent {

  private ClassLoadersCollection classLoaders = new ClassLoadersCollection(getClass().getClassLoader());

  private List<PluginMetadata> metadata = Lists.newArrayList();

  public void addForCreation(PluginMetadata plugin) {
    metadata.add(plugin);
  }

  ClassLoader create(String pluginKey, Collection<File> classloaderFiles, boolean useChildFirstClassLoader) {
    try {
      List<URL> urls = Lists.newArrayList();
      for (File file : classloaderFiles) {
        urls.add(toUrl(file));
      }
      return classLoaders.createClassLoader(pluginKey, urls, useChildFirstClassLoader);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Fail to load the classloader of the plugin: " + pluginKey, e);
    }
  }

  private void extend(String basePluginKey, String pluginKey, Collection<File> classloaderFiles) {
    try {
      List<URL> urls = Lists.newArrayList();
      for (File file : classloaderFiles) {
        urls.add(toUrl(file));
      }
      classLoaders.extend(basePluginKey, pluginKey, urls);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Fail to load the classloader of the plugin: " + pluginKey, e);
    }
  }

  URL toUrl(File file) throws MalformedURLException {
    // From Classworlds javadoc :
    // A constituent is a URL that points to either a JAR format file containing
    // classes and/or resources, or a directory that should be used for searching.
    // If the constituent is a directory, then the URL must end with a slash (/).
    // Otherwise the constituent will be treated as a JAR file.
    URL url = file.toURI().toURL();
    if (file.isDirectory()) {
      if (!url.toString().endsWith("/")) {
        url = new URL(url.toString() + "/");
      }
    } else if (!StringUtils.endsWithIgnoreCase(file.getName(), "jar")) {
      url = file.getParentFile().toURI().toURL();
    }
    return url;
  }

  public ClassLoader getClassLoader(String pluginKey) {
    return classLoaders.get(pluginKey);
  }

  public Class getClass(String pluginKey, String classname) {
    Class clazz = null;
    ClassLoader classloader = getClassLoader(pluginKey);
    if (classloader != null) {
      try {
        clazz = classloader.loadClass(classname);

      } catch (ClassNotFoundException e) {
        LoggerFactory.getLogger(getClass()).warn("Class not found in plugin " + pluginKey + ": " + classname, e);
      }
    }
    return clazz;
  }

  public List<PluginMetadata> completeCreation() {
    List<PluginMetadata> created = Lists.newArrayList();
    for (PluginMetadata pluginMetadata : metadata) {
      if (StringUtils.isEmpty(pluginMetadata.getBasePlugin())) {
        create(pluginMetadata.getKey(), pluginMetadata.getDeployedFiles(), pluginMetadata.isUseChildFirstClassLoader());
        created.add(pluginMetadata);
      }
    }
    // Extend plugins by other plugins
    for (PluginMetadata pluginMetadata : metadata) {
      String pluginKey = pluginMetadata.getKey();
      String basePluginKey = pluginMetadata.getBasePlugin();
      if (StringUtils.isNotEmpty(pluginMetadata.getBasePlugin())) {
        if (classLoaders.get(basePluginKey) != null) {
          Logs.INFO.debug("Plugin {} extends {}", pluginKey, basePluginKey);
          extend(basePluginKey, pluginKey, pluginMetadata.getDeployedFiles());
          created.add(pluginMetadata);
        } else {
          // Ignored, because base plugin doesn't exists
          Logs.INFO.warn("Plugin {} extends nonexistent plugin {}", pluginKey, basePluginKey);
        }
      }
    }
    classLoaders.done();
    return created;
  }
}
