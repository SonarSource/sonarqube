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
package org.sonar.server.plugins;

import org.apache.commons.lang.StringUtils;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.sonar.api.ServerComponent;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PluginClassLoaders implements ServerComponent {

  private Map<String, ClassLoader> classLoaderByPluginKey = new HashMap<String, ClassLoader>();
  private ClassWorld world = new ClassWorld();

  public ClassLoader create(PluginMetadata plugin) {
    try {
      ClassRealm realm;
      realm = world.newRealm(plugin.getKey(), getClass().getClassLoader());
      for (File file : plugin.getDeployedFiles()) {
        realm.addConstituent(toUrl(file));
      }
      ClassLoader classloader = realm.getClassLoader();
      classLoaderByPluginKey.put(plugin.getKey(), classloader);
      return classloader;

    } catch (DuplicateRealmException e) {
      throw new RuntimeException("Fail to load the classloader of the plugin: " + plugin, e);

    } catch (MalformedURLException e) {
      throw new RuntimeException("Fail to load the classloader of the plugin: " + plugin, e);
    }
  }

  private URL toUrl(File file) throws MalformedURLException {
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
    return classLoaderByPluginKey.get(pluginKey);
  }
}
