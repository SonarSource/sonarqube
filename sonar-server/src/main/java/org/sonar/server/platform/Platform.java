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
package org.sonar.server.platform;

import org.apache.commons.configuration.BaseConfiguration;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.internal.TempFolderCleaner;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.DefaultDatabase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.PreviewDatabaseFactory;
import org.sonar.core.persistence.SemaphoreUpdater;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;
import org.sonar.server.db.EmbeddedDatabaseFactory;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.DatabaseMigrations;
import org.sonar.server.db.migrations.DatabaseMigrator;
import org.sonar.server.es.ESNode;
import org.sonar.server.platform.ws.PlatformWs;
import org.sonar.server.platform.ws.RestartHandler;
import org.sonar.server.plugins.ApplicationDeployer;
import org.sonar.server.plugins.DefaultServerPluginRepository;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.PluginDeployer;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.ServerPluginInstaller;
import org.sonar.server.startup.ServerMetadataPersister;
import org.sonar.server.ui.JRubyI18n;
import org.sonar.server.ui.JRubyProfiling;

import javax.annotation.CheckForNull;
import javax.servlet.ServletContext;

/**
 * @since 2.2
 */
public class Platform {

  private static final Platform INSTANCE = new Platform();

  // level 1 : database stuff, settings
  // level 2 : level 1 + components that are never restarted
  // level 3 : level 2 + all other components, including plugin extensions
  private ComponentContainer level1Container, level2Container, level3Container;

  private boolean connected = false;
  private boolean started = false;

  private Platform() {
  }

  public static Platform getInstance() {
    return INSTANCE;
  }

  /**
   * shortcut for ruby code
   */
  public static Server getServer() {
    return (Server) getInstance().getComponent(Server.class);
  }

  /**
   * Used by ruby code
   */
  @CheckForNull
  public static <T> T component(Class<T> type) {
    if (INSTANCE.started) {
      return INSTANCE.getContainer().getComponentByType(type);
    }
    return null;
  }

  public void init(ServletContext servletContext) {
    if (!connected) {
      startLevel1Container(servletContext);
      connected = true;
    }
  }

  // Platform is injected in Pico, so do not rename "start"
  public void doStart() {
    if (!started && getDatabaseStatus() == DatabaseVersion.Status.UP_TO_DATE) {
      TimeProfiler profiler = new TimeProfiler().start("Start components");
      startLevel2Container();
      started = true;
      profiler.stop();
    }
  }

  private void startLevel1Container(ServletContext servletContext) {
    level1Container = new ComponentContainer();
    level1Container.addSingleton(this);
    level1Container.addSingleton(servletContext);
    level1Container.addSingleton(new BaseConfiguration());
    level1Container.addSingleton(ServerSettings.class);
    level1Container.addSingleton(ServerImpl.class);
    level1Container.addSingleton(Logback.class);
    level1Container.addSingleton(Profiling.class);
    level1Container.addSingleton(JRubyProfiling.class);
    level1Container.addSingleton(EmbeddedDatabaseFactory.class);
    level1Container.addSingleton(DefaultDatabase.class);
    level1Container.addSingleton(MyBatis.class);
    level1Container.addSingleton(DefaultServerUpgradeStatus.class);
    level1Container.addSingleton(DatabaseServerCompatibility.class);
    for (Class<? extends DatabaseMigration> migrationClass : DatabaseMigrations.CLASSES) {
      level1Container.addSingleton(migrationClass);
    }
    level1Container.addSingleton(DatabaseMigrator.class);
    level1Container.addSingleton(DatabaseVersion.class);
    for (Class daoClass : DaoUtils.getDaoClasses()) {
      level1Container.addSingleton(daoClass);
    }
    level1Container.addSingleton(PurgeProfiler.class);
    level1Container.addSingleton(PluginDeployer.class);
    level1Container.addSingleton(ServerPluginInstaller.class);
    level1Container.addSingleton(InstalledPluginReferentialFactory.class);
    level1Container.addSingleton(DefaultServerPluginRepository.class);
    level1Container.addSingleton(DefaultServerFileSystem.class);
    level1Container.addSingleton(ApplicationDeployer.class);
    level1Container.addSingleton(JRubyI18n.class);
    level1Container.addSingleton(DefaultI18n.class);
    level1Container.addSingleton(RuleI18nManager.class);
    level1Container.addSingleton(GwtI18n.class);
    level1Container.addSingleton(Durations.class);
    level1Container.addSingleton(PreviewDatabaseFactory.class);
    level1Container.addSingleton(SemaphoreUpdater.class);
    level1Container.addSingleton(SemaphoresImpl.class);
    level1Container.addPicoAdapter(new TempFolderProvider());
    level1Container.addSingleton(TempFolderCleaner.class);
    level1Container.startComponents();
  }

  private DatabaseVersion.Status getDatabaseStatus() {
    DatabaseVersion version = getContainer().getComponentByType(DatabaseVersion.class);
    return version.getStatus();
  }

  /**
   * Components listed here are never restarted
   */
  private void startLevel2Container() {
    level2Container = level1Container.createChild();
    level2Container.addSingleton(PersistentSettings.class);
    level2Container.addSingleton(DefaultDatabaseConnector.class);
    level2Container.addSingleton(ServerExtensionInstaller.class);
    level2Container.addSingleton(ThreadLocalDatabaseSessionFactory.class);
    level2Container.addPicoAdapter(new DatabaseSessionProvider());
    level2Container.addSingleton(ServerMetadataPersister.class);
    level2Container.addSingleton(ESNode.class);
    level2Container.addSingleton(HttpDownloader.class);
    level2Container.addSingleton(UriReader.class);
    level2Container.addSingleton(ServerIdGenerator.class);

    // ws
    level2Container.addSingleton(RestartHandler.class);
    level2Container.addSingleton(PlatformWs.class);

    level2Container.startComponents();

    restartLevel3Container();
  }

  // Do not rename "stop"
  public void doStop() {
    if (level1Container != null) {
      try {
        level1Container.stopComponents();
        level1Container = null;
        connected = false;
        started = false;
      } catch (Exception e) {
        LoggerFactory.getLogger(getClass()).debug("Fail to stop server - ignored", e);
      }
    }
  }

  public void restartLevel3Container() {
    if (level3Container != null) {
      level3Container.stopComponents();
    }
    level3Container = level2Container.createChild();
    new ServerComponentsStarter().start(level3Container);
  }

  public ComponentContainer getContainer() {
    if (level3Container != null) {
      return level3Container;
    }
    if (level2Container != null) {
      return level2Container;
    }
    return level1Container;
  }

  public Object getComponent(Object key) {
    return getContainer().getComponentByKey(key);
  }
}
