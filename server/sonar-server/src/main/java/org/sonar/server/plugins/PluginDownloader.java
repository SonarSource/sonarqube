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
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang.StringUtils.substringAfterLast;

/**
 * Downloads plugins from update center. Files are copied in the directory extensions/downloads and then
 * moved to extensions/plugins after server restart.
 */
public class PluginDownloader implements Startable {

  private static final Logger LOG = Loggers.get(PluginDownloader.class);
  private static final String TMP_SUFFIX = "tmp";
  private static final String PLUGIN_EXTENSION = "jar";

  private final UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private final HttpDownloader downloader;
  private final File downloadDir;

  public PluginDownloader(UpdateCenterMatrixFactory updateCenterMatrixFactory, HttpDownloader downloader,
    DefaultServerFileSystem fileSystem) {
    this.updateCenterMatrixFactory = updateCenterMatrixFactory;
    this.downloader = downloader;
    this.downloadDir = fileSystem.getDownloadedPluginsDir();
  }

  /**
   * Deletes the temporary files remaining from previous downloads
   */
  @Override
  public void start() {
    try {
      forceMkdir(downloadDir);
      for (File tempFile : listTempFile(this.downloadDir)) {
        deleteQuietly(tempFile);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory: " + downloadDir, e);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  public void cancelDownloads() {
    try {
      if (downloadDir.exists()) {
        cleanDirectory(downloadDir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean the plugin downloads directory: " + downloadDir, e);
    }
  }

  public boolean hasDownloads() {
    return !getDownloadedPluginFilenames().isEmpty();
  }

  public List<String> getDownloadedPluginFilenames() {
    List<String> names = new ArrayList<>();
    for (File file : listPlugins(this.downloadDir)) {
      names.add(file.getName());
    }
    return names;
  }

  /**
   * @return the list of download plugins as {@link PluginInfo} instances
   */
  public Collection<PluginInfo> getDownloadedPlugins() {
    return newArrayList(transform(listPlugins(this.downloadDir), PluginInfo.JarToPluginInfo.INSTANCE));
  }

  public void download(String pluginKey, Version version) {
    for (Release release : updateCenterMatrixFactory.getUpdateCenter(true).findInstallablePlugins(pluginKey, version)) {
      try {
        downloadRelease(release);

      } catch (Exception e) {
        String message = String.format("Fail to download the plugin (%s, version %s) from %s (error is : %s)",
          release.getArtifact().getKey(), release.getVersion().getName(), release.getDownloadUrl(), e.getMessage());
        LOG.debug(message, e);
        throw new SonarException(message, e);
      }
    }
  }

  private void downloadRelease(Release release) throws URISyntaxException, IOException {
    String url = release.getDownloadUrl();

    URI uri = new URI(url);
    if (url.startsWith("file:")) {
      // used for tests
      File file = toFile(uri.toURL());
      copyFileToDirectory(file, downloadDir);
    } else {
      String filename = substringAfterLast(uri.getPath(), "/");
      if (!filename.endsWith("." + PLUGIN_EXTENSION)) {
        filename = release.getKey() + "-" + release.getVersion() + "." + PLUGIN_EXTENSION;
      }
      File targetFile = new File(downloadDir, filename);
      File tempFile = new File(downloadDir, filename + "." + TMP_SUFFIX);
      downloader.download(uri, tempFile);
      copyFile(tempFile, targetFile);
      deleteQuietly(tempFile);
    }
  }

  private static Collection<File> listTempFile(File dir) {
    return FileUtils.listFiles(dir, new String[] {TMP_SUFFIX}, false);
  }

  private static Collection<File> listPlugins(File dir) {
    return FileUtils.listFiles(dir, new String[] {PLUGIN_EXTENSION}, false);
  }
}
