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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarPlugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.core.plugins.RemotePlugin;
import org.sonar.core.plugins.RemotePluginFile;
import org.sonar.home.cache.FileCache;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link PluginsRepository} implementation that put downloaded plugins in a FS cache.
 */
public class DefaultPluginsRepository implements PluginsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultPluginsRepository.class);

  private ServerClient server;
  private FileCache fileCache;

  public DefaultPluginsRepository(FileCache fileCache, ServerClient server) {
    this.server = server;
    this.fileCache = fileCache;
  }

  @Override
  public File pluginFile(final RemotePlugin remote) {
    try {
      final RemotePluginFile file = remote.file();
      return fileCache.get(file.getFilename(), file.getHash(), new FileCache.Downloader() {
        @Override
        public void download(String filename, File toFile) throws IOException {
          String url = "/deploy/plugins/" + remote.getKey() + "/" + file.getFilename();
          if (LOG.isDebugEnabled()) {
            LOG.debug("Download {} to {}", url, toFile.getAbsolutePath());
          } else {
            LOG.info("Download {}", file.getFilename());
          }
          server.download(url, toFile);
        }
      });

    } catch (Exception e) {
      throw new IllegalStateException("Fail to download plugin: " + remote.getKey(), e);
    }
  }

  @Override
  public List<RemotePlugin> pluginList() {
    String url = "/deploy/plugins/index.txt";
    try {
      LOG.debug("Download index of plugins");
      String indexContent = server.request(url);
      String[] rows = StringUtils.split(indexContent, CharUtils.LF);
      List<RemotePlugin> remoteLocations = Lists.newArrayList();
      for (String row : rows) {
        remoteLocations.add(RemotePlugin.unmarshal(row));
      }
      return remoteLocations;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to download plugins index: " + url, e);
    }
  }

  @Override
  public Map<PluginMetadata, SonarPlugin> localPlugins() {
    return Collections.emptyMap();
  }

}
