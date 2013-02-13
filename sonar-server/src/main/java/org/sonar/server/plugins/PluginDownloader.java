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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

public class PluginDownloader implements ServerComponent {

  private UpdateCenterClient center;
  private HttpDownloader downloader;
  private File downloadDir;

  public PluginDownloader(UpdateCenterClient center, HttpDownloader downloader, DefaultServerFileSystem fileSystem) {
    this.center = center;
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
  PluginDownloader(UpdateCenterClient center, HttpDownloader downloader, File downloadDir) {
    this.center = center;
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
    List<File> files = (List<File>) FileUtils.listFiles(downloadDir, new String[] { "jar" }, false);
    for (File file : files) {
      names.add(file.getName());
    }
    return names;
  }

  public void download(String pluginKey, Version version) {
    Plugin plugin = center.getCenter().getPlugin(pluginKey);
    if (plugin == null) {
      String message = "This plugin does not exist: " + pluginKey;
      Logs.INFO.warn(message);
      throw new SonarException(message);
    }

    Release release = plugin.getRelease(version);
    if (release == null || StringUtils.isBlank(release.getDownloadUrl())) {
      String message = "This release can not be installed: " + pluginKey + ", version " + version;
      Logs.INFO.warn(message);
      throw new SonarException(message);
    }

    try {
      URI uri = new URI(release.getDownloadUrl());
      String filename = StringUtils.substringAfterLast(uri.getPath(), "/");
      downloader.download(uri, new File(downloadDir, filename));

    } catch (Exception e) {
      String message = "Fail to download the plugin (" + pluginKey + ", version " + version + ") from " + release.getDownloadUrl();
      Logs.INFO.warn(message, e);
      throw new SonarException(message, e);
    }
  }
}
