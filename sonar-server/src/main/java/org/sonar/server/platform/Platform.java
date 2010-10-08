/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugins;
import org.sonar.api.database.configuration.DatabaseConfiguration;
import org.sonar.api.platform.Environment;
import org.sonar.api.platform.Server;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.resources.Languages;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.IocContainer;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.jpa.dao.*;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;
import org.sonar.server.charts.ChartFactory;
import org.sonar.server.configuration.Backup;
import org.sonar.server.configuration.ConfigurationLogger;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.database.EmbeddedDatabaseFactory;
import org.sonar.server.database.JndiDatabaseConnector;
import org.sonar.server.filters.FilterExecutor;
import org.sonar.server.mavendeployer.MavenRepository;
import org.sonar.server.plugins.*;
import org.sonar.server.qualitymodel.DefaultModelManager;
import org.sonar.server.rules.*;
import org.sonar.server.startup.*;
import org.sonar.server.ui.AuthenticatorFactory;
import org.sonar.server.ui.CodeColorizers;
import org.sonar.server.ui.Views;

/**
 * @since 2.2
 */
public final class Platform {

  private static final Platform INSTANCE = new Platform();

  private MutablePicoContainer rootContainer;//level 1 : only database connectors
  private MutablePicoContainer coreContainer;//level 2 : level 1 + core components
  private MutablePicoContainer servicesContainer;//level 3 : level 2 + plugin extensions + core components that depend on plugin extensions

  private boolean connected = false;
  private boolean started = false;

  public static Platform getInstance() {
    return INSTANCE;
  }

  private Platform() {
  }

