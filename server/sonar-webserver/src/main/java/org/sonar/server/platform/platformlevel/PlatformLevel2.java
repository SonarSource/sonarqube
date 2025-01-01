/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.SpringComponentContainer;
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
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.platform.web.WebPagesCache;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.PluginJarLoader;
import org.sonar.server.plugins.ServerPluginJarExploder;
import org.sonar.server.plugins.ServerPluginManager;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.WebServerExtensionInstaller;
import org.sonar.server.telemetry.TelemetryDbMigrationStepDurationProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationStepsProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationSuccessProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationTotalTimeProvider;

import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel;

public class PlatformLevel2 extends PlatformLevel {
  public PlatformLevel2(PlatformLevel parent) {
    super("level2", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      new MigrationConfigurationModule(),
      MigrationEsClientImpl.class,
      DatabaseVersion.class,
      DatabaseServerCompatibility.class,

      new StartupMetadataProvider(),
      DefaultServerUpgradeStatus.class,
      Durations.class,

      // index.html cache
      WebPagesCache.class,

      // plugins
      PluginJarLoader.class,
      ServerPluginRepository.class,
      ServerPluginManager.class,
      ServerPluginJarExploder.class,
      PluginClassLoader.class,
      PluginClassloaderFactory.class,
      InstalledPluginReferentialFactory.class,
      WebServerExtensionInstaller.class,

      // depends on plugins
      ServerI18n.class,

      OfficialDistribution.class);

    // Migration state must be kept at level2 to survive moving in and then out of safe mode
    // ExecutorService must be kept at level2 because stopping it when stopping safe mode level causes error making SQ fail
    add(
      TelemetryDbMigrationTotalTimeProvider.class,
      TelemetryDbMigrationStepsProvider.class,
      TelemetryDbMigrationSuccessProvider.class,
      TelemetryDbMigrationStepDurationProvider.class,
      DatabaseMigrationStateImpl.class,
      DatabaseMigrationExecutorServiceImpl.class);

    addIfStartupLeader(
      DatabaseCharsetChecker.class,
      CheckDatabaseCharsetAtStartup.class);
  }

  @Override
  public PlatformLevel start() {
    SpringComponentContainer container = getContainer();
    CoreExtensionsInstaller coreExtensionsInstaller = parent.get(WebCoreExtensionsInstaller.class);
    coreExtensionsInstaller.install(container, hasPlatformLevel(2), noAdditionalSideFilter());

    return super.start();
  }
}
