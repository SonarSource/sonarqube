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
package org.sonar.batch.bootstrap;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.utils.SonarException;
import org.sonar.core.classloaders.ClassLoadersCollection;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class BatchPluginRepository implements PluginRepository {

  private static final Logger LOG = LoggerFactory.getLogger(BatchPluginRepository.class);

  private JpaPluginDao dao;
  private ArtifactDownloader artifactDownloader;
  private Map<String, Plugin> pluginsByKey;
  private Set<String> whiteList = null;
  private Set<String> blackList = null;

  public BatchPluginRepository(JpaPluginDao dao, ArtifactDownloader artifactDownloader, Configuration configuration) {
    this.dao = dao;
    this.artifactDownloader = artifactDownloader;
    if (configuration.getString(CoreProperties.INCLUDE_PLUGINS)!=null) {
      whiteList = Sets.newTreeSet(Arrays.asList(configuration.getStringArray(CoreProperties.INCLUDE_PLUGINS)));
      LOG.info("Include plugins: " + Joiner.on(", ").join(whiteList));
    }
    if (configuration.getString(CoreProperties.EXCLUDE_PLUGINS)!=null) {
      blackList = Sets.newTreeSet(Arrays.asList(configuration.getStringArray(CoreProperties.EXCLUDE_PLUGINS)));
      LOG.info("Exclude plugins: " + Joiner.on(", ").join(blackList));
    }
//  TODO reactivate somewhere else:  LOG.info("Execution environment: {} {}", environment.getKey(), environment.getVersion());
  }

  public void start() {
    List<JpaPlugin> plugins = filter(dao.getPlugins());
    LOG.debug("Starting plugins: " + Joiner.on(", ").join(plugins));
    doStart(plugins);
  }

  List<JpaPlugin> filter(List<JpaPlugin> plugins) {
    return ImmutableList.copyOf(Iterables.filter(plugins, new Predicate<JpaPlugin>() {
      public boolean apply(JpaPlugin p) {
        return isAccepted(p.getKey()) && (StringUtils.isBlank(p.getBasePlugin()) || isAccepted(p.getBasePlugin()));
      }
    }));
  }

  public void doStart(List<JpaPlugin> basePlugins) {
    pluginsByKey = Maps.newHashMap();
    ClassLoadersCollection classLoaders = new ClassLoadersCollection(Thread.currentThread().getContextClassLoader());

    List<JpaPlugin> pluginsMetadata = Lists.newArrayList(basePlugins);
    createClassloaders(classLoaders, basePlugins);
    pluginsMetadata.addAll(extendClassloaders(classLoaders, pluginsMetadata));
    instantiatePluginEntryPoints(classLoaders, pluginsMetadata);

    classLoaders.done();
  }

  private void instantiatePluginEntryPoints(ClassLoadersCollection classLoaders, List<JpaPlugin> pluginsMetadata) {
    for (JpaPlugin pluginMetadata : pluginsMetadata) {
      try {
        Class claz = classLoaders.get(pluginMetadata.getKey()).loadClass(pluginMetadata.getPluginClass());
        Plugin plugin = (Plugin) claz.newInstance();
        pluginsByKey.put(pluginMetadata.getKey(), plugin);

      } catch (Exception e) {
        throw new SonarException("Fail to load plugin " + pluginMetadata.getKey(), e);
      }
    }
  }

  private List<JpaPlugin> extendClassloaders(ClassLoadersCollection classLoaders, List<JpaPlugin> pluginsMetadata) {
    List<JpaPlugin> extensions = Lists.newArrayList();
    // Extend plugins by other plugins
    for (JpaPlugin pluginMetadata : pluginsMetadata) {
      String pluginKey = pluginMetadata.getKey();
      String basePluginKey = pluginMetadata.getBasePlugin();
      if (StringUtils.isNotEmpty(basePluginKey)) {
        if (classLoaders.get(basePluginKey) != null) {
          LOG.debug("Plugin {} extends {}", pluginKey, basePluginKey);
          List<URL> urls = download(pluginMetadata);
          classLoaders.extend(basePluginKey, pluginKey, urls);
          extensions.add(pluginMetadata);

        } else {
          // Ignored, because base plugin doesn't exists
          LOG.warn("Plugin {} extends nonexistent plugin {}", pluginKey, basePluginKey);
        }
      }
    }
    return extensions;
  }

  private void createClassloaders(ClassLoadersCollection classLoaders, List<JpaPlugin> basePlugins) {
    for (JpaPlugin pluginMetadata : basePlugins) {
      if (StringUtils.isEmpty(pluginMetadata.getBasePlugin())) {
        String key = pluginMetadata.getKey();
        List<URL> urls = download(pluginMetadata);
        classLoaders.createClassLoader(key, urls, pluginMetadata.isUseChildFirstClassLoader() == Boolean.TRUE);
      }
    }
  }

  private List<URL> download(JpaPlugin pluginMetadata) {
    List<URL> urls = Lists.newArrayList();
    for (JpaPluginFile pluginFile : pluginMetadata.getFiles()) {
      File file = artifactDownloader.downloadExtension(pluginFile);
      try {
        urls.add(file.toURI().toURL());

      } catch (MalformedURLException e) {
        throw new SonarException("Can not get the URL of: " + file, e);
      }
    }
    return urls;
  }

  public Collection<Plugin> getPlugins() {
    return pluginsByKey.values();
  }

  public Plugin getPlugin(String key) {
    return pluginsByKey.get(key);
  }

  public Map<String, Plugin> getPluginsByKey() {
    return Collections.unmodifiableMap(pluginsByKey);
  }

  // TODO remove this method. Not used in batch.
  public Property[] getProperties(Plugin plugin) {
    if (plugin != null) {
      Class<? extends Plugin> classInstance = plugin.getClass();
      if (classInstance.isAnnotationPresent(Properties.class)) {
        return classInstance.getAnnotation(Properties.class).value();
      }
    }
    return new Property[0];
  }

  boolean isAccepted(String pluginKey) {
    if (whiteList!=null) {
      return whiteList.contains(pluginKey);
    }
    if (blackList!=null) {
      return !blackList.contains(pluginKey);
    }
    return true;
  }
}
