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

import java.time.Clock;
import java.util.List;
import java.util.Map;
import javax.annotation.Priority;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.Version;
import org.sonar.core.documentation.DefaultDocumentationLinkGenerator;
import org.sonar.core.extension.CoreExtensionRepositoryImpl;
import org.sonar.core.extension.CoreExtensionsLoader;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.scanner.extension.ScannerCoreExtensionsInstaller;
import org.sonar.scanner.http.ScannerWsClientProvider;
import org.sonar.scanner.notifications.DefaultAnalysisWarnings;
import org.sonar.scanner.platform.DefaultServer;
import org.sonar.scanner.repository.DefaultMetricsRepositoryLoader;
import org.sonar.scanner.repository.DefaultNewCodePeriodLoader;
import org.sonar.scanner.repository.MetricsRepositoryProvider;
import org.sonar.scanner.repository.settings.DefaultGlobalSettingsLoader;

@Priority(4)
public class SpringGlobalContainer extends SpringComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(SpringGlobalContainer.class);
  private final Map<String, String> scannerProperties;

  private SpringGlobalContainer(Map<String, String> scannerProperties, List<?> addedExternally) {
    super(addedExternally);
    this.scannerProperties = scannerProperties;
  }

  public static SpringGlobalContainer create(Map<String, String> scannerProperties, List<?> extensions) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("JVM max available memory: {}", FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()));
    }
    return new SpringGlobalContainer(scannerProperties, extensions);
  }

  @Override
  public void doBeforeStart() {
    ScannerProperties rawScannerProperties = new ScannerProperties(scannerProperties);
    GlobalAnalysisMode globalMode = new GlobalAnalysisMode(rawScannerProperties);
    add(rawScannerProperties);
    add(globalMode);
    addBootstrapComponents();
  }

  private void addBootstrapComponents() {
    Version apiVersion = MetadataLoader.loadApiVersion(System2.INSTANCE);
    Version sqVersion = MetadataLoader.loadSQVersion(System2.INSTANCE);
    SonarEdition edition = MetadataLoader.loadEdition(System2.INSTANCE);
    DefaultAnalysisWarnings analysisWarnings = new DefaultAnalysisWarnings(System2.INSTANCE);
    LOG.debug("{} {}", edition.getLabel(), sqVersion);
    add(
      // plugins
      ScannerPluginRepository.class,
      PluginClassLoader.class,
      PluginClassloaderFactory.class,
      ScannerPluginJarExploder.class,
      ExtensionInstaller.class,
      new SonarQubeVersion(sqVersion),
      new GlobalServerSettingsProvider(),
      new GlobalConfigurationProvider(),
      new ScannerWsClientProvider(),
      DefaultServer.class,
      DefaultDocumentationLinkGenerator.class,
      new GlobalTempFolderProvider(),
      new SonarUserHomeProvider(),
      analysisWarnings,
      UriReader.class,
      PluginFiles.class,
      System2.INSTANCE,
      Clock.systemDefaultZone(),
      new MetricsRepositoryProvider(),
      UuidFactoryImpl.INSTANCE,
      DefaultHttpDownloader.class,
      SonarRuntimeImpl.forSonarQube(apiVersion, SonarQubeSide.SCANNER, edition),
      ScannerPluginInstaller.class,
      CoreExtensionRepositoryImpl.class,
      CoreExtensionsLoader.class,
      ScannerCoreExtensionsInstaller.class,
      DefaultGlobalSettingsLoader.class,
      DefaultNewCodePeriodLoader.class,
      DefaultMetricsRepositoryLoader.class,
      RuntimeJavaVersion.class);
  }

  @Override
  protected void doAfterStart() {
    installRequiredPlugins();
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
    getComponentByType(RuntimeJavaVersion.class).checkJavaVersion();
    new SpringScannerContainer(this).execute();

    LOG.info("Analysis total time: {}", formatTime(System.currentTimeMillis() - startTime));
  }

  private void installRequiredPlugins() {
    PluginRepository pluginRepository = getComponentByType(PluginRepository.class);
    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      Plugin instance = pluginRepository.getPluginInstance(pluginInfo.getKey());
      addExtension(pluginInfo, instance);
    }
  }

  private void loadCoreExtensions() {
    getComponentByType(CoreExtensionsLoader.class).load();
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
