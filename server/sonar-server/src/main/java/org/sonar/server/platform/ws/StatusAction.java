/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;

/**
 * Implementation of the {@code status} action for the System WebService.
 */
public class StatusAction implements SystemWsAction {

  private final Server server;
  private final DatabaseMigrationState migrationState;
  private final Platform platform;
  private final RestartFlagHolder restartFlagHolder;

  public StatusAction(Server server, DatabaseMigrationState migrationState,
    Platform platform, RestartFlagHolder restartFlagHolder) {
    this.server = server;
    this.migrationState = migrationState;
    this.platform = platform;
    this.restartFlagHolder = restartFlagHolder;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("status")
      .setDescription("Get state information about SonarQube." +
        "<p>status: the running status" +
        " <ul>" +
        " <li>STARTING: SonarQube Web Server is up and serving some Web Services (eg. api/system/status) " +
        "but initialization is still ongoing</li>" +
        " <li>UP: SonarQube instance is up and running</li>" +
        " <li>DOWN: SonarQube instance is up but not running because " +
        "migration has failed (refer to WS /api/system/migrate_db for details) or some other reason (check logs).</li>" +
        " <li>RESTARTING: SonarQube instance is still up but a restart has been requested " +
        "(refer to WS /api/system/restart for details).</li>" +
        " <li>DB_MIGRATION_NEEDED: database migration is required. DB migration can be started using WS /api/system/migrate_db.</li>" +
        " <li>DB_MIGRATION_RUNNING: DB migration is running (refer to WS /api/system/migrate_db for details)</li>" +
        " </ul>" +
        "</p>")
      .setSince("5.2")
      .setResponseExample(Resources.getResource(this.getClass(), "example-status.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter();
    writeJson(json);
    json.close();
  }

  private void writeJson(JsonWriter json) {
    Status status = computeStatus();

    json.beginObject();
    json.prop("id", server.getId());
    json.prop("version", server.getVersion());
    json.prop("status", status.toString());
    json.endObject();
  }

  private Status computeStatus() {
    Platform.Status platformStatus = platform.status();
    switch (platformStatus) {
      case BOOTING:
        // can not happen since there can not even exist an instance of the current class
        // unless the Platform's status is UP/SAFEMODE/STARTING
        return Status.DOWN;
      case UP:
        return restartFlagHolder.isRestarting() ? Status.RESTARTING : Status.UP;
      case STARTING:
        return computeStatusInStarting();
      case SAFEMODE:
        return computeStatusInSafemode();
      default:
        throw new IllegalArgumentException("Unsupported Platform.Status " + platformStatus);
    }
  }

  private Status computeStatusInStarting() {
    DatabaseMigrationState.Status databaseMigrationStatus = migrationState.getStatus();
    switch (databaseMigrationStatus) {
      case NONE:
        return Status.STARTING;
      case RUNNING:
        return Status.DB_MIGRATION_RUNNING;
      case FAILED:
        return Status.DOWN;
      case SUCCEEDED:
        // DB migration can be finished while we haven't yet finished SQ's initialization
        return Status.STARTING;
      default:
        throw new IllegalArgumentException("Unsupported DatabaseMigration.Status " + databaseMigrationStatus);
    }
  }

  private Status computeStatusInSafemode() {
    DatabaseMigrationState.Status databaseMigrationStatus = migrationState.getStatus();
    switch (databaseMigrationStatus) {
      case NONE:
        return Status.DB_MIGRATION_NEEDED;
      case RUNNING:
        return Status.DB_MIGRATION_RUNNING;
      case FAILED:
        return Status.DOWN;
      case SUCCEEDED:
        return Status.STARTING;
      default:
        throw new IllegalArgumentException("Unsupported DatabaseMigration.Status " + databaseMigrationStatus);
    }
  }

  private enum Status {
    UP, DOWN, DB_MIGRATION_NEEDED, DB_MIGRATION_RUNNING, STARTING, RESTARTING
  }

}
