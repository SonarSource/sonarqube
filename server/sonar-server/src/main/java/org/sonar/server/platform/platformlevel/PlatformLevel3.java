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

import org.sonar.api.utils.UriReader;
import org.sonar.core.extension.CoreExtensionsInstaller;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.server.async.AsyncExecutionModule;
import org.sonar.server.organization.DefaultOrganizationProviderImpl;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.StartupMetadataPersister;
import org.sonar.server.platform.WebCoreExtensionsInstaller;
import org.sonar.server.platform.db.migration.NoopDatabaseMigrationImpl;
import org.sonar.server.platform.serverid.ServerIdModule;
import org.sonar.server.setting.DatabaseSettingLoader;
import org.sonar.server.setting.DatabaseSettingsEnabler;

import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel;

public class PlatformLevel3 extends PlatformLevel {
  public PlatformLevel3(PlatformLevel parent) {
    super("level3", parent);
  }

  @Override
  protected void configureLevel() {
    addIfStartupLeader(StartupMetadataPersister.class);
    add(
      NoopDatabaseMigrationImpl.class,
      ServerIdModule.class,
      ServerImpl.class,
      DatabaseSettingLoader.class,
      DatabaseSettingsEnabler.class,
      UriReader.class,
      DefaultHttpDownloader.class,
      DefaultOrganizationProviderImpl.class,
      OrganizationFlagsImpl.class,
      AsyncExecutionModule.class);
  }

  @Override
  public PlatformLevel start() {
    ComponentContainer container = getContainer();
    CoreExtensionsInstaller coreExtensionsInstaller = get(WebCoreExtensionsInstaller.class);
    coreExtensionsInstaller.install(container, hasPlatformLevel(3), noAdditionalSideFilter());

    return super.start();
  }
}
