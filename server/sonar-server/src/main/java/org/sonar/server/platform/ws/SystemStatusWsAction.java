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
package org.sonar.server.platform.ws;

import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.IsAliveMapper;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.platform.Platform;

import com.google.common.io.Resources;

/**
 * Implementation of the {@code status} action for the System WebService.
 */
public class SystemStatusWsAction implements SystemWsAction {

  private static final Logger LOGGER = Loggers.get(SystemStatusWsAction.class);

  private final Server server;
  private final DatabaseMigration databaseMigration;
  private final Platform platform;
  private final DbClient dbClient;

  public SystemStatusWsAction(Server server, DatabaseMigration databaseMigration, Platform platform, DbClient dbClient) {
    this.server = server;
    this.databaseMigration = databaseMigration;
    this.platform = platform;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("status")
      .setDescription("Get the server status:" +
        "<ul>" +
        "<li>UP: SonarQube instance is up and running</li>" +
        "<li>DOWN: SonarQube instance is up but not running because SQ can not connect to database or " +
        "migration has failed (refer to WS /api/system/migrate_db for details) or some other reason (check logs).</li>" +
        "<li>DB_MIGRATION_NEEDED: database migration is required. DB migration can be started using WS /api/system/migrate_db.</li>" +
        "<li>DB_MIGRATION_RUNNING: DB migration is running (refer to WS /api/system/migrate_db for details)</li>" +
        "</ul>")
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
    if (!isConnectedToDB()) {
      return Status.DOWN;
    }

    Platform.Status platformStatus = platform.status();
    switch (platformStatus) {
      case BOOTING:
        // can not happen since there can not even exist an instance of the current class
        // unless the Platform's status is UP or SAFEMODE
        return Status.DOWN;
      case UP:
        return Status.UP;
      case SAFEMODE:
        return computeFromDbMigrationStatus();
      default:
        throw new IllegalArgumentException("Unsupported Platform.Status " + platformStatus);
    }
  }

  private boolean isConnectedToDB() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbSession.getMapper(IsAliveMapper.class).isAlive() == IsAliveMapper.IS_ALIVE_RETURNED_VALUE;
    } catch (RuntimeException e) {
      LOGGER.error("DB connection is down", e);
      return false;
    }
  }

  private Status computeFromDbMigrationStatus() {
    DatabaseMigration.Status databaseMigrationStatus = databaseMigration.status();
    switch (databaseMigrationStatus) {
      case NONE:
        return Status.DB_MIGRATION_NEEDED;
      case RUNNING:
        return Status.DB_MIGRATION_RUNNING;
      case FAILED:
        return Status.DOWN;
      case SUCCEEDED:
        // status of Platform is supposed to be UP _before_ DatabaseMigration status becomes UP too
        // so, in theory, this case can not happen
        return Status.UP;
      default:
        throw new IllegalArgumentException("Unsupported DatabaseMigration.Status " + databaseMigrationStatus);
    }
  }

  private enum Status {
    UP, DOWN, DB_MIGRATION_NEEDED, DB_MIGRATION_RUNNING
  }

}
