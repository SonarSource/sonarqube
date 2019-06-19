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
package org.sonar.scanner.bootstrap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.ScannerPluginInstaller.InstalledPlugin;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import static java.lang.String.format;

public class PluginFiles {

  private static final Logger LOGGER = Loggers.get(PluginFiles.class);
  private static final String MD5_HEADER = "Sonar-MD5";
  private static final String COMPRESSION_HEADER = "Sonar-Compression";
  private static final String PACK200 = "pack200";
  private static final String UNCOMPRESSED_MD5_HEADER = "Sonar-UncompressedMD5";

  private final DefaultScannerWsClient wsClient;
  private final File cacheDir;
  private final File tempDir;

  public PluginFiles(DefaultScannerWsClient wsClient, Configuration configuration) {
    this.wsClient = wsClient;
    File home = locateHomeDir(configuration);
    this.cacheDir = mkdir(new File(home, "cache"), "user cache");
    this.tempDir = mkdir(new File(home, "_tmp"), "temp dir");
    LOGGER.info("User cache: {}", cacheDir.getAbsolutePath());
  }

  public File createTempDir() {
    try {
      return Files.createTempDirectory(tempDir.toPath(), "plugins").toFile();
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
    File jarInCache = jarInCache(plugin.key, plugin.hash);
    if (jarInCache.exists() && jarInCache.isFile()) {
      return Optional.of(jarInCache);
    }
    return download(plugin);
  }

  private Optional<File> download(InstalledPlugin plugin) {
    GetRequest request = new GetRequest("api/plugins/download")
      .setParam("plugin", plugin.key)
      .setParam("acceptCompressions", PACK200)
      .setTimeOutInMs(5 * 60_000);

    File downloadedFile = newTempFile();
    LOGGER.debug("Download plugin '{}' to '{}'", plugin.key, downloadedFile);

    try (WsResponse response = wsClient.call(request)) {
      Optional<String> expectedMd5 = response.header(MD5_HEADER);
      if (!expectedMd5.isPresent()) {
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
      File tempJar;
      Optional<String> compression = response.header(COMPRESSION_HEADER);
      if (compression.isPresent() && PACK200.equals(compression.get())) {
        tempJar = unpack200(plugin.key, downloadedFile);
        cacheMd5 = response.header(UNCOMPRESSED_MD5_HEADER).orElseThrow(() -> new IllegalStateException(format(
          "Fail to download plugin [%s]. Request to %s did not return header %s.", plugin.key, response.requestUrl(), UNCOMPRESSED_MD5_HEADER)));
      } else {
        tempJar = downloadedFile;
        cacheMd5 = expectedMd5.get();
      }

      // put in cache
      File jarInCache = jarInCache(plugin.key, cacheMd5);
      mkdir(jarInCache.getParentFile());
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

  private static void downloadBinaryTo(InstalledPlugin plugin, File downloadedFile, WsResponse response) {
    try (InputStream stream = response.contentStream()) {
      FileUtils.copyInputStreamToFile(stream, downloadedFile);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to download plugin [%s] into %s", plugin.key, downloadedFile), e);
    }
  }

  private File jarInCache(String pluginKey, String hash) {
    File hashDir = new File(cacheDir, hash);
    File file = new File(hashDir, format("sonar-%s-plugin.jar", pluginKey));
    if (!file.getParentFile().toPath().equals(hashDir.toPath())) {
      // vulnerability - attempt to create a file outside the cache directory
      throw new IllegalStateException(format("Fail to download plugin [%s]. Key is not valid.", pluginKey));
    }
    return file;
  }

  private File newTempFile() {
    try {
      return File.createTempFile("fileCache", null, tempDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file in " + tempDir, e);
    }
  }

  private File unpack200(String pluginKey, File compressedFile) {
    LOGGER.debug("Unpacking plugin {}", pluginKey);
    File jar = newTempFile();
    try (InputStream input = new GZIPInputStream(new BufferedInputStream(FileUtils.openInputStream(compressedFile)));
         JarOutputStream output = new JarOutputStream(new BufferedOutputStream(FileUtils.openOutputStream(jar)))) {
      Pack200.newUnpacker().unpack(input, output);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to download plugin [%s]. Pack200 error.", pluginKey), e);
    }
    return jar;
  }

  private static String computeMd5(File file) {
    try (InputStream fis = new BufferedInputStream(FileUtils.openInputStream(file))) {
      return DigestUtils.md5Hex(fis);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to compute md5 of " + file, e);
    }
  }

  private static void moveFile(File sourceFile, File targetFile) {
    boolean rename = sourceFile.renameTo(targetFile);
    // Check if the file was cached by another process during download
    if (!rename && !targetFile.exists()) {
      LOGGER.warn("Unable to rename {} to {}", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
      LOGGER.warn("A copy/delete will be tempted but with no guarantee of atomicity");
      try {
        Files.move(sourceFile.toPath(), targetFile.toPath());
      } catch (IOException e) {
        throw new IllegalStateException("Fail to move " + sourceFile.getAbsolutePath() + " to " + targetFile, e);
      }
    }
  }

  private static void mkdir(File dir) {
    try {
      Files.createDirectories(dir.toPath());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create cache directory: " + dir, e);
    }
  }

  private static File mkdir(File dir, String debugTitle) {
    if (!dir.isDirectory() || !dir.exists()) {
      LOGGER.debug("Create : {}", dir.getAbsolutePath());
      try {
        Files.createDirectories(dir.toPath());
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create " + debugTitle + dir.getAbsolutePath(), e);
      }
    }
    return dir;
  }

  private static File locateHomeDir(Configuration configuration) {
    return Stream.of(
      configuration.get("sonar.userHome").orElse(null),
      System.getenv("SONAR_USER_HOME"),
      System.getProperty("user.home") + File.separator + ".sonar")
      .filter(Objects::nonNull)
      .findFirst()
      .map(File::new)
      .get();
  }
}
