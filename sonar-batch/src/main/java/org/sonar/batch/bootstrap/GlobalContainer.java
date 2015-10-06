/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import com.github.kevinsawicki.http.HttpRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.cache.GlobalPersistentCacheProvider;
import org.sonar.batch.cache.ProjectSyncContainer;
import org.sonar.batch.cache.StrategyWSLoaderProvider;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.batch.index.CachesManager;
import org.sonar.batch.platform.DefaultServer;
import org.sonar.batch.repository.DefaultGlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesProvider;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;

import static java.lang.String.format;

public class GlobalContainer extends ComponentContainer {

  private static final org.sonar.api.utils.log.Logger LOG = Loggers.get(GlobalContainer.class);

  private final Map<String, String> bootstrapProperties;
  private boolean preferCache;

  private GlobalContainer(Map<String, String> bootstrapProperties, boolean preferCache) {
    super();
    this.bootstrapProperties = bootstrapProperties;
    this.preferCache = preferCache;
  }

  public static GlobalContainer create(Map<String, String> bootstrapProperties, List<?> extensions, boolean preferCache) {
    GlobalContainer container = new GlobalContainer(bootstrapProperties, preferCache);
    container.add(extensions);
    return container;
  }

  @Override
  protected void doBeforeStart() {
    GlobalProperties bootstrapProps = new GlobalProperties(bootstrapProperties);
    GlobalMode globalMode = new GlobalMode(bootstrapProps);
    LoadStrategy strategy = getDataLoadingStrategy(globalMode, preferCache);
    StrategyWSLoaderProvider wsLoaderProvider = new StrategyWSLoaderProvider(strategy);
    add(wsLoaderProvider);
    add(bootstrapProps);
    add(globalMode);
    addBootstrapComponents();
  }

  private static LoadStrategy getDataLoadingStrategy(GlobalMode mode, boolean preferCache) {
    if (!mode.isIssues()) {
      return LoadStrategy.SERVER_ONLY;
    }

    return preferCache ? LoadStrategy.CACHE_FIRST : LoadStrategy.SERVER_FIRST;
  }

  private void addBootstrapComponents() {
    add(
      // plugins
      BatchPluginRepository.class,
      PluginLoader.class,
      PluginClassloaderFactory.class,
      BatchPluginJarExploder.class,
      BatchPluginPredicate.class,
      ExtensionInstaller.class,

      CachesManager.class,
      GlobalSettings.class,
      ServerClient.class,
      DefaultServer.class,
      new GlobalTempFolderProvider(),
      DefaultHttpDownloader.class,
      UriReader.class,
      new FileCacheProvider(),
      new GlobalPersistentCacheProvider(),
      System2.INSTANCE,
      new GlobalRepositoriesProvider(),
      UuidFactoryImpl.INSTANCE);
    addIfMissing(BatchPluginInstaller.class, PluginInstaller.class);
    addIfMissing(DefaultGlobalRepositoriesLoader.class, GlobalRepositoriesLoader.class);
  }

  @Override
  protected void doAfterStart() {
    installPlugins();
  }

  private void installPlugins() {
    PluginRepository pluginRepository = getComponentByType(PluginRepository.class);
    for (PluginInfo pluginInfo : pluginRepository.getPluginInfos()) {
      SonarPlugin instance = pluginRepository.getPluginInstance(pluginInfo.getKey());
      addExtension(pluginInfo, instance);
    }
  }

  public void executeAnalysis(Map<String, String> analysisProperties, Object... components) {
    GlobalProperties globalProperties = this.getComponentByType(GlobalProperties.class);
    // SONAR-6888
    String task = analysisProperties.get(CoreProperties.TASK);
    if ("views".equals(task)) {
      triggerViews(this.getComponentByType(ServerClient.class), this.getComponentByType(Server.class));
      return;
    }
    if (StringUtils.isNotBlank(task) && !CoreProperties.SCAN_TASK.equals(task)) {
      throw MessageException.of("Tasks are no more supported on batch side since SonarQube 5.2");
    }

    AnalysisProperties props = new AnalysisProperties(analysisProperties, globalProperties.property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    if (isIssuesMode(props)) {
      String projectKey = getProjectKeyWithBranch(props);
      new ProjectSyncContainer(this, projectKey, false).execute();
    }
    new ProjectScanContainer(this, props, components).execute();
  }

  private static void triggerViews(ServerClient serverClient, Server server) {
    LOG.info("Trigger Views update");
    URL url;
    try {
      url = new URL(serverClient.getURL() + "/api/views/run");
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL", e);
    }
    HttpRequest request = HttpRequest.post(url);
    request.trustAllCerts();
    request.trustAllHosts();
    request.header("User-Agent", format("SonarQube %s", server.getVersion()));
    request.basic(serverClient.getLogin(), serverClient.getPassword());
    if (!request.ok()) {
      int responseCode = request.code();
      if (responseCode == 401) {
        throw new IllegalStateException(format(serverClient.getMessageWhenNotAuthorized(), CoreProperties.LOGIN, CoreProperties.PASSWORD));
      }
      if (responseCode == 409) {
        throw new IllegalStateException("A full refresh of Views is already queued or running");
      }
      if (responseCode == 403) {
        // SONAR-4397 Details are in response content
        throw new IllegalStateException(request.body());
      }
      throw new IllegalStateException(format("Fail to execute request [code=%s, url=%s]: %s", responseCode, url, request.body()));
    }
  }

  @CheckForNull
  private static String getProjectKeyWithBranch(AnalysisProperties props) {
    String projectKey = props.property(CoreProperties.PROJECT_KEY_PROPERTY);
    if (projectKey != null && props.property(CoreProperties.PROJECT_BRANCH_PROPERTY) != null) {
      projectKey = projectKey + ":" + props.property(CoreProperties.PROJECT_BRANCH_PROPERTY);
    }
    return projectKey;
  }

  public void syncProject(String projectKey, boolean force) {
    new ProjectSyncContainer(this, projectKey, force).execute();
  }

  private boolean isIssuesMode(AnalysisProperties props) {
    DefaultAnalysisMode mode = new DefaultAnalysisMode(this.getComponentByType(GlobalProperties.class), props);
    return mode.isIssues();
  }

}
