/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.platform;

import org.apache.commons.configuration.BaseConfiguration;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugins;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.Server;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.IocContainer;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.i18n.I18nManager;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.metric.DefaultMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.persistence.*;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.jpa.dao.DaoFacade;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;
import org.sonar.server.charts.ChartFactory;
import org.sonar.server.configuration.Backup;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.database.EmbeddedDatabaseFactory;
import org.sonar.server.filters.FilterExecutor;
import org.sonar.server.notifications.NotificationService;
import org.sonar.server.notifications.reviews.ReviewsNotificationManager;
import org.sonar.server.plugins.*;
import org.sonar.server.qualitymodel.DefaultModelManager;
import org.sonar.server.rules.ProfilesConsole;
import org.sonar.server.rules.RulesConsole;
import org.sonar.server.startup.*;
import org.sonar.server.ui.CodeColorizers;
import org.sonar.server.ui.JRubyI18n;
import org.sonar.server.ui.SecurityRealmFactory;
import org.sonar.server.ui.Views;

import javax.servlet.ServletContext;

/**
 * @since 2.2
 */
public final class Platform {

  private static final Platform INSTANCE = new Platform();

  private ComponentContainer rootContainer;// level 1 : only database connectors
  private ComponentContainer coreContainer;// level 2 : level 1 + core components
  private ComponentContainer servicesContainer;// level 3 : level 2 + plugin extensions + core components that depend on plugin extensions

  private boolean connected = false;
  private boolean started = false;

  public static Platform getInstance() {
    return INSTANCE;
  }

  private Platform() {
  }

