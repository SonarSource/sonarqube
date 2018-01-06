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

import org.sonar.server.platform.ws.SafeModeHealthActionModule;
import org.sonar.server.authentication.SafeModeUserSession;
import org.sonar.server.organization.NoopDefaultOrganizationCache;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.db.migration.AutoDbMigration;
import org.sonar.server.platform.db.migration.DatabaseMigrationImpl;
import org.sonar.server.platform.db.migration.MigrationEngineModule;
import org.sonar.server.platform.db.migration.NoopDatabaseMigrationImpl;
import org.sonar.server.platform.web.WebPagesFilter;
import org.sonar.server.platform.ws.DbMigrationStatusAction;
import org.sonar.server.platform.ws.IndexAction;
import org.sonar.server.platform.ws.L10nWs;
import org.sonar.server.platform.ws.MigrateDbAction;
import org.sonar.server.platform.ws.StatusAction;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.ws.WebServiceEngine;
import org.sonar.server.ws.WebServiceFilter;
import org.sonar.server.ws.ws.WebServicesWsModule;

public class PlatformLevelSafeMode extends PlatformLevel {
  public PlatformLevelSafeMode(PlatformLevel parent) {
    super("Safemode", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      ServerImpl.class,
      WebPagesFilter.class,

      // l10n WS
      L10nWs.class,
      IndexAction.class,

      // Server WS
      StatusAction.class,
      MigrateDbAction.class,
      DbMigrationStatusAction.class,
      SafeModeHealthActionModule.class,
      SystemWs.class,

      // Listing WS
      WebServicesWsModule.class,

      // WS engine
      SafeModeUserSession.class,
      WebServiceEngine.class,
      WebServiceFilter.class,

      NoopDefaultOrganizationCache.class);
    addIfStartupLeader(
      DatabaseMigrationImpl.class,
      MigrationEngineModule.class,
      AutoDbMigration.class)
        .otherwiseAdd(NoopDatabaseMigrationImpl.class);
  }
}
