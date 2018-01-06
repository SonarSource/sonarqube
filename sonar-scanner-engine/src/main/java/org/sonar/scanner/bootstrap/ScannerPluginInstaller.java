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
package org.sonar.scanner.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.PluginInfo;
import org.sonar.home.cache.FileCache;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static java.lang.String.format;

/**
 * Downloads the plugins installed on server and stores them in a local user cache
 * (see {@link FileCacheProvider}).
 */
public class ScannerPluginInstaller implements PluginInstaller {

  private static final Logger LOG = Loggers.get(ScannerPluginInstaller.class);
  private static final String PLUGINS_WS_URL = "/api/plugins/installed";

  private final FileCache fileCache;
  private final ScannerPluginPredicate pluginPredicate;
  private final ScannerWsClient wsClient;

  public ScannerPluginInstaller(ScannerWsClient wsClient, FileCache fileCache, ScannerPluginPredicate pluginPredicate) {
    this.fileCache = fileCache;
    this.pluginPredicate = pluginPredicate;
    this.wsClient = wsClient;
  }

  @Override
  public Map<String, ScannerPlugin> installRemotes() {
    return loadPlugins(listInstalledPlugins());
  }

  private Map<String, ScannerPlugin> loadPlugins(InstalledPlugin[] remotePlugins) {
    Map<String, ScannerPlugin> infosByKey = new HashMap<>(remotePlugins.length);

    Profiler profiler = Profiler.create(LOG).startInfo("Load/download plugins");

    for (InstalledPlugin installedPlugin : remotePlugins) {
      if (pluginPredicate.apply(installedPlugin.key)) {
        File jarFile = download(installedPlugin);
        PluginInfo info = PluginInfo.create(jarFile);
        infosByKey.put(info.getKey(), new ScannerPlugin(installedPlugin.key, installedPlugin.updatedAt, info));
      }
    }
    profiler.stopInfo();
    return infosByKey;
  }

  /**
   * Returns empty on purpose. This method is used only by medium tests.
   * @see org.sonar.scanner.mediumtest.ScannerMediumTester
   */
  @Override
  public List<Object[]> installLocals() {
    return Collections.emptyList();
  }

  @VisibleForTesting
  File download(final InstalledPlugin remote) {
    try {
      if (remote.compressedFilename != null) {
        return fileCache.getCompressed(remote.compressedFilename, remote.compressedHash, new FileDownloader(remote.key));
      } else {
        return fileCache.get(remote.filename, remote.hash, new FileDownloader(remote.key));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to download plugin: " + remote.key, e);
    }
  }

  /**
   * Gets information about the plugins installed on server (filename, checksum)
   */
  @VisibleForTesting
  InstalledPlugin[] listInstalledPlugins() {
    Profiler profiler = Profiler.create(LOG).startInfo("Load plugins index");
    GetRequest getRequest = new GetRequest(PLUGINS_WS_URL);
    InstalledPlugins installedPlugins;
    try (Reader reader = wsClient.call(getRequest).contentReader()) {
      installedPlugins = new Gson().fromJson(reader, InstalledPlugins.class);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    profiler.stopInfo();
    return installedPlugins.plugins;
  }

  private static class InstalledPlugins {
    InstalledPlugin[] plugins;
  }

  static class InstalledPlugin {
    String key;
    String hash;
    String filename;
    long updatedAt;
    @Nullable
    String compressedHash;
    @Nullable
    String compressedFilename;
  }

  private class FileDownloader implements FileCache.Downloader {
    private String key;

    FileDownloader(String key) {
      this.key = key;
    }

    @Override
    public void download(String filename, File toFile) throws IOException {
      String url = format("/deploy/plugins/%s/%s", key, filename);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Download plugin '{}' to '{}'", filename, toFile);
      } else {
        LOG.debug("Download '{}'", filename);
      }

      WsResponse response = wsClient.call(new GetRequest(url));
      try (InputStream stream = response.contentStream()) {
        FileUtils.copyInputStreamToFile(stream, toFile);
      }
    }
  }
}
