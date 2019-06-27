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

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.extension.CoreExtensionRepositoryImpl;
import org.sonar.core.extension.CoreExtensionsLoader;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.scanner.extension.ScannerCoreExtensionsInstaller;
import org.sonar.scanner.platform.DefaultServer;
import org.sonar.scanner.repository.DefaultMetricsRepositoryLoader;
import org.sonar.scanner.repository.MetricsRepositoryLoader;
import org.sonar.scanner.repository.MetricsRepositoryProvider;
import org.sonar.scanner.repository.settings.DefaultGlobalSettingsLoader;
import org.sonar.scanner.repository.settings.GlobalSettingsLoader;
import org.sonar.scanner.scan.ProjectScanContainer;

public class GlobalContainer extends ComponentContainer {
  private static final Logger LOG = Loggers.get(GlobalContainer.class);
  private final Map<String, String> scannerProperties;

  private GlobalContainer(Map<String, String> scannerProperties) {
    super();
    this.scannerProperties = scannerProperties;
  }

  public static GlobalContainer create(Map<String, String> scannerProperties, List<?> extensions) {
    GlobalContainer container = new GlobalContainer(scannerProperties);
    container.add(extensions);
    return container;
  }

  @Override
  protected void doBeforeStart() {
    RawScannerProperties rawScannerProperties = new RawScannerProperties(scannerProperties);
    GlobalAnalysisMode globalMode = new GlobalAnalysisMode(rawScannerProperties);
    add(rawScannerProperties);
    add(globalMode);
    addBootstrapComponents();
  }

  private static void checkJavaVersion() {
    try {
      String.class.getMethod("isBlank");
    } catch (NoSuchMethodException e) {
      LOG.warn("SonarScanner will require Java 11+ to run starting in SonarQube 8.x");
    }
  }

  private void addBootstrapComponents() {
    Version apiVersion = MetadataLoader.loadVersion(System2.INSTANCE);
    SonarEdition edition = MetadataLoader.loadEdition(System2.INSTANCE);
    if (edition != SonarEdition.SONARCLOUD) {
      checkJavaVersion();
    }
    LOG.debug("{} {}", edition.getLabel(), apiVersion);
    add(
      // plugins
      ScannerPluginRepository.class,
      PluginLoader.class,
      PluginClassloaderFactory.class,
      ScannerPluginJarExploder.class,
      ExtensionInstaller.class,
      new SonarQubeVersion(apiVersion),
      new GlobalServerSettingsProvider(),
      new GlobalConfigurationProvider(),
      new ScannerWsClientProvider(),
      DefaultServer.class,
      new GlobalTempFolderProvider(),
      DefaultHttpDownloader.class,
      UriReader.class,
      PluginFiles.class,
      System2.INSTANCE,
      Clock.systemDefaultZone(),
      new MetricsRepositoryProvider(),
      UuidFactoryImpl.INSTANCE);
    addIfMissing(SonarRuntimeImpl.forSonarQube(apiVersion, SonarQubeSide.SCANNER, edition), SonarRuntime.class);
    addIfMissing(ScannerPluginInstaller.class, PluginInstaller.class);
    add(CoreExtensionRepositoryImpl.class, CoreExtensionsLoader.class, ScannerCoreExtensionsInstaller.class);
    addIfMissing(DefaultGlobalSettingsLoader.class, GlobalSettingsLoader.class);
    addIfMissing(DefaultMetricsRepositoryLoader.class, MetricsRepositoryLoader.class);
  }

  @Override
  protected void doAfterStart() {
    installPlugins();
    loadCoreExtensions();

    long startTime = System.currentTimeMillis();
    String taskKey = StringUtils.defaultIfEmpty(scannerProperties.get(CoreProperties.TASK), CoreProperties.SCAN_TASK);
    if (taskKey.equals("views")) {
      throw MessageException.of("The task 'views' was removed with SonarQube 7.1. " +
        "You can safely remove this call since portfolios and applications are automatically re-calculated.");
    } else if (!taskKey.equals(CoreProperties.SCAN_TASK)) {
      throw MessageException.of("Tasks support was removed in SonarQube 7.6.");
    }
    String analysisMode = StringUtils.defaultIfEmpty(scannerProperties.get("sonar.analysis.mode"), "publish");
    if (!analysisMode.equals("publish")) {
      throw MessageException.of("The preview mode, along with the 'sonar.analysis.mode' parameter, is no more supported. You should stop using this parameter.");
    }
    new ProjectScanContainer(this).execute();

    LOG.info("Analysis total time: {}", formatTime(System.currentTimeMillis() - startTime));
  }

  private void installPlugins() {
    PluginRepository pluginRepository = getComponentByType(PluginRepository.class);
    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      Plugin instance = pluginRepository.getPluginInstance(pluginInfo.getKey());
      addExtension(pluginInfo, instance);
    }
  }

  private void loadCoreExtensions() {
    CoreExtensionsLoader loader = getComponentByType(CoreExtensionsLoader.class);
    loader.load();
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
