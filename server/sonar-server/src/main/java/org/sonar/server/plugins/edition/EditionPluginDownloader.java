/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.plugins.edition;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.FileUtils;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang.StringUtils.substringAfterLast;

public class EditionPluginDownloader {
  private static final Logger LOG = Loggers.get(EditionPluginDownloader.class);
  private static final String PLUGIN_EXTENSION = "jar";

  private final Path tmpDir;
  private final Path downloadDir;
  private final HttpDownloader downloader;

  public EditionPluginDownloader(HttpDownloader downloader, ServerFileSystem fileSystem) {
    this.downloadDir = fileSystem.getEditionDownloadedPluginsDir().toPath();
    this.downloader = downloader;
    this.tmpDir = downloadDir.resolveSibling(downloadDir.getFileName() + "_tmp");
  }

  public void downloadEditionPlugins(Set<String> pluginKeys, UpdateCenter updateCenter) {
    try {
      Set<Release> pluginsToInstall = new HashSet<>();
      for (String pluginKey : pluginKeys) {
        pluginsToInstall.addAll(updateCenter.findInstallablePlugins(pluginKey, Version.create("")));
      }

      FileUtils.deleteDirectory(tmpDir);
      Files.createDirectories(tmpDir);

      for (Release r : pluginsToInstall) {
        download(r);
      }

      FileUtils.deleteDirectory(downloadDir);
      Files.move(tmpDir, downloadDir);
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    } finally {
      FileUtils.deleteQuietly(tmpDir);
    }
  }

  protected void download(Release release) {
    try {
      LOG.info("Downloading plugin: {}", release.getArtifact().getKey());
      downloadRelease(release);
    } catch (Exception e) {
      String message = String.format("Fail to download the plugin (%s, version %s) from %s (error is : %s)",
        release.getArtifact().getKey(), release.getVersion().getName(), release.getDownloadUrl(), e.getMessage());
      LOG.debug(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  private void downloadRelease(Release release) throws URISyntaxException, IOException {
    String url = release.getDownloadUrl();

    URI uri = new URI(url);
    if (url.startsWith("file:")) {
      // used for tests
      File file = toFile(uri.toURL());
      Files.copy(file.toPath(), tmpDir.resolve(file.getName()));
    } else {
      String filename = substringAfterLast(uri.getPath(), "/");
      if (!filename.endsWith("." + PLUGIN_EXTENSION)) {
        filename = release.getKey() + "-" + release.getVersion() + "." + PLUGIN_EXTENSION;
      }
      Path targetFile = tmpDir.resolve(filename);
      downloader.download(uri, targetFile.toFile());
    }
  }
}
