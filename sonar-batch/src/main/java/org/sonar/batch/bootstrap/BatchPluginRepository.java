/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.core.plugins.PluginClassloaders;
import org.sonar.core.plugins.PluginInstaller;
import org.sonar.core.plugins.RemotePlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BatchPluginRepository implements PluginRepository {

  private static final Logger LOG = LoggerFactory.getLogger(BatchPluginRepository.class);
  private static final String CORE_PLUGIN = "core";
  private static final String ENGLISH_PACK_PLUGIN = "l10nen";

  private PluginDownloader pluginDownloader;
  private Map<String, Plugin> pluginsByKey;
  private Map<String, PluginMetadata> metadataByKey;
  private Set<String> whiteList = null;
  private Set<String> blackList = null;
  private PluginClassloaders classLoaders;

  public BatchPluginRepository(PluginDownloader pluginDownloader, Settings settings) {
    this.pluginDownloader = pluginDownloader;
    if (settings.hasKey(CoreProperties.BATCH_INCLUDE_PLUGINS)) {
      whiteList = Sets.newTreeSet(Arrays.asList(settings.getStringArray(CoreProperties.BATCH_INCLUDE_PLUGINS)));
      LOG.info("Include plugins: " + Joiner.on(", ").join(whiteList));
    }
    if (settings.hasKey(CoreProperties.BATCH_EXCLUDE_PLUGINS)) {
      blackList = Sets.newTreeSet(Arrays.asList(settings.getStringArray(CoreProperties.BATCH_EXCLUDE_PLUGINS)));
      LOG.info("Exclude plugins: " + Joiner.on(", ").join(blackList));
    }
  }

  public void start() {
    LOG.info("Install plugins");
    doStart(pluginDownloader.downloadPluginIndex());
  }

  void doStart(List<RemotePlugin> remotePlugins) {
    PluginInstaller extractor = new PluginInstaller();
    metadataByKey = Maps.newHashMap();
    for (RemotePlugin remote : remotePlugins) {
      if (isAccepted(remote.getKey())) {
        List<File> pluginFiles = pluginDownloader.downloadPlugin(remote);
        List<File> extensionFiles = pluginFiles.subList(1, pluginFiles.size());
        PluginMetadata metadata = extractor.installInSameLocation(pluginFiles.get(0), remote.isCore(), extensionFiles);
        if (StringUtils.isBlank(metadata.getBasePlugin()) || isAccepted(metadata.getBasePlugin())) {
          LOG.debug("Excluded plugin: " + metadata.getKey());
          metadataByKey.put(metadata.getKey(), metadata);
        }
      }
    }
    classLoaders = new PluginClassloaders(Thread.currentThread().getContextClassLoader());
    pluginsByKey = classLoaders.init(metadataByKey.values());
  }

  public void stop() {
    if (classLoaders != null) {
      classLoaders.clean();
      classLoaders = null;
    }
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

  public Collection<PluginMetadata> getMetadata() {
    return metadataByKey.values();
  }

  public PluginMetadata getMetadata(String pluginKey) {
    return metadataByKey.get(pluginKey);
  }

  boolean isAccepted(String pluginKey) {
    if (CORE_PLUGIN.equals(pluginKey) || ENGLISH_PACK_PLUGIN.equals(pluginKey)) {
      return true;
    }
    if (whiteList != null) {
      return whiteList.contains(pluginKey);
    }
    return blackList == null || !blackList.contains(pluginKey);
  }

  public Map<PluginMetadata, Plugin> getPluginsByMetadata() {
    Map<PluginMetadata, Plugin> result = Maps.newHashMap();
    for (Map.Entry<String, PluginMetadata> entry : metadataByKey.entrySet()) {
      String pluginKey = entry.getKey();
      PluginMetadata metadata = entry.getValue();
      result.put(metadata, pluginsByKey.get(pluginKey));
    }
    return result;
  }
}
