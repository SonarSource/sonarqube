/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.sonar.api.Plugin;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.UriReader;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.components.PastSnapshotFinderByDate;
import org.sonar.batch.components.PastSnapshotFinderByDays;
import org.sonar.batch.components.PastSnapshotFinderByPreviousAnalysis;
import org.sonar.batch.components.PastSnapshotFinderByPreviousVersion;
import org.sonar.batch.components.PastSnapshotFinderByVersion;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.I18nManager;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.metric.CacheMetricFinder;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.SemaphoreUpdater;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.core.user.HibernateUserFinder;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.JpaDatabaseSession;

import java.util.List;
import java.util.Map;

public class BootstrapContainer extends ComponentContainer {

  private BootstrapContainer() {
    super();
  }

  public static BootstrapContainer create(List objects) {
    BootstrapContainer container = new BootstrapContainer();
    container.add(objects);
    return container;
  }

  @Override
  protected void doBeforeStart() {
    addBootstrapComponents();
    addDatabaseComponents();
    addCoreComponents();
  }

  private void addBootstrapComponents() {
    add(
      new PropertiesConfiguration(),
      BootstrapSettings.class,
      AnalysisMode.class,
      PluginDownloader.class,
      BatchPluginRepository.class,
      BatchSettings.class,
      ServerClient.class,
      ExtensionInstaller.class,
      Logback.class,
      ServerMetadata.class,
      org.sonar.batch.ServerMetadata.class,
      TempDirectories.class,
      HttpDownloader.class,
      UriReader.class,
      new FileCacheProvider());
  }

  private void addDatabaseComponents() {
    add(
      PreviewDatabase.class,
      JdbcDriverHolder.class,
      BatchDatabase.class,
      MyBatis.class,
      DatabaseVersion.class,
      // TODO check that it still works (see @Freddy)
      DatabaseCompatibility.class,
      DefaultDatabaseConnector.class,
      JpaDatabaseSession.class,
      BatchDatabaseSessionFactory.class,
      DaoUtils.getDaoClasses(),
      PurgeProfiler.class);
  }

  /**
   * These components MUST not depend on extensions provided by plugins
   */
  private void addCoreComponents() {
    add(
      EmailSettings.class,
      I18nManager.class,
      RuleI18nManager.class,
      MeasuresDao.class,
      RulesDao.class,
      ProfilesDao.class,
      CacheRuleFinder.class,
      CacheMetricFinder.class,
      HibernateUserFinder.class,
      SemaphoreUpdater.class,
      SemaphoresImpl.class,
      PastSnapshotFinderByDate.class,
      PastSnapshotFinderByDays.class,
      PastSnapshotFinderByPreviousAnalysis.class,
      PastSnapshotFinderByVersion.class,
      PastSnapshotFinderByPreviousVersion.class,
      PastMeasuresLoader.class,
      PastSnapshotFinder.class,
      DefaultModelFinder.class);
  }

  @Override
  protected void doAfterStart() {
    installPlugins();
    executeTask();
  }

  private void installPlugins() {
    for (Map.Entry<PluginMetadata, Plugin> entry : getComponentByType(BatchPluginRepository.class).getPluginsByMetadata().entrySet()) {
      PluginMetadata metadata = entry.getKey();
      Plugin plugin = entry.getValue();
      addExtension(metadata, plugin);
    }
  }

  void executeTask() {
    new TaskContainer(this).execute();
  }
}
