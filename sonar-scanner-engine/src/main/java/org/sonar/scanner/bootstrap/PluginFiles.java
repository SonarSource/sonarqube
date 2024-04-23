/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.bootstrap.ScannerPluginInstaller.InstalledPlugin;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import static java.lang.String.format;

public class PluginFiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginFiles.class);
  private static final String MD5_HEADER = "Sonar-MD5";
  @VisibleForTesting
  static final String PLUGINS_DOWNLOAD_TIMEOUT_PROPERTY = "sonar.plugins.download.timeout";
  private static final int PLUGINS_DOWNLOAD_TIMEOUT_DEFAULT = 300;

  private final DefaultScannerWsClient wsClient;
  private final Configuration configuration;
  private final Path cacheDir;
  private final Path tempDir;

  public PluginFiles(DefaultScannerWsClient wsClient, Configuration configuration, SonarUserHome sonarUserHome) {
    this.wsClient = wsClient;
    this.configuration = configuration;
    var home = sonarUserHome.getPath();
    this.cacheDir = mkdir(home.resolve("cache"), "user cache");
    this.tempDir = mkdir(home.resolve("_tmp"), "temp dir");
    LOGGER.info("User cache: {}", cacheDir);
  }

  public File createTempDir() {
    try {
      return Files.createTempDirectory(tempDir, "plugins").toFile();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp directory in " + tempDir, e);
    }
  }

  /**
   * Get the JAR file of specified plugin. If not present in user local cache,
   * then it's downloaded from server and added to cache.
   *
   * @return the file, or {@link Optional#empty()} if plugin not found (404 HTTP code)
   * @throws IllegalStateException if the plugin can't be downloaded (not 404 nor 2xx HTTP codes)
   * or can't be cached locally.
   */
  public Optional<File> get(InstalledPlugin plugin) {
    // Does not fail if another process tries to create the directory at the same time.
    Path jarInCache = jarInCache(plugin.key, plugin.hash);
    if (Files.isRegularFile(jarInCache)) {
      return Optional.of(jarInCache.toFile());
    }
    return download(plugin).map(Path::toFile);
  }

  private Optional<Path> download(InstalledPlugin plugin) {
    GetRequest request = new GetRequest("api/plugins/download")
      .setParam("plugin", plugin.key)
      .setTimeOutInMs(configuration.getInt(PLUGINS_DOWNLOAD_TIMEOUT_PROPERTY).orElse(PLUGINS_DOWNLOAD_TIMEOUT_DEFAULT) * 1000);

    Path downloadedFile = newTempFile();
    LOGGER.debug("Download plugin '{}' to '{}'", plugin.key, downloadedFile);

    try (WsResponse response = wsClient.call(request)) {
      Optional<String> expectedMd5 = response.header(MD5_HEADER);
      if (expectedMd5.isEmpty()) {
        throw new IllegalStateException(format(
          "Fail to download plugin [%s]. Request to %s did not return header %s", plugin.key, response.requestUrl(), MD5_HEADER));
      }

      downloadBinaryTo(plugin, downloadedFile, response);

      // verify integrity
      String effectiveTempMd5 = computeMd5(downloadedFile);
      if (!expectedMd5.get().equals(effectiveTempMd5)) {
        throw new IllegalStateException(format(
          "Fail to download plugin [%s]. File %s was expected to have checksum %s but had %s", plugin.key, downloadedFile, expectedMd5.get(), effectiveTempMd5));
      }

      // un-compress if needed
      String cacheMd5;
      Path tempJar;

      tempJar = downloadedFile;
      cacheMd5 = expectedMd5.get();

      // put in cache
      Path jarInCache = jarInCache(plugin.key, cacheMd5);
      mkdir(jarInCache.getParent());
      moveFile(tempJar, jarInCache);
      return Optional.of(jarInCache);

    } catch (HttpException e) {
      if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
        // Plugin was listed but not longer available. It has probably been
        // uninstalled.
        return Optional.empty();
      }

      // not 2xx nor 404
      throw new IllegalStateException(format("Fail to download plugin [%s]. Request to %s returned code %d.", plugin.key, e.url(), e.code()));
    }
  }

  private static void downloadBinaryTo(InstalledPlugin plugin, Path downloadedFile, WsResponse response) {
    try (InputStream stream = response.contentStream()) {
      FileUtils.copyInputStreamToFile(stream, downloadedFile.toFile());
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to download plugin [%s] into %s", plugin.key, downloadedFile), e);
    }
  }

  private Path jarInCache(String pluginKey, String hash) {
    Path hashDir = cacheDir.resolve(hash);
    Path file = hashDir.resolve(format("sonar-%s-plugin.jar", pluginKey));
    if (!file.getParent().equals(hashDir)) {
      // vulnerability - attempt to create a file outside the cache directory
      throw new IllegalStateException(format("Fail to download plugin [%s]. Key is not valid.", pluginKey));
    }
    return file;
  }

  private Path newTempFile() {
    try {
      return Files.createTempFile(tempDir, "fileCache", null);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file in " + tempDir, e);
    }
  }

  private static String computeMd5(Path file) {
    try (InputStream fis = new BufferedInputStream(Files.newInputStream(file))) {
      return DigestUtils.md5Hex(fis);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to compute md5 of " + file, e);
    }
  }

  private static void moveFile(Path sourceFile, Path targetFile) {
    try {
      Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e1) {
      // Check if the file was cached by another process during download
      if (!Files.exists(targetFile)) {
        LOGGER.warn("Unable to rename {} to {}", sourceFile, targetFile);
        LOGGER.warn("A copy/delete will be tempted but with no guarantee of atomicity");
        try {
          Files.move(sourceFile, targetFile);
        } catch (IOException e2) {
          throw new IllegalStateException("Fail to move " + sourceFile + " to " + targetFile, e2);
        }
      }
    }
  }

  private static void mkdir(Path dir) {
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create cache directory: " + dir, e);
    }
  }

  private static Path mkdir(Path dir, String debugTitle) {
    if (!Files.isDirectory(dir)) {
      LOGGER.debug("Create : {}", dir);
      try {
        Files.createDirectories(dir);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create folder " + debugTitle + " at " + dir, e);
      }
    }
    return dir;
  }

}
