/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.sca;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.internal.apachecommons.lang3.SystemUtils;
import org.sonar.api.utils.System2;
import org.sonar.scanner.bootstrap.SonarUserHome;
import org.sonar.scanner.http.ScannerWsClient;
import org.sonar.scanner.repository.TelemetryCache;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static java.lang.String.format;

/**
 * This class is responsible for checking the SQ server for the latest version of the CLI,
 * caching the CLI for use across different projects, updating the cached CLI to the latest
 * version, and holding on to the cached CLI's file location so that other service classes
 * can make use of it.
 */
public class CliCacheService {
  protected static final String CLI_WS_URL = "api/v2/sca/clis";
  private static final Logger LOG = LoggerFactory.getLogger(CliCacheService.class);
  private final SonarUserHome sonarUserHome;
  private final ScannerWsClient wsClient;
  private final TelemetryCache telemetryCache;
  private final System2 system2;

  public CliCacheService(SonarUserHome sonarUserHome, ScannerWsClient wsClient, TelemetryCache telemetryCache, System2 system2) {
    this.sonarUserHome = sonarUserHome;
    this.wsClient = wsClient;
    this.telemetryCache = telemetryCache;
    this.system2 = system2;
  }

  static Path newTempFile(Path tempDir) {
    try {
      return Files.createTempFile(tempDir, "scaFileCache", null);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file in " + tempDir, e);
    }
  }

  static void moveFile(Path sourceFile, Path targetFile) {
    try {
      Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e1) {
      // Check if the file was cached by another process during download
      if (!Files.exists(targetFile)) {
        LOG.warn("Unable to rename {} to {}", sourceFile, targetFile);
        LOG.warn("A copy/delete will be tempted but with no guarantee of atomicity");
        try {
          Files.move(sourceFile, targetFile);
        } catch (IOException e2) {
          throw new IllegalStateException("Fail to move " + sourceFile + " to " + targetFile, e2);
        }
      }
    }
  }

  static void mkdir(Path dir) {
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create cache directory: " + dir, e);
    }
  }

  static void downloadBinaryTo(Path downloadLocation, WsResponse response) {
    try (InputStream stream = response.contentStream()) {
      FileUtils.copyInputStreamToFile(stream, downloadLocation.toFile());
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to download SCA CLI into %s", downloadLocation), e);
    }
  }

  public File cacheCli() {
    boolean success = false;

    var alternateLocation = system2.envVariable("TIDELIFT_CLI_LOCATION");
    if (alternateLocation != null) {
      LOG.info("Using alternate location for Tidelift CLI: {}", alternateLocation);
      // If the TIDELIFT_CLI_LOCATION environment variable is set, we should use that location
      // instead of trying to download the CLI from the server.
      File cliFile = new File(alternateLocation);
      if (!cliFile.exists()) {
        throw new IllegalStateException(format("Alternate location for Tidelift CLI has been set but no file was found at %s", alternateLocation));
      }
      return cliFile;
    }

    try {
      List<CliMetadataResponse> metadataResponses = getLatestMetadata(apiOsName(), apiArch());

      if (metadataResponses.isEmpty()) {
        throw new IllegalStateException(format("Could not find CLI for %s %s", apiOsName(), apiArch()));
      }

      // We should only be getting one matching CLI for the OS + Arch combination.
      // If we have more than one CLI to choose from then I'm not sure which one to choose.
      if (metadataResponses.size() > 1) {
        throw new IllegalStateException("Multiple CLI matches found. Unable to correctly cache CLI.");
      }

      CliMetadataResponse metadataResponse = metadataResponses.get(0);
      String checksum = metadataResponse.sha256();
      // If we have a matching checksum dir with the existing CLI file, then we are up to date.
      if (!cachedCliFile(checksum).exists()) {
        LOG.debug("SCA CLI update detected");
        downloadCli(metadataResponse.id(), checksum);
        telemetryCache.put("scanner.sca.get.cli.cache.hit", "false");
      } else {
        telemetryCache.put("scanner.sca.get.cli.cache.hit", "true");
      }

      File cliFile = cachedCliFile(checksum);
      success = true;
      return cliFile;
    } finally {
      telemetryCache.put("scanner.sca.get.cli.success", String.valueOf(success));
    }
  }

  Path cacheDir() {
    return sonarUserHome.getPath().resolve("cache");
  }

  private File cachedCliFile(String checksum) {
    return cacheDir().resolve(checksum).resolve(fileName()).toFile();
  }

  private String fileName() {
    return system2.isOsWindows() ? "tidelift.exe" : "tidelift";
  }

  private List<CliMetadataResponse> getLatestMetadata(String osName, String arch) {
    LOG.info("Requesting CLI for OS {} and arch {}", osName, arch);
    GetRequest getRequest = new GetRequest(CLI_WS_URL).setParam("os", osName).setParam("arch", arch);
    try (WsResponse response = wsClient.call(getRequest)) {
      try (Reader reader = response.contentReader()) {
        Type listOfMetadata = new TypeToken<ArrayList<CliMetadataResponse>>() {
        }.getType();
        return new Gson().fromJson(reader, listOfMetadata);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void downloadCli(String id, String checksum) {
    LOG.info("Downloading cli {}", id);
    long startTime = system2.now();
    boolean success = false;
    GetRequest getRequest = new GetRequest(CLI_WS_URL + "/" + id).setHeader("Accept", "application/octet-stream");

    try (WsResponse response = wsClient.call(getRequest)) {
      // Download to a temporary file location in case another process is also trying to
      // create the CLI file in the checksum cache directory. Once the file is downloaded to a temporary
      // location, do an atomic move to the correct cache location.
      Path tempDir = createTempDir();
      Path tempFile = newTempFile(tempDir);
      downloadBinaryTo(tempFile, response);
      File destinationFile = cachedCliFile(checksum);
      // We need to make sure the folder structure exists for the correct cache location before performing the move.
      mkdir(destinationFile.toPath().getParent());
      moveFile(tempFile, destinationFile.toPath());
      if (!destinationFile.setExecutable(true, false)) {
        throw new IllegalStateException("Unable to mark CLI as executable");
      }
      success = true;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to download CLI executable", e);
    } finally {
      telemetryCache.put("scanner.sca.download.cli.duration", String.valueOf(system2.now() - startTime));
      telemetryCache.put("scanner.sca.download.cli.success", String.valueOf(success));
    }
  }

  String apiOsName() {
    // We don't want to send the raw OS name because there could be too many combinations of the OS name
    // to reliably match up with the correct CLI needed to be downloaded. Instead, we send a subset of
    // OS names that should match to the correct CLI here.
    if (system2.isOsWindows()) {
      return "windows";
    } else if (system2.isOsMac()) {
      return "mac";
    } else {
      return "linux";
    }
  }

  String apiArch() {
    return SystemUtils.OS_ARCH.toLowerCase(Locale.ENGLISH);
  }

  Path createTempDir() {
    Path dir = sonarUserHome.getPath().resolve("_tmp");
    try {
      if (Files.exists(dir)) {
        return dir;
      } else {
        return Files.createDirectory(dir);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create temp directory at " + dir, e);
    }
  }

  private record CliMetadataResponse(
    String id,
    String filename,
    String sha256,
    String os,
    String arch) {
  }
}