  public void init(Configuration conf) {
    if (!connected) {
      try {
        startDatabaseConnectors(conf);
        connected = true;

      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).error("Can not start Sonar", e);
      }
    }
  }

  public void start() {
    if (!started && isConnectedToDatabase()) {
      TimeProfiler profiler = new TimeProfiler().start("Start services");
      startCoreComponents();
      startServiceComponents();
      executeStartupTasks();
      started = true;
      profiler.stop();
    }
  }


  private void startDatabaseConnectors(Configuration configuration) {
    rootContainer = IocContainer.buildPicoContainer();
    ConfigurationLogger.log(configuration);

    rootContainer.as(Characteristics.CACHE).addComponent(configuration);
    rootContainer.as(Characteristics.CACHE).addComponent(EmbeddedDatabaseFactory.class);
    rootContainer.as(Characteristics.CACHE).addComponent(JndiDatabaseConnector.class);
    rootContainer.start();

    // Platform is already starting, so it's registered after the container startup
  }

  private boolean isConnectedToDatabase() {
    JndiDatabaseConnector databaseConnector = getContainer().getComponent(JndiDatabaseConnector.class);
    return databaseConnector.isOperational();
  }

  private void startCoreComponents() {
    coreContainer = rootContainer.makeChildContainer();
    coreContainer.as(Characteristics.CACHE).addComponent(Environment.SERVER);
    coreContainer.as(Characteristics.CACHE).addComponent(PluginClassLoaders.class);
    coreContainer.as(Characteristics.CACHE).addComponent(PluginDeployer.class);
    coreContainer.as(Characteristics.CACHE).addComponent(ServerImpl.class);
    coreContainer.as(Characteristics.CACHE).addComponent(DefaultServerFileSystem.class);
    coreContainer.as(Characteristics.CACHE).addComponent(JpaPluginDao.class);
    coreContainer.as(Characteristics.CACHE).addComponent(ServerPluginRepository.class);
    coreContainer.as(Characteristics.CACHE).addComponent(ThreadLocalDatabaseSessionFactory.class);
    coreContainer.as(Characteristics.CACHE).addComponent(HttpDownloader.class);
    coreContainer.as(Characteristics.CACHE).addComponent(UpdateCenterClient.class);
    coreContainer.as(Characteristics.CACHE).addComponent(UpdateFinderFactory.class);
    coreContainer.as(Characteristics.CACHE).addComponent(PluginDownloader.class);
    coreContainer.as(Characteristics.NO_CACHE).addComponent(FilterExecutor.class);
    coreContainer.as(Characteristics.NO_CACHE).addAdapter(new DatabaseSessionProvider());
    coreContainer.start();

    DatabaseConfiguration dbConfiguration = new DatabaseConfiguration(coreContainer.getComponent(DatabaseSessionFactory.class));
    coreContainer.getComponent(CompositeConfiguration.class).addConfiguration(dbConfiguration);
  }

  /**
   * plugin extensions + all the components that depend on plugin extensions
   */
  private void startServiceComponents() {
    servicesContainer = coreContainer.makeChildContainer();

    ServerPluginRepository pluginRepository = servicesContainer.getComponent(ServerPluginRepository.class);
    pluginRepository.registerPlugins(servicesContainer);

    servicesContainer.as(Characteristics.CACHE).addComponent(DefaultModelFinder.class); // depends on plugins
    servicesContainer.as(Characteristics.CACHE).addComponent(DefaultModelManager.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(Plugins.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(ChartFactory.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(Languages.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(Views.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(CodeColorizers.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(RulesDao.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(org.sonar.api.database.daos.RulesDao.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(MeasuresDao.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(org.sonar.api.database.daos.MeasuresDao.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(ProfilesDao.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(AsyncMeasuresDao.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(DaoFacade.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(DefaultRulesManager.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(ProfilesManager.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(AsyncMeasuresService.class);
    servicesContainer.as(Characteristics.NO_CACHE).addComponent(Backup.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(AuthenticatorFactory.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(ServerLifecycleNotifier.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(AnnotationProfileParser.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(XMLProfileParser.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(XMLProfileSerializer.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(XMLRuleParser.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(DefaultRuleFinder.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(DeprecatedRuleRepositories.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(DeprecatedProfiles.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(DeprecatedProfileExporters.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(DeprecatedProfileImporters.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(ProfilesConsole.class);
    servicesContainer.as(Characteristics.CACHE).addComponent(RulesConsole.class);

    servicesContainer.start();
  }

  private void executeStartupTasks() {
    MutablePicoContainer startupContainer = servicesContainer.makeChildContainer();
    try {
      startupContainer.as(Characteristics.CACHE).addComponent(MavenRepository.class);
      startupContainer.as(Characteristics.CACHE).addComponent(GwtPublisher.class);
      startupContainer.as(Characteristics.CACHE).addComponent(RegisterMetrics.class);
      startupContainer.as(Characteristics.CACHE).addComponent(RegisterRules.class);
      startupContainer.as(Characteristics.CACHE).addComponent(RegisterProvidedProfiles.class);
      startupContainer.as(Characteristics.CACHE).addComponent(ActivateDefaultProfiles.class);
      startupContainer.as(Characteristics.CACHE).addComponent(JdbcDriverDeployer.class);
      startupContainer.as(Characteristics.CACHE).addComponent(ServerMetadataPersister.class);
      startupContainer.as(Characteristics.CACHE).addComponent(RegisterQualityModels.class);
      startupContainer.start();

      startupContainer.getComponent(ServerLifecycleNotifier.class).notifyStart();

    } finally {
      startupContainer.stop();
      servicesContainer.removeChildContainer(startupContainer);
      startupContainer = null;
      servicesContainer.getComponent(DatabaseSessionFactory.class).clear();
    }
  }

  public void stop() {
    if (rootContainer != null) {
      try {
        TimeProfiler profiler = new TimeProfiler().start("Stop sonar");
        rootContainer.stop();
        rootContainer = null;
        connected = false;
        started = false;
        profiler.stop();
      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).debug("Fail to stop Sonar - ignored", e);
      }
    }
  }

  public MutablePicoContainer getContainer() {
    if (servicesContainer != null) {
      return servicesContainer;
    }
    if (coreContainer != null) {
      return coreContainer;
    }
    return rootContainer;
  }

  public Object getComponent(Object key) {
    return getContainer().getComponent(key);
  }

  /**
   * shortcut for ruby code
   */
  public static Server getServer() {
    return (Server) getInstance().getComponent(Server.class);
  }
}
