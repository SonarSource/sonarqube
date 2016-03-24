/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.bootstrap;

import java.util.List;
import java.util.Map;
import org.sonar.api.Plugin;
import org.sonar.api.internal.SonarQubeVersionFactory;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.batch.cache.GlobalPersistentCacheProvider;
import org.sonar.batch.cache.ProjectSyncContainer;
import org.sonar.batch.cache.StrategyWSLoaderProvider;
import org.sonar.batch.cache.WSLoader.LoadStrategy;
import org.sonar.batch.index.CachesManager;
import org.sonar.batch.platform.DefaultServer;
import org.sonar.batch.repository.DefaultGlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesLoader;
import org.sonar.batch.repository.GlobalRepositoriesProvider;
import org.sonar.batch.task.TaskContainer;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;

public class GlobalContainer extends ComponentContainer {

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

      SonarQubeVersionFactory.create(System2.INSTANCE),
      CachesManager.class,
      GlobalSettings.class,
      new BatchWsClientProvider(),
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
      Plugin instance = pluginRepository.getPluginInstance(pluginInfo.getKey());
      addExtension(pluginInfo, instance);
    }
  }

  public void executeTask(Map<String, String> taskProperties, Object... components) {
    new TaskContainer(this, taskProperties, components).execute();
  }

  public void syncProject(String projectKey, boolean force) {
    new ProjectSyncContainer(this, projectKey, force).execute();
  }

}
