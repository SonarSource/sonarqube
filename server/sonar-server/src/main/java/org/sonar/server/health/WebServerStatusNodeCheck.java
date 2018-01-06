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
package org.sonar.server.health;

import java.util.EnumSet;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;

import static org.sonar.server.health.Health.newHealthCheckBuilder;

/**
 * Checks the running status of the WebServer when it is not anymore in safemode.
 */
public class WebServerStatusNodeCheck implements NodeHealthCheck {
  private static final EnumSet<DatabaseMigrationState.Status> VALID_DATABASEMIGRATION_STATUSES = EnumSet.of(
    DatabaseMigrationState.Status.NONE, DatabaseMigrationState.Status.SUCCEEDED);

  private final DatabaseMigrationState migrationState;
  private final Platform platform;
  private final RestartFlagHolder restartFlagHolder;

  public WebServerStatusNodeCheck(DatabaseMigrationState migrationState, Platform platform, RestartFlagHolder restartFlagHolder) {
    this.migrationState = migrationState;
    this.platform = platform;
    this.restartFlagHolder = restartFlagHolder;
  }

  @Override
  public Health check() {
    Platform.Status platformStatus = platform.status();
    if (platformStatus == Platform.Status.UP
      && VALID_DATABASEMIGRATION_STATUSES.contains(migrationState.getStatus())
      && !restartFlagHolder.isRestarting()) {
      return Health.GREEN;
    }
    return newHealthCheckBuilder()
      .setStatus(Health.Status.RED)
      .addCause("SonarQube webserver is not up")
      .build();
  }
}
