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

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.internal.ApiVersion;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.scanner.platform.DefaultServer;
import org.sonar.scanner.repository.DefaultMetricsRepositoryLoader;
import org.sonar.scanner.repository.MetricsRepositoryLoader;
import org.sonar.scanner.repository.MetricsRepositoryProvider;
import org.sonar.scanner.repository.settings.DefaultSettingsLoader;
import org.sonar.scanner.repository.settings.SettingsLoader;
import org.sonar.scanner.storage.StoragesManager;
import org.sonar.scanner.task.TaskContainer;

public class GlobalContainer extends ComponentContainer {
  private static final Logger LOG = Loggers.get(GlobalContainer.class);
  private final Map<String, String> bootstrapProperties;

  private GlobalContainer(Map<String, String> bootstrapProperties) {
    super();
    this.bootstrapProperties = bootstrapProperties;
  }

  public static GlobalContainer create(Map<String, String> bootstrapProperties, List<?> extensions) {
    GlobalContainer container = new GlobalContainer(bootstrapProperties);
    container.add(extensions);
    return container;
  }

  @Override
  protected void doBeforeStart() {
    GlobalProperties bootstrapProps = new GlobalProperties(bootstrapProperties);
    GlobalAnalysisMode globalMode = new GlobalAnalysisMode(bootstrapProps);
    add(bootstrapProps);
    add(globalMode);
    addBootstrapComponents();
  }

  private void addBootstrapComponents() {
    Version apiVersion = ApiVersion.load(System2.INSTANCE);
    add(
      // plugins
      ScannerPluginRepository.class,
      PluginLoader.class,
      PluginClassloaderFactory.class,
      ScannerPluginJarExploder.class,
      ScannerPluginPredicate.class,
      ExtensionInstaller.class,

      new SonarQubeVersion(apiVersion),
      SonarRuntimeImpl.forSonarQube(apiVersion, SonarQubeSide.SCANNER),
      StoragesManager.class,
      MutableGlobalSettings.class,
      new GlobalConfigurationProvider(),
      new ScannerWsClientProvider(),
      DefaultServer.class,
      new GlobalTempFolderProvider(),
      DefaultHttpDownloader.class,
      UriReader.class,
      new FileCacheProvider(),
      System2.INSTANCE,
      Clock.systemDefaultZone(),
      new MetricsRepositoryProvider(),
      UuidFactoryImpl.INSTANCE);
    addIfMissing(ScannerPluginInstaller.class, PluginInstaller.class);
    addIfMissing(DefaultSettingsLoader.class, SettingsLoader.class);
    addIfMissing(DefaultMetricsRepositoryLoader.class, MetricsRepositoryLoader.class);
  }

  @Override
  protected void doAfterStart() {
    installPlugins();
  }

  private void installPlugins() {
    PluginRepository pluginRepository = getComponentByType(PluginRepository.class);
    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      Plugin instance = pluginRepository.getPluginInstance(pluginInfo.getKey());
      addExtension(pluginInfo, instance);
    }
  }

  public void executeTask(Map<String, String> taskProperties, Object... components) {
    long startTime = System.currentTimeMillis();
    new TaskContainer(this, taskProperties, components).execute();

    LOG.info("Task total time: {}", formatTime(System.currentTimeMillis() - startTime));
  }

  static String formatTime(long time) {
    long h = time / (60 * 60 * 1000);
    long m = (time - h * 60 * 60 * 1000) / (60 * 1000);
    long s = (time - h * 60 * 60 * 1000 - m * 60 * 1000) / 1000;
    long ms = time % 1000;
    final String format;
    if (h > 0) {
      format = "%1$d:%2$02d:%3$02d.%4$03d s";
    } else if (m > 0) {
      format = "%2$d:%3$02d.%4$03d s";
    } else {
      format = "%3$d.%4$03d s";
    }
    return String.format(format, h, m, s, ms);
  }

}
