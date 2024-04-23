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

import com.google.gson.Gson;
import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonar.scanner.mediumtest.LocalPlugin;
import org.sonarqube.ws.client.GetRequest;

import static java.lang.String.format;

/**
 * Downloads the plugins installed on server and stores them in a local user cache
 */
public class ScannerPluginInstaller implements PluginInstaller {

  private static final Logger LOG = Loggers.get(ScannerPluginInstaller.class);
  private static final String PLUGINS_WS_URL = "api/plugins/installed";

  private final PluginFiles pluginFiles;
  private final DefaultScannerWsClient wsClient;

  private List<InstalledPlugin> availablePlugins;

  public ScannerPluginInstaller(PluginFiles pluginFiles, DefaultScannerWsClient wsClient) {
    this.pluginFiles = pluginFiles;
    this.wsClient = wsClient;
  }

  @Override
  public Map<String, ScannerPlugin> installAllPlugins() {
    LOG.info("Loading all plugins");
    return installPlugins(p -> true).installedPluginsByKey;
  }

  @Override
  public Map<String, ScannerPlugin> installRequiredPlugins() {
    LOG.info("Loading required plugins");
    InstallResult result = installPlugins(p -> p.getRequiredForLanguages() == null || p.getRequiredForLanguages().isEmpty());

    LOG.debug("Plugins not loaded because they are optional: {}", result.skippedPlugins);

    return result.installedPluginsByKey;
  }

  @Override
  public Map<String, ScannerPlugin> installPluginsForLanguages(Set<String> languageKeys) {
    LOG.info("Loading plugins for detected languages");
    LOG.debug("Detected languages: {}", languageKeys);
    InstallResult result = installPlugins(
      p -> p.getRequiredForLanguages() != null && !Collections.disjoint(p.getRequiredForLanguages(), languageKeys)
    );

    List<InstalledPlugin> skippedLanguagePlugins = result.skippedPlugins.stream()
      .filter(p -> p.getRequiredForLanguages() != null && !p.getRequiredForLanguages().isEmpty()).toList();
    LOG.debug("Optional language-specific plugins not loaded: {}", skippedLanguagePlugins);

    return result.installedPluginsByKey;
  }

  private InstallResult installPlugins(Predicate<InstalledPlugin> pluginFilter) {
    if (this.availablePlugins == null) {
      this.availablePlugins = listInstalledPlugins();
    }

    Profiler profiler = Profiler.create(LOG).startInfo("Load/download plugins");
    try {
      InstallResult result = new InstallResult();
      Loaded loaded = loadPlugins(result, pluginFilter);
      if (!loaded.ok) {
        // retry once, a plugin may have been uninstalled during downloads
        this.availablePlugins = listInstalledPlugins();
        result.installedPluginsByKey.clear();
        loaded = loadPlugins(result, pluginFilter);
        if (!loaded.ok) {
          throw new IllegalStateException(format("Fail to download plugin [%s]. Not found.", loaded.notFoundPlugin));
        }
      }
      return result;
    } finally {
      profiler.stopInfo();
    }
  }

  private Loaded loadPlugins(InstallResult result, Predicate<InstalledPlugin> pluginFilter) {
    List<InstalledPlugin> pluginsToInstall = availablePlugins.stream()
      .filter(pluginFilter).toList();

    for (InstalledPlugin plugin : pluginsToInstall) {
      Optional<File> jarFile = pluginFiles.get(plugin);
      if (jarFile.isEmpty()) {
        return new Loaded(false, plugin.key);
      }

      PluginInfo info = PluginInfo.create(jarFile.get());
      result.installedPluginsByKey.put(info.getKey(), new ScannerPlugin(plugin.key, plugin.updatedAt, PluginType.valueOf(plugin.type), info));
    }

    result.skippedPlugins = availablePlugins.stream()
      .filter(Predicate.not(pluginFilter)).toList();

    return new Loaded(true, null);
  }

  /**
   * Returns empty on purpose. This method is used only by medium tests.
   */
  @Override
  public List<LocalPlugin> installLocals() {
    return Collections.emptyList();
  }

  /**
   * Returns empty on purpose. This method is used only by medium tests.
   */
  @Override
  public List<LocalPlugin> installOptionalLocals(Set<String> languageKeys) {
    return Collections.emptyList();
  }

  /**
   * Gets information about the plugins installed on server (filename, checksum)
   */
  private List<InstalledPlugin> listInstalledPlugins() {
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

  private static class InstallResult {
    Map<String, ScannerPlugin> installedPluginsByKey = new HashMap<>();
    List<InstalledPlugin> skippedPlugins = new ArrayList<>();
  }

  private static class InstalledPlugins {
    List<InstalledPlugin> plugins;

    public InstalledPlugins() {
      // http://stackoverflow.com/a/18645370/229031
    }
  }

  static class InstalledPlugin {
    String key;
    String hash;
    long updatedAt;
    String type;
    private Set<String> requiredForLanguages;

    public InstalledPlugin() {
      // http://stackoverflow.com/a/18645370/229031
    }

    public Set<String> getRequiredForLanguages() {
      return requiredForLanguages;
    }

    @Override
    public String toString() {
      return key;
    }

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
