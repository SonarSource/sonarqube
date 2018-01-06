/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.server.async.AsyncExecutionModule;
import org.sonar.server.organization.DefaultOrganizationProviderImpl;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.platform.ServerIdManager;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.StartupMetadataPersister;
import org.sonar.server.platform.db.migration.NoopDatabaseMigrationImpl;
import org.sonar.server.setting.DatabaseSettingLoader;
import org.sonar.server.setting.DatabaseSettingsEnabler;

public class PlatformLevel3 extends PlatformLevel {
  public PlatformLevel3(PlatformLevel parent) {
    super("level3", parent);
  }

  @Override
  protected void configureLevel() {
    addIfStartupLeader(StartupMetadataPersister.class);
    add(
      NoopDatabaseMigrationImpl.class,
      ServerIdManager.class,
      ServerImpl.class,
      DatabaseSettingLoader.class,
      DatabaseSettingsEnabler.class,
      UriReader.class,
      DefaultHttpDownloader.class,
      DefaultOrganizationProviderImpl.class,
      OrganizationFlagsImpl.class,
      AsyncExecutionModule.class);
  }
}