  public void init(ServletContext servletContext) {
    if (!connected) {
      try {
        startDatabaseConnectors(servletContext);
        connected = true;

      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).error("Can not start Sonar", e);
      }
    }
  }

  public void start() {
    if (!started && getDatabaseStatus()== DatabaseVersion.Status.UP_TO_DATE) {
      try {
        TimeProfiler profiler = new TimeProfiler().start("Start components");
        startCoreComponents();
        startServiceComponents();
        executeStartupTasks();
        started = true;
        profiler.stop();
      } catch (Exception e) {
        // full stacktrace is lost by jruby. It must be logged now.
        LoggerFactory.getLogger(getClass()).error("Fail to start server", e);
        throw new IllegalStateException("Fail to start server", e);
      }
    }
  }

  private void startDatabaseConnectors(ServletContext servletContext) {
    rootContainer = new ComponentContainer();
    rootContainer.addSingleton(servletContext);
    rootContainer.addSingleton(IocContainer.class); // for backward compatibility
    rootContainer.addSingleton(new BaseConfiguration());
    rootContainer.addSingleton(ServerSettings.class);
    rootContainer.addSingleton(EmbeddedDatabaseFactory.class);
    rootContainer.addSingleton(DefaultDatabase.class);
    rootContainer.addSingleton(MyBatis.class);
    rootContainer.addSingleton(DefaultServerUpgradeStatus.class);
    rootContainer.addSingleton(DatabaseServerCompatibility.class);
    rootContainer.addSingleton(DatabaseMigrator.class);
    rootContainer.addSingleton(DatabaseVersion.class);
    for (Class daoClass : DaoUtils.getDaoClasses()) {
      rootContainer.addSingleton(daoClass);
    }
    rootContainer.startComponents();
  }

  private DatabaseVersion.Status getDatabaseStatus() {
    DatabaseVersion version = getContainer().getComponentByType(DatabaseVersion.class);
    return version.getStatus();
  }

  private void startCoreComponents() {
    coreContainer = rootContainer.createChild();
    coreContainer.addSingleton(DefaultDatabaseConnector.class);
    coreContainer.addSingleton(PluginDeployer.class);
    coreContainer.addSingleton(DefaultServerPluginRepository.class);
    coreContainer.addSingleton(ServerExtensionInstaller.class);
    coreContainer.addSingleton(DefaultServerFileSystem.class);
    coreContainer.addSingleton(ThreadLocalDatabaseSessionFactory.class);
    coreContainer.addPicoAdapter(new DatabaseSessionProvider());
    coreContainer.startComponents();

    DatabaseSessionFactory sessionFactory = coreContainer.getComponentByType(DatabaseSessionFactory.class);
    ServerSettings serverSettings = coreContainer.getComponentByType(ServerSettings.class);
    serverSettings.setSessionFactory(sessionFactory);
    serverSettings.load();
  }

  /**
   * plugin extensions + all the components that depend on plugin extensions
   */
  private void startServiceComponents() {
    servicesContainer = coreContainer.createChild();
    ServerExtensionInstaller extensionRegistrar = servicesContainer.getComponentByType(ServerExtensionInstaller.class);
    extensionRegistrar.registerExtensions(servicesContainer);

    servicesContainer.addSingleton(HttpDownloader.class);
    servicesContainer.addSingleton(UpdateCenterClient.class);
    servicesContainer.addSingleton(UpdateCenterMatrixFactory.class);
    servicesContainer.addSingleton(PluginDownloader.class);
    servicesContainer.addSingleton(ServerIdGenerator.class);
    servicesContainer.addSingleton(ServerImpl.class);
    servicesContainer.addComponent(FilterExecutor.class, false);
    servicesContainer.addSingleton(DefaultModelFinder.class); // depends on plugins
    servicesContainer.addSingleton(DefaultModelManager.class);
    servicesContainer.addSingleton(Plugins.class);
    servicesContainer.addSingleton(ChartFactory.class);
    servicesContainer.addSingleton(Languages.class);
    servicesContainer.addSingleton(Views.class);
    servicesContainer.addSingleton(CodeColorizers.class);
    servicesContainer.addComponent(RulesDao.class, false);
    servicesContainer.addComponent(MeasuresDao.class, false);
    servicesContainer.addComponent(org.sonar.api.database.daos.MeasuresDao.class, false);
    servicesContainer.addComponent(ProfilesDao.class, false);
    servicesContainer.addComponent(DaoFacade.class, false);
    servicesContainer.addComponent(DefaultRulesManager.class, false);
    servicesContainer.addComponent(ProfilesManager.class, false);
    servicesContainer.addComponent(Backup.class, false);
    servicesContainer.addSingleton(SecurityRealmFactory.class);
    servicesContainer.addSingleton(ServerLifecycleNotifier.class);
    servicesContainer.addSingleton(AnnotationProfileParser.class);
    servicesContainer.addSingleton(XMLProfileParser.class);
    servicesContainer.addSingleton(XMLProfileSerializer.class);
    servicesContainer.addSingleton(AnnotationRuleParser.class);
    servicesContainer.addSingleton(XMLRuleParser.class);
    servicesContainer.addSingleton(DefaultRuleFinder.class);
    servicesContainer.addSingleton(DefaultMetricFinder.class);
    servicesContainer.addSingleton(ProfilesConsole.class);
    servicesContainer.addSingleton(RulesConsole.class);
    servicesContainer.addSingleton(JRubyI18n.class);
    servicesContainer.addSingleton(DefaultUserFinder.class);
    servicesContainer.addSingleton(I18nManager.class);
    servicesContainer.addSingleton(RuleI18nManager.class);
    servicesContainer.addSingleton(GwtI18n.class);
    servicesContainer.addSingleton(ResourceTypes.class);

    // Notifications
    servicesContainer.addSingleton(NotificationService.class);
    servicesContainer.addSingleton(DefaultNotificationManager.class);
    servicesContainer.addSingleton(ReviewsNotificationManager.class);

    servicesContainer.startComponents();
  }

  private void executeStartupTasks() {
    ComponentContainer startupContainer = servicesContainer.createChild();
    startupContainer.addSingleton(GwtPublisher.class);
    startupContainer.addSingleton(RegisterMetrics.class);
    startupContainer.addSingleton(RegisterRules.class);
    startupContainer.addSingleton(RegisterProvidedProfiles.class);
    startupContainer.addSingleton(EnableProfiles.class);
    startupContainer.addSingleton(ActivateDefaultProfiles.class);
    startupContainer.addSingleton(JdbcDriverDeployer.class);
    startupContainer.addSingleton(ServerMetadataPersister.class);
    startupContainer.addSingleton(RegisterQualityModels.class);
    startupContainer.addSingleton(DeleteDeprecatedMeasures.class);
    startupContainer.addSingleton(GeneratePluginIndex.class);
    startupContainer.addSingleton(RegisterNewDashboards.class);
    startupContainer.startComponents();

    startupContainer.getComponentByType(ServerLifecycleNotifier.class).notifyStart();

    // Do not put the following statements in a finally block.
    // It would hide the possible exception raised during startup
    // See SONAR-3107
    startupContainer.stopComponents();
    servicesContainer.removeChild();
    servicesContainer.getComponentByType(DatabaseSessionFactory.class).clear();
  }

  public void stop() {
    if (rootContainer != null) {
      try {
        TimeProfiler profiler = new TimeProfiler().start("Stop sonar");
        rootContainer.stopComponents();
        rootContainer = null;
        connected = false;
        started = false;
        profiler.stop();
      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).debug("Fail to stop Sonar - ignored", e);
      }
    }
  }

  public ComponentContainer getContainer() {
    if (servicesContainer != null) {
      return servicesContainer;
    }
    if (coreContainer != null) {
      return coreContainer;
    }
    return rootContainer;
  }

  public Object getComponent(Object key) {
    return getContainer().getComponentByKey(key);
  }

  /**
   * shortcut for ruby code
   */
  public static Server getServer() {
    return (Server) getInstance().getComponent(Server.class);
  }
}
