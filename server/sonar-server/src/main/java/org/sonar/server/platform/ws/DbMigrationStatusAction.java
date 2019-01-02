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
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.NO_CONNECTION_TO_DB;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.UNSUPPORTED_DATABASE_MIGRATION_STATUS;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.statusDescription;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.write;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.writeMigrationRequiredResponse;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.writeNotSupportedResponse;

/**
 * Implementation of the {@code db_migration_status} action for the System WebService.
 */
public class DbMigrationStatusAction implements SystemWsAction {

  private final DatabaseVersion databaseVersion;
  private final DatabaseMigrationState databaseMigrationState;
  private final Database database;

  public DbMigrationStatusAction(DatabaseVersion databaseVersion, Database database, DatabaseMigrationState databaseMigrationState) {
    this.databaseVersion = databaseVersion;
    this.database = database;
    this.databaseMigrationState = databaseMigrationState;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("db_migration_status")
      .setDescription("Display the database migration status of SonarQube." +
        "<br/>" +
        statusDescription())
      .setSince("5.2")
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-migrate_db.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Optional<Long> currentVersion = databaseVersion.getVersion();
    checkState(currentVersion.isPresent(), NO_CONNECTION_TO_DB);

    JsonWriter json = response.newJsonWriter();
    try {
      DatabaseVersion.Status status = databaseVersion.getStatus();
      if (status == DatabaseVersion.Status.UP_TO_DATE || status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
        write(json, databaseMigrationState);
      } else if (!database.getDialect().supportsMigration()) {
        writeNotSupportedResponse(json);
      } else {
        switch (databaseMigrationState.getStatus()) {
          case RUNNING:
          case FAILED:
          case SUCCEEDED:
            write(json, databaseMigrationState);
            break;
          case NONE:
            writeMigrationRequiredResponse(json);
            break;
          default:
            throw new IllegalArgumentException(UNSUPPORTED_DATABASE_MIGRATION_STATUS);
        }
      }
    } finally {
      json.close();
    }
  }
}
