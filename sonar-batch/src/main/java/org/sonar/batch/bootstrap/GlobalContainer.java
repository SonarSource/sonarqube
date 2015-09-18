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

import org.sonar.batch.cache.GlobalPersistentCacheProvider;

import javax.annotation.CheckForNull;

import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.cache.StrategyWSLoaderProvider;
import org.sonar.batch.cache.ProjectSyncContainer;
import org.sonar.batch.rule.RulesLoader;
import org.sonar.batch.rule.DefaultRulesLoader;
import org.sonar.batch.rule.RulesProvider;

import java.util.List;
import java.util.Map;

import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.batch.index.CachesManager;
import org.sonar.batch.platform.DefaultServer;
import org.sonar.batch.repository.DefaultGlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesProvider;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.core.config.Logback;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;

public class GlobalContainer extends ComponentContainer {

  private final Map<String, String> bootstrapProperties;
  private boolean forceSync;

  private GlobalContainer(Map<String, String> bootstrapProperties, boolean forceSync) {
    super();
    this.bootstrapProperties = bootstrapProperties;
    this.forceSync = forceSync;
  }

  public static GlobalContainer create(Map<String, String> bootstrapProperties, List<?> extensions, boolean forceSync) {
    GlobalContainer container = new GlobalContainer(bootstrapProperties, forceSync);
    container.add(extensions);
    return container;
  }

  @Override
  protected void doBeforeStart() {
    GlobalProperties bootstrapProps = new GlobalProperties(bootstrapProperties);
    StrategyWSLoaderProvider wsLoaderProvider = forceSync ? new StrategyWSLoaderProvider(LoadStrategy.SERVER_ONLY) : new StrategyWSLoaderProvider(LoadStrategy.SERVER_FIRST);
    add(wsLoaderProvider);
    add(bootstrapProps);
    addBootstrapComponents();
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
      GlobalMode.class,
      GlobalSettings.class,
      new RulesProvider(),
      ServerClient.class,
      Logback.class,
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
    addIfMissing(DefaultRulesLoader.class, RulesLoader.class);
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
    AnalysisProperties props = new AnalysisProperties(analysisProperties, this.getComponentByType(GlobalProperties.class).property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    if (isIssuesMode(props)) {
      String projectKey = getProjectKeyWithBranch(props);
      new ProjectSyncContainer(this, projectKey, false).execute();
    }
    new ProjectScanContainer(this, props, components).execute();
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
