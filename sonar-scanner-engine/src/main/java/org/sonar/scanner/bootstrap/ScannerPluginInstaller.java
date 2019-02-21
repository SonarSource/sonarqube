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

import com.google.gson.Gson;
import java.io.File;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.PluginInfo;
import org.sonarqube.ws.client.GetRequest;

import static java.lang.String.format;

/**
 * Downloads the plugins installed on server and stores them in a local user cache
 */
public class ScannerPluginInstaller implements PluginInstaller {

  private static final Logger LOG = Loggers.get(ScannerPluginInstaller.class);
  private static final String PLUGINS_WS_URL = "api/plugins/installed";

  private final PluginFiles pluginFiles;
  private final ScannerWsClient wsClient;

  public ScannerPluginInstaller(PluginFiles pluginFiles, ScannerWsClient wsClient) {
    this.pluginFiles = pluginFiles;
    this.wsClient = wsClient;
  }

  @Override
  public Map<String, ScannerPlugin> installRemotes() {
    Profiler profiler = Profiler.create(LOG).startInfo("Load/download plugins");
    try {
      Map<String, ScannerPlugin> result = new HashMap<>();
      Loaded loaded = loadPlugins(result);
      if (!loaded.ok) {
        // retry once, a plugin may have been uninstalled during downloads
        result.clear();
        loaded = loadPlugins(result);
        if (!loaded.ok) {
          throw new IllegalStateException(format("Fail to download plugin [%s]. Not found.", loaded.notFoundPlugin));
        }
      }
      return result;
    } finally {
      profiler.stopInfo();
    }
  }

  private Loaded loadPlugins(Map<String, ScannerPlugin> result) {
    for (InstalledPlugin plugin : listInstalledPlugins()) {
      Optional<File> jarFile = pluginFiles.get(plugin);
      if (!jarFile.isPresent()) {
        return new Loaded(false, plugin.key);
      }

      PluginInfo info = PluginInfo.create(jarFile.get());
      result.put(info.getKey(), new ScannerPlugin(plugin.key, plugin.updatedAt, info));
    }
    return new Loaded(true, null);
  }

  /**
   * Returns empty on purpose. This method is used only by medium tests.
   */
  @Override
  public List<Object[]> installLocals() {
    return Collections.emptyList();
  }

  /**
   * Gets information about the plugins installed on server (filename, checksum)
   */
  private InstalledPlugin[] listInstalledPlugins() {
    Profiler profiler = Profiler.create(LOG).startInfo("Load plugins index");
    GetRequest getRequest = new GetRequest(PLUGINS_WS_URL);
    InstalledPlugins installedPlugins;
    try (Reader reader = wsClient.call(getRequest).contentReader()) {
      installedPlugins = new Gson().fromJson(reader, InstalledPlugins.class);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to parse response of " + PLUGINS_WS_URL, e);
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
    long updatedAt;
  }

  private static class Loaded {
    private final boolean ok;
    @Nullable
    private final String notFoundPlugin;

    private Loaded(boolean ok, @Nullable String notFoundPlugin) {
      this.ok = ok;
      this.notFoundPlugin = notFoundPlugin;
    }
  }
}
