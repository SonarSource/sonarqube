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

import java.util.List;
import java.util.Map;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.batch.index.CachesManager;
import org.sonar.batch.issue.tracking.DefaultServerLineHashesLoader;
import org.sonar.batch.issue.tracking.ServerLineHashesLoader;
import org.sonar.batch.platform.DefaultServer;
import org.sonar.batch.repository.DefaultGlobalRepositoriesLoader;
import org.sonar.batch.repository.DefaultProjectRepositoriesLoader;
import org.sonar.batch.repository.DefaultServerIssuesLoader;
import org.sonar.batch.repository.GlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesProvider;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.repository.user.UserRepository;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.DefaultHttpDownloader;

public class GlobalContainer extends ComponentContainer {

  private final Map<String, String> bootstrapProperties;
  private PersistentCacheProvider persistentCacheProvider;

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
    BootstrapProperties bootstrapProps = new BootstrapProperties(bootstrapProperties);
    DefaultAnalysisMode analysisMode = new DefaultAnalysisMode(bootstrapProps.properties());
    add(bootstrapProps, analysisMode);
    addBootstrapComponents();
  }

  private void addBootstrapComponents() {
    persistentCacheProvider = new PersistentCacheProvider();

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
      Logback.class,
      DefaultServer.class,
      new TempFolderProvider(),
      DefaultHttpDownloader.class,
      UriReader.class,
      new FileCacheProvider(),
      persistentCacheProvider,
      System2.INSTANCE,
      DefaultI18n.class,
      Durations.class,
      new GlobalRepositoriesProvider(),
      UserRepository.class);
    addIfMissing(BatchPluginInstaller.class, PluginInstaller.class);
    addIfMissing(DefaultGlobalRepositoriesLoader.class, GlobalRepositoriesLoader.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
    addIfMissing(DefaultServerIssuesLoader.class, ServerIssuesLoader.class);
    addIfMissing(DefaultServerLineHashesLoader.class, ServerLineHashesLoader.class);
  }

  public void addIfMissing(Object object, Class<?> objectType) {
    if (getComponentByType(objectType) == null) {
      add(object);
    }
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

  public void executeAnalysis(Map<String, String> analysisProperties, Object... components) {
    AnalysisProperties props = new AnalysisProperties(analysisProperties, this.getComponentByType(BootstrapProperties.class).property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    persistentCacheProvider.reconfigure(props);
    new ProjectScanContainer(this, props, components).execute();
  }
}
