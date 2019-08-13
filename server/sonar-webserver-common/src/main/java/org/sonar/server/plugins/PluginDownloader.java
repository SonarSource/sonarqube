/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.sonar.core.util.FileUtils.deleteQuietly;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

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
    ServerFileSystem fileSystem) {
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
        org.sonar.core.util.FileUtils.cleanDirectory(downloadDir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to clean the plugin downloads directory: " + downloadDir, e);
    }
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
    return listPlugins(this.downloadDir)
      .stream()
      .map(PluginInfo::create)
      .collect(MoreCollectors.toList());
  }

  public void download(String pluginKey, Version version) {
    Optional<UpdateCenter> updateCenter = updateCenterMatrixFactory.getUpdateCenter(true);
    if (updateCenter.isPresent()) {
      List<Release> installablePlugins = updateCenter.get().findInstallablePlugins(pluginKey, version);
      checkRequest(!installablePlugins.isEmpty(), "Error while downloading plugin '%s' with version '%s'. No compatible plugin found.", pluginKey, version.getName());
      for (Release release : installablePlugins) {
        try {
          downloadRelease(release);
        } catch (Exception e) {
          String message = String.format("Fail to download the plugin (%s, version %s) from %s (error is : %s)",
            release.getArtifact().getKey(), release.getVersion().getName(), release.getDownloadUrl(), e.getMessage());
          LOG.debug(message, e);
          throw new IllegalStateException(message, e);
        }
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
