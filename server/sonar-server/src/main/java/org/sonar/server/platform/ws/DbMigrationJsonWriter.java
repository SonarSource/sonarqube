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
package org.sonar.server.platform.ws;

import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;

import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.RUNNING;

public class DbMigrationJsonWriter {
  static final String FIELD_STATE = "state";
  static final String FIELD_MESSAGE = "message";
  static final String FIELD_STARTED_AT = "startedAt";

  static final String STATUS_NO_MIGRATION = "NO_MIGRATION";
  static final String STATUS_NOT_SUPPORTED = "NOT_SUPPORTED";
  static final String STATUS_MIGRATION_RUNNING = "MIGRATION_RUNNING";
  static final String STATUS_MIGRATION_FAILED = "MIGRATION_FAILED";
  static final String STATUS_MIGRATION_SUCCEEDED = "MIGRATION_SUCCEEDED";
  static final String STATUS_MIGRATION_REQUIRED = "MIGRATION_REQUIRED";

  static final String NO_CONNECTION_TO_DB = "Cannot connect to Database.";
  static final String UNSUPPORTED_DATABASE_MIGRATION_STATUS = "Unsupported DatabaseMigration status";
  static final String MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE = "Upgrade is not supported on embedded database.";
  static final String MESSAGE_MIGRATION_REQUIRED = "Database migration is required. DB migration can be started using WS /api/system/migrate_db.";
  static final String MESSAGE_STATUS_NONE = "Database is up-to-date, no migration needed.";
  static final String MESSAGE_STATUS_RUNNING = "Database migration is running.";
  static final String MESSAGE_STATUS_SUCCEEDED = "Migration succeeded.";
  static final String MESSAGE_STATUS_FAILED = "Migration failed: %s.<br/> Please check logs.";

  private DbMigrationJsonWriter() {
    // static methods only
  }

  static void write(JsonWriter json, DatabaseMigrationState databaseMigrationState) {
    json.beginObject()
      .prop(FIELD_STATE, statusToJson(databaseMigrationState.getStatus()))
      .prop(FIELD_MESSAGE, buildMessage(databaseMigrationState))
      .propDateTime(FIELD_STARTED_AT, databaseMigrationState.getStartedAt())
      .endObject();
  }

  static void writeNotSupportedResponse(JsonWriter json) {
    json.beginObject()
      .prop(FIELD_STATE, STATUS_NOT_SUPPORTED)
      .prop(FIELD_MESSAGE, MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE)
      .endObject();
  }

  static void writeJustStartedResponse(JsonWriter json, DatabaseMigrationState databaseMigrationState) {
    json.beginObject()
      .prop(FIELD_STATE, statusToJson(RUNNING))
      .prop(FIELD_MESSAGE, MESSAGE_STATUS_RUNNING)
      .propDateTime(FIELD_STARTED_AT, databaseMigrationState.getStartedAt())
      .endObject();
  }

  static void writeMigrationRequiredResponse(JsonWriter json) {
    json.beginObject()
      .prop(FIELD_STATE, STATUS_MIGRATION_REQUIRED)
      .prop(FIELD_MESSAGE, MESSAGE_MIGRATION_REQUIRED)
      .endObject();
  }

  private static String statusToJson(DatabaseMigrationState.Status status) {
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

  private static String buildMessage(DatabaseMigrationState databaseMigrationState) {
    switch (databaseMigrationState.getStatus()) {
      case NONE:
        return MESSAGE_STATUS_NONE;
      case RUNNING:
        return MESSAGE_STATUS_RUNNING;
      case SUCCEEDED:
        return MESSAGE_STATUS_SUCCEEDED;
      case FAILED:
        return String.format(MESSAGE_STATUS_FAILED, failureMessage(databaseMigrationState));
      default:
        return UNSUPPORTED_DATABASE_MIGRATION_STATUS;
    }
  }

  private static String failureMessage(DatabaseMigrationState databaseMigrationState) {
    Throwable failureError = databaseMigrationState.getError();
    if (failureError == null) {
      return "No failure error";
    }
    return failureError.getMessage();
  }

  static String statusDescription() {
    return "State values are:" +
      "<ul>" +
      "<li>NO_MIGRATION: DB is up to date with current version of SonarQube.</li>" +
      "<li>NOT_SUPPORTED: Migration is not supported on embedded databases.</li>" +
      "<li>MIGRATION_RUNNING: DB migration is under go.</li>" +
      "<li>MIGRATION_SUCCEEDED: DB migration has run and has been successful.</li>" +
      "<li>MIGRATION_FAILED: DB migration has run and failed. SonarQube must be restarted in order to retry a " +
      "DB migration (optionally after DB has been restored from backup).</li>" +
      "<li>MIGRATION_REQUIRED: DB migration is required.</li>" +
      "</ul>";
  }
}
