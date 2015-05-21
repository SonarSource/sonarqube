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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.server.db.migrations.DatabaseMigration;

import static org.sonar.server.db.migrations.DatabaseMigration.Status.RUNNING;

/**
 * Implementation of the {@code migrate_db} action for the System WebService.
 */
public class MigrateDbSystemAction implements SystemWsAction {

  private static final String UNSUPPORTED_DATABASE_MIGRATION_STATUS = "Unsupported DatabaseMigration status";
  private static final String MESSAGE_STATUS_NONE = "Database is up-to-date, no migration needed.";
  private static final String MESSAGE_STATUS_RUNNING = "Database migration is running.";
  private static final String MESSAGE_STATUS_SUCCEEDED = "Migration succeeded.";
  private static final String MESSAGE_STATUS_FAILED = "Migration failed: %s.<br/> Please check logs.";

  private static final String STATUS_NO_MIGRATION = "NO_MIGRATION";
  private static final String STATUS_NOT_SUPPORTED = "NOT_SUPPORTED";
  private static final String STATUS_MIGRATION_RUNNING = "MIGRATION_RUNNING";
  private static final String STATUS_MIGRATION_FAILED = "MIGRATION_FAILED";
  private static final String STATUS_MIGRATION_SUCCEEDED = "MIGRATION_SUCCEEDED";

  private static final String PROPERTY_STATE = "state";
  private static final String PROPERTY_MESSAGE = "message";
  private static final String PROPERTY_STARTED_AT = "startedAt";

  private final DatabaseVersion databaseVersion;
  private final DatabaseMigration databaseMigration;
  private final Database database;

  public MigrateDbSystemAction(DatabaseVersion databaseVersion,
                               Database database,
                               DatabaseMigration databaseMigration) {
    this.databaseVersion = databaseVersion;
    this.database = database;
    this.databaseMigration = databaseMigration;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("migrate_db")
      .setDescription("Migrate the database to match the current version of SonarQube." +
        "<br/>" +
        "Sending a POST request to this URL starts the DB migration. " +
        "It is strongly advised to <strong>make a database backup</strong> before invoking this WS." +
        "<br/>" +
        "State values are:" +
        "<ul>" +
        "<li>NO_MIGRATION: DB is up to date with current version of SonarQube.</li>" +
        "<li>NOT_SUPPORTED: Migration is not supported on embedded databases.</li>" +
        "<li>MIGRATION_RUNNING: DB migration is under go.</li>" +
        "<li>MIGRATION_SUCCEEDED: DB migration has run and has been successful.</li>" +
        "<li>MIGRATION_FAILED: DB migration has run and failed. SonarQube must be restarted in order to retry a " +
        "DB migration (optionally after DB has been restored from backup).</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "example-migrate_db.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Integer currentVersion = databaseVersion.getVersion();
    if (currentVersion == null) {
      throw new IllegalStateException("Version can not be retrieved from Database. Database is either blank or corrupted");
    }

    if (currentVersion >= DatabaseVersion.LAST_VERSION) {
      writeResponse(response, databaseMigration);
    } else if (!database.getDialect().supportsMigration()) {
      writeNotSupportedResponse(response);
    } else {
      switch (databaseMigration.status()) {
        case RUNNING:
        case FAILED:
        case SUCCEEDED:
          writeResponse(response, databaseMigration);
          break;
        case NONE:
          databaseMigration.startIt();
          writeNoneResponse(response, databaseMigration);
          break;
        default:
          throw new IllegalArgumentException(UNSUPPORTED_DATABASE_MIGRATION_STATUS);
      }
    }
  }

  private void writeNotSupportedResponse(Response response) {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject()
      .prop(PROPERTY_STATE, STATUS_NOT_SUPPORTED)
      .prop(PROPERTY_MESSAGE, "Upgrade is not supported on embedded database.")
      .endObject();
    jsonWriter.close();
  }

  private void writeNoneResponse(Response response, DatabaseMigration databaseMigration) {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject()
        .prop(PROPERTY_STATE, statusToJson(RUNNING))
        .prop(PROPERTY_MESSAGE, MESSAGE_STATUS_RUNNING)
        .propDateTime(PROPERTY_STARTED_AT, databaseMigration.startedAt())
        .endObject();
    jsonWriter.close();
  }

  private void writeResponse(Response response, DatabaseMigration databaseMigration) {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter.beginObject()
      .prop(PROPERTY_STATE, statusToJson(databaseMigration.status()))
      .prop(PROPERTY_MESSAGE, buildMessage(databaseMigration))
      .propDateTime(PROPERTY_STARTED_AT, databaseMigration.startedAt())
      .endObject();
    jsonWriter.close();
  }

  private String statusToJson(DatabaseMigration.Status status) {
    switch (status) {
      case NONE:
        return STATUS_NO_MIGRATION;
      case RUNNING:
        return STATUS_MIGRATION_RUNNING;
      case FAILED:
        return STATUS_MIGRATION_FAILED;
      case SUCCEEDED:
        return STATUS_MIGRATION_SUCCEEDED;
      default:
        throw new IllegalArgumentException(
          "Unsupported DatabaseMigration.Status " + status + " can not be converted to JSON value");
    }
  }

  private static String buildMessage(DatabaseMigration databaseMigration) {
    switch (databaseMigration.status()) {
      case NONE:
        return MESSAGE_STATUS_NONE;
      case RUNNING:
        return MESSAGE_STATUS_RUNNING;
      case SUCCEEDED:
        return MESSAGE_STATUS_SUCCEEDED;
      case FAILED:
        return String.format(MESSAGE_STATUS_FAILED, failureMessage(databaseMigration));
      default:
        return UNSUPPORTED_DATABASE_MIGRATION_STATUS;
    }
  }

  private static String failureMessage(DatabaseMigration databaseMigration) {
    Throwable failureError = databaseMigration.failureError();
    if (failureError == null) {
      return "No failure error";
    }
    return failureError.getMessage();
  }

}
