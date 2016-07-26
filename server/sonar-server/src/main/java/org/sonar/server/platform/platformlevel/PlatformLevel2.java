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
package org.sonar.server.platform.platformlevel;

import org.sonar.api.utils.Durations;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginLoader;
import org.sonar.db.charset.DatabaseCharsetChecker;
import org.sonar.db.version.MigrationStepModule;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.StartupMetadataProvider;
import org.sonar.server.platform.db.CheckDatabaseCharsetAtStartup;
import org.sonar.server.platform.db.migrations.DatabaseMigrator;
import org.sonar.server.platform.db.migrations.PlatformDatabaseMigration;
import org.sonar.server.platform.db.migrations.PlatformDatabaseMigrationExecutorServiceImpl;
import org.sonar.server.platform.web.RailsAppsDeployer;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.ServerPluginJarExploder;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.WebServerExtensionInstaller;
import org.sonar.server.ruby.PlatformRubyBridge;
import org.sonar.server.ui.JRubyI18n;

public class PlatformLevel2 extends PlatformLevel {
  public PlatformLevel2(PlatformLevel parent) {
    super("level2", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      new StartupMetadataProvider(),
      ServerImpl.class,
      DefaultServerUpgradeStatus.class,

      // depends on Ruby
      PlatformRubyBridge.class,

      // plugins
      ServerPluginRepository.class,
      ServerPluginJarExploder.class,
      PluginLoader.class,
      PluginClassloaderFactory.class,
      InstalledPluginReferentialFactory.class,
      WebServerExtensionInstaller.class,

      // depends on plugins
      RailsAppsDeployer.class,
      JRubyI18n.class,
      DefaultI18n.class,
      RuleI18nManager.class,
      Durations.class,

      // DB migration
      PlatformDatabaseMigrationExecutorServiceImpl.class,
      PlatformDatabaseMigration.class,
      DatabaseMigrator.class,
      MigrationStepModule.class);

    addIfStartupLeader(
      DatabaseCharsetChecker.class,
      CheckDatabaseCharsetAtStartup.class);
  }
}
