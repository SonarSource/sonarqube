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
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.Plugin;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.core.plugin.AbstractPluginRepository;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BatchPluginRepository extends AbstractPluginRepository {

  private static final Logger LOG = LoggerFactory.getLogger(BatchPluginRepository.class);

  private Map<String, ClassLoader> classloaders;
  private String baseUrl;
  private JpaPluginDao dao;

  public BatchPluginRepository(JpaPluginDao dao, ServerMetadata server) {
    this.dao = dao;
    this.baseUrl = server.getUrl() + "/deploy/plugins/";
  }

  /**
   * for unit tests only
   */
  BatchPluginRepository() {

  }

  public void start() {
    HashMultimap<String, URL> urlsByKey = HashMultimap.create();
    for (JpaPluginFile pluginFile : dao.getPluginFiles()) {
      try {
        String key = getClassloaderKey(pluginFile.getPluginKey());
        URL url = new URL(baseUrl + pluginFile.getPath());
        urlsByKey.put(key, url);

      } catch (MalformedURLException e) {
        throw new SonarException("Can not build the classloader of the plugin " + pluginFile.getPluginKey(), e);
      }
    }

    classloaders = new HashMap<String, ClassLoader>();
    for (String key : urlsByKey.keySet()) {
      Set<URL> urls = urlsByKey.get(key);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Classloader of plugin " + key + ":");
        for (URL url : urls) {
          LOG.debug("   -> " + url);
        }
      }
      classloaders.put(key, new RemoteClassLoader(urls, Thread.currentThread().getContextClassLoader()).getClassLoader());
    }
  }

  private String getClassloaderKey(String pluginKey) {
    return "sonar-plugin-" + pluginKey;
  }

  public void registerPlugins(MutablePicoContainer pico) {
    for (JpaPlugin pluginMetadata : dao.getPlugins()) {
      try {
        String classloaderKey = getClassloaderKey(pluginMetadata.getKey());
        Class claz = classloaders.get(classloaderKey).loadClass(pluginMetadata.getPluginClass());
        Plugin plugin = (Plugin) claz.newInstance();
        registerPlugin(pico, plugin, pluginMetadata.getKey());

      } catch (Exception e) {
        throw new SonarException("Fail to load extensions from plugin " + pluginMetadata.getKey(), e);
      }
    }
    invokeExtensionProviders(pico);
  }

  private String getOldCoveragePluginKey(String pluginKey) {
    if (StringUtils.equals("sonar-jacoco-plugin", pluginKey)) {
      return "jacoco";
    }
    if (StringUtils.equals("sonar-emma-plugin", pluginKey)) {
      return "emma";
    }
    return null;
  }

  @Override
  protected boolean shouldRegisterExtension(PicoContainer container, String pluginKey, Object extension) {
    boolean ok = isType(extension, BatchExtension.class);
    if (ok && isType(extension, AbstractCoverageExtension.class)) {
      ok = shouldRegisterCoverageExtension(pluginKey, container.getComponent(Project.class), container.getComponent(Configuration.class));
      if (!ok) {
        LOG.debug("The following extension is ignored: " + extension + ". See the parameter " + AbstractCoverageExtension.PARAM_PLUGIN);
      }
    }
    return ok;
  }

  boolean shouldRegisterCoverageExtension(String pluginKey, Project project, Configuration conf) {
    boolean ok=true;
    if (StringUtils.equals(project.getLanguageKey(), Java.KEY)) {
      String[] selectedPluginKeys = conf.getStringArray(AbstractCoverageExtension.PARAM_PLUGIN);
      if (ArrayUtils.isEmpty(selectedPluginKeys)) {
        selectedPluginKeys = new String[]{AbstractCoverageExtension.DEFAULT_PLUGIN};
      }
      String oldCoveragePluginKey = getOldCoveragePluginKey(pluginKey);
      ok = ArrayUtils.contains(selectedPluginKeys, pluginKey) || ArrayUtils.contains(selectedPluginKeys, oldCoveragePluginKey);
    }
    return ok;
  }
}
