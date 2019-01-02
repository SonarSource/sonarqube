/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.platformlevel;

import org.sonar.api.utils.Durations;
import org.sonar.core.extension.CoreExtensionsInstaller;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginLoader;
import org.sonar.server.es.MigrationEsClientImpl;
import org.sonar.server.l18n.ServerI18n;
import org.sonar.server.platform.DatabaseServerCompatibility;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.StartupMetadataProvider;
import org.sonar.server.platform.WebCoreExtensionsInstaller;
import org.sonar.server.platform.db.CheckDatabaseCharsetAtStartup;
import org.sonar.server.platform.db.migration.DatabaseMigrationExecutorServiceImpl;
import org.sonar.server.platform.db.migration.DatabaseMigrationStateImpl;
import org.sonar.server.platform.db.migration.MigrationConfigurationModule;
import org.sonar.server.platform.db.migration.charset.DatabaseCharsetChecker;
import org.sonar.server.platform.db.migration.history.MigrationHistoryTable;
import org.sonar.server.platform.db.migration.history.MigrationHistoryTableImpl;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.platform.web.WebPagesCache;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.PluginFileSystem;
import org.sonar.server.plugins.ServerPluginJarExploder;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.WebServerExtensionInstaller;
import org.sonar.server.startup.ClusterConfigurationCheck;

import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel;

public class PlatformLevel2 extends PlatformLevel {
  public PlatformLevel2(PlatformLevel parent) {
    super("level2", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      MigrationConfigurationModule.class,
      DatabaseVersion.class,
      DatabaseServerCompatibility.class,
      MigrationEsClientImpl.class,

      new StartupMetadataProvider(),
      DefaultServerUpgradeStatus.class,
      Durations.class,

      // index.html cache
      WebPagesCache.class,

      // plugins
      ServerPluginRepository.class,
      ServerPluginJarExploder.class,
      PluginLoader.class,
      PluginFileSystem.class,
      PluginClassloaderFactory.class,
      InstalledPluginReferentialFactory.class,
      WebServerExtensionInstaller.class,

      // depends on plugins
      ServerI18n.class,
      RuleI18nManager.class,

      OfficialDistribution.class);

    // Migration state must be kept at level2 to survive moving in and then out of safe mode
    // ExecutorService must be kept at level2 because stopping it when stopping safe mode level causes error making SQ fail
    add(
      MigrationHistoryTableImpl.class,
      DatabaseMigrationStateImpl.class,
      DatabaseMigrationExecutorServiceImpl.class);

    addIfCluster(
      ClusterConfigurationCheck.class);

    addIfStartupLeader(
      DatabaseCharsetChecker.class,
      CheckDatabaseCharsetAtStartup.class);
  }

  @Override
  public PlatformLevel start() {
    // ensuring the HistoryTable exists must be the first thing done when this level is started
    getOptional(MigrationHistoryTable.class).ifPresent(MigrationHistoryTable::start);

    ComponentContainer container = getContainer();
    CoreExtensionsInstaller coreExtensionsInstaller = get(WebCoreExtensionsInstaller.class);
    coreExtensionsInstaller.install(container, hasPlatformLevel(2), noAdditionalSideFilter());

    return super.start();
  }
}
