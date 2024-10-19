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

import org.sonar.server.authentication.SafeModeUserSession;
import org.sonar.server.monitoring.ServerMonitoringMetrics;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.db.migration.AutoDbMigration;
import org.sonar.server.platform.db.migration.DatabaseMigrationImpl;
import org.sonar.server.platform.db.migration.MigrationEngineModule;
import org.sonar.server.platform.db.migration.NoopDatabaseMigrationImpl;
import org.sonar.server.platform.web.WebServiceFilter;
import org.sonar.server.platform.ws.IndexAction;
import org.sonar.server.platform.ws.L10nWs;
import org.sonar.server.platform.ws.SafeModeHealthCheckerModule;
import org.sonar.server.platform.ws.SafemodeSystemWsModule;
import org.sonar.server.ws.WebServiceEngine;
import org.sonar.server.ws.ws.WebServicesWsModule;

public class PlatformLevelSafeMode extends PlatformLevel {
  public PlatformLevelSafeMode(PlatformLevel parent) {
    super("Safemode", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      ServerImpl.class,

      // l10n WS
      L10nWs.class,
      IndexAction.class,

      // Server WS
      new SafeModeHealthCheckerModule(),
      new SafemodeSystemWsModule(),

      // Listing WS
      new WebServicesWsModule(),

      // WS engine
      SafeModeUserSession.class,
      WebServiceEngine.class,
      WebServiceFilter.class,

      // Monitoring
      ServerMonitoringMetrics.class);
    addIfStartupLeader(
      DatabaseMigrationImpl.class,
      new MigrationEngineModule(),
      AutoDbMigration.class)
        .otherwiseAdd(NoopDatabaseMigrationImpl.class);
  }
}
