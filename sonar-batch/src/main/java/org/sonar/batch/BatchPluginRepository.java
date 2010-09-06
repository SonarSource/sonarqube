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
package org.sonar.batch;

import com.google.common.collect.HashMultimap;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginRepository;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BatchPluginRepository extends PluginRepository {

  private Map<String, ClassLoader> classloaders;
  private String baseUrl;
  private JpaPluginDao dao;

  public BatchPluginRepository(JpaPluginDao dao, ServerMetadata server) {
    this.dao= dao;
    this.baseUrl = server.getUrl() + "/deploy/plugins/";
  }

  public void start() {
    HashMultimap<String, URL> urlsByKey = HashMultimap.create();
    for (JpaPluginFile pluginFile : dao.getPluginFiles()) {
      try {
        String key = getClassloaderKey(pluginFile.getPluginKey());
        URL url = new URL(baseUrl + pluginFile.getPath());
        urlsByKey.put(key, url);

      } catch (MalformedURLException e) {
        throw new RuntimeException("Can not build the classloader of the plugin " + pluginFile.getPluginKey(), e);
      }
    }

    classloaders = new HashMap<String, ClassLoader>();
    for (String key : urlsByKey.keySet()) {
      Set<URL> urls = urlsByKey.get(key);

      Logger logger = LoggerFactory.getLogger(getClass());
      if (logger.isDebugEnabled()) {
        logger.debug("Classloader of plugin " + key + ":");
        for (URL url : urls) {
          logger.debug("   -> " + url);
        }
      }
      classloaders.put(key, new RemoteClassLoader(urls, Thread.currentThread().getContextClassLoader()).getClassLoader());
    }
  }

  private String getClassloaderKey(String pluginKey) {
    return "sonar-plugin-" + pluginKey;
  }

  public void registerPlugins(MutablePicoContainer pico) {
    try {
      for (JpaPlugin pluginMetadata : dao.getPlugins()) {
        String classloaderKey = getClassloaderKey(pluginMetadata.getKey());
        Class claz = classloaders.get(classloaderKey).loadClass(pluginMetadata.getPluginClass());
        Plugin plugin = (Plugin) claz.newInstance();
        registerPlugin(pico, plugin, BatchExtension.class);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
