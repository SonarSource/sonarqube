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
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class PluginDownloader implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PluginDownloader.class);
  private UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private HttpDownloader downloader;
  private File downloadDir;

  public PluginDownloader(UpdateCenterMatrixFactory updateCenterMatrixFactory, HttpDownloader downloader, DefaultServerFileSystem fileSystem) {
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.downloader = downloader;
    this.downloadDir = fileSystem.getDownloadedPluginsDir();
    try {
      FileUtils.forceMkdir(downloadDir);

    } catch (IOException e) {
      throw new SonarException("Fail to create the plugin downloads directory: " + downloadDir, e);
    }
  }

  /**
   * for unit tests
   */
  PluginDownloader(UpdateCenterMatrixFactory updateCenterMatrixFactory, HttpDownloader downloader, File downloadDir) {
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.downloader = downloader;
    this.downloadDir = downloadDir;
  }

  public void cancelDownloads() {
    try {
      if (downloadDir.exists()) {
        FileUtils.cleanDirectory(downloadDir);
      }

    } catch (IOException e) {
      throw new SonarException("Fail to clean the plugin downloads directory: " + downloadDir, e);
    }
  }

  public boolean hasDownloads() {
    return getDownloads().size() > 0;
  }

  public List<String> getDownloads() {
    List<String> names = new ArrayList<String>();
    List<File> files = (List<File>) FileUtils.listFiles(downloadDir, new String[]{"jar"}, false);
    for (File file : files) {
      names.add(file.getName());
    }
    return names;
  }

  public void download(String pluginKey, Version version) {
    for (Release release : updateCenterMatrixFactory.getUpdateCenter(false).findInstallablePlugins(pluginKey, version)) {
      try {
        downloadRelease(release);

      } catch (Exception e) {
        String message = "Fail to download the plugin (" + release.getArtifact().getKey() + ", version " + release.getVersion().getName() + ") from " + release.getDownloadUrl();
        LOG.warn(message, e);
        throw new SonarException(message, e);
      }
    }
  }

  private void downloadRelease(Release release) throws URISyntaxException {
    URI uri = new URI(release.getDownloadUrl());
    String filename = StringUtils.substringAfterLast(uri.getPath(), "/");
    downloader.download(uri, new File(downloadDir, filename));
  }
}
