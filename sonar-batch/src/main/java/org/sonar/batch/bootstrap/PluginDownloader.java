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

import com.google.common.collect.Lists;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.cache.SonarCache;
import org.sonar.core.plugins.RemotePlugin;
import org.sonar.core.plugins.RemotePluginFile;

import java.io.File;
import java.util.List;

public class PluginDownloader implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PluginDownloader.class);

  private ServerClient server;
  private BatchSonarCache batchCache;

  public PluginDownloader(BatchSonarCache batchCache, ServerClient server) {
    this.server = server;
    this.batchCache = batchCache;
  }

  private SonarCache getSonarCache() {
    return batchCache.getCache();
  }

  public List<File> downloadPlugin(RemotePlugin remote) {
    try {
      List<File> files = Lists.newArrayList();
      for (RemotePluginFile file : remote.getFiles()) {
        File fileInCache = getSonarCache().getFileFromCache(file.getFilename(), file.getMd5());
        if (fileInCache == null) {
          File tmpDownloadFile = getSonarCache().getTemporaryFile();
          String url = "/deploy/plugins/" + remote.getKey() + "/" + file.getFilename();
          if (LOG.isDebugEnabled()) {
            LOG.debug("Downloading {} to {}", url, tmpDownloadFile.getAbsolutePath());
          }
          else {
            LOG.info("Downloading {}", file.getFilename());
          }
          server.download(url, tmpDownloadFile);
          String md5 = getSonarCache().cacheFile(tmpDownloadFile, file.getFilename());
          fileInCache = getSonarCache().getFileFromCache(file.getFilename(), md5);
          if (!md5.equals(file.getMd5())) {
            throw new SonarException("INVALID CHECKSUM: File " + fileInCache.getAbsolutePath() + " was expected to have checksum " + file.getMd5()
              + " but was downloaded with checksum " + md5);
          }
        }
        files.add(fileInCache);
      }
      return files;

    } catch (SonarException e) {
      throw e;
    } catch (Exception e) {
      throw new SonarException("Fail to download plugin: " + remote.getKey(), e);
    }
  }

  public List<RemotePlugin> downloadPluginIndex() {
    String url = "/deploy/plugins/index.txt";
    try {
      LOG.debug("Downloading index of plugins");
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
