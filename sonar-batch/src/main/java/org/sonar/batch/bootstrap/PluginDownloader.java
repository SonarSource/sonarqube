/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.api.BatchComponent;
import org.sonar.api.utils.SonarException;
import org.sonar.core.plugins.RemotePlugin;
import org.sonar.core.plugins.RemotePluginFile;
import org.sonar.home.cache.FileCache;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PluginDownloader implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PluginDownloader.class);

  private ServerClient server;
  private FileCache fileCache;

  public PluginDownloader(FileCache fileCache, ServerClient server) {
    this.server = server;
    this.fileCache = fileCache;
  }

  public File downloadPlugin(final RemotePlugin remote) {
    try {
      final RemotePluginFile file = remote.file();
      File cachedFile = fileCache.get(file.getFilename(), file.getHash(), new FileCache.Downloader() {
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
      return cachedFile;

    } catch (Exception e) {
      throw new SonarException("Fail to download plugin: " + remote.getKey(), e);
    }
  }

  public List<RemotePlugin> downloadPluginIndex() {
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
      throw new SonarException("Fail to download plugins index: " + url, e);
    }
  }

}
