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

import org.sonar.api.Plugin;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.internal.TempFolderCleaner;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByDate;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByDays;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByPreviousAnalysis;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByPreviousVersion;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByVersion;
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
import org.sonar.core.cluster.NullQueue;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.SemaphoreUpdater;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.core.user.HibernateUserFinder;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.JpaDatabaseSession;

import java.util.List;
import java.util.Map;

public class GlobalContainer extends ComponentContainer {

  private final Map<String, String> bootstrapProperties;

  private GlobalContainer(Map<String, String> bootstrapProperties) {
    super();
    this.bootstrapProperties = bootstrapProperties;
  }

  public static GlobalContainer create(Map<String, String> bootstrapProperties, List extensions) {
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
    if (analysisMode.isDb()) {
      addDatabaseComponents();
    }

  }

  private void addBootstrapComponents() {
    add(
      // plugins
      BatchPluginRepository.class,
      PluginLoader.class,
      BatchPluginUnzipper.class,
      BatchPluginPredicate.class,
      ExtensionInstaller.class,

      GlobalSettings.class,
      ServerClient.class,
      Logback.class,
      DefaultServer.class,
      new TempFolderProvider(),
      TempFolderCleaner.class,
      DefaultHttpDownloader.class,
      UriReader.class,
      new FileCacheProvider(),
      System2.INSTANCE,
      DefaultI18n.class,
      new GlobalRepositoriesProvider(),
      UserRepository.class);
    addIfMissing(BatchPluginInstaller.class, PluginInstaller.class);
    addIfMissing(DefaultGlobalRepositoriesLoader.class, GlobalRepositoriesLoader.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
    addIfMissing(DefaultServerIssuesLoader.class, ServerIssuesLoader.class);
    addIfMissing(DefaultServerLineHashesLoader.class, ServerLineHashesLoader.class);
  }

  public void addIfMissing(Object object, Class objectType) {
    if (getComponentByType(objectType) == null) {
      add(object);
    }
  }

  private void addDatabaseComponents() {
    add(
      JdbcDriverHolder.class,
      BatchDatabase.class,
      MyBatis.class,
      NullQueue.class,
      DatabaseVersion.class,
      DatabaseCompatibility.class,
      DefaultDatabaseConnector.class,
      JpaDatabaseSession.class,
      BatchDatabaseSessionFactory.class,
      DaoUtils.getDaoClasses(),
      PurgeProfiler.class,
      CacheRuleFinder.class,
      EmailSettings.class,
      RuleI18nManager.class,
      MeasuresDao.class,
      HibernateUserFinder.class,
      SemaphoreUpdater.class,
      SemaphoresImpl.class,
      PastSnapshotFinderByDate.class,
      PastSnapshotFinderByDays.class,
      PastSnapshotFinderByPreviousAnalysis.class,
      PastSnapshotFinderByVersion.class,
      PastSnapshotFinderByPreviousVersion.class,
      PastSnapshotFinder.class,
      Durations.class);
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

}
