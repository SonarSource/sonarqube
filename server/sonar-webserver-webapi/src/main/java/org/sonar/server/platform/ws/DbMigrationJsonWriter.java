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
package org.sonar.server.platform.ws;

import java.util.Date;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;

import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.FAILED;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.MIGRATION_REQUIRED;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.RUNNING;

public class DbMigrationJsonWriter {
  static final String FIELD_STATE = "state";
  static final String FIELD_MESSAGE = "message";
  static final String FIELD_STARTED_AT = "startedAt";


  private DbMigrationJsonWriter() {
    // static methods only
  }

  static void write(JsonWriter json, DatabaseMigrationState databaseMigrationState) {
    json.beginObject()
      .prop(FIELD_STATE, databaseMigrationState.getStatus().toString())
      .prop(FIELD_MESSAGE, writeMessageIncludingError(databaseMigrationState))
      .propDateTime(FIELD_STARTED_AT, databaseMigrationState.getStartedAt().map(Date::from).orElse(null))
      .endObject();
  }

  private static String writeMessageIncludingError(DatabaseMigrationState state) {
    if (state.getStatus() == FAILED) {
      Throwable error = state.getError().orElse(null);
      return String.format(state.getStatus().getMessage(), error != null ? error.getMessage() : "No failure error");
    } else {
      return state.getStatus().getMessage();
    }
  }

  static void writeNotSupportedResponse(JsonWriter json) {
    json.beginObject()
      .prop(FIELD_STATE, DatabaseMigrationState.Status.STATUS_NOT_SUPPORTED.toString())
      .prop(FIELD_MESSAGE, DatabaseMigrationState.Status.STATUS_NOT_SUPPORTED.getMessage())
      .endObject();
  }

  static void writeJustStartedResponse(JsonWriter json, DatabaseMigrationState databaseMigrationState) {
    json.beginObject()
      .prop(FIELD_STATE, RUNNING.toString())
      .prop(FIELD_MESSAGE, RUNNING.getMessage())
      .propDateTime(FIELD_STARTED_AT, databaseMigrationState.getStartedAt().map(Date::from).orElse(null))
      .endObject();
  }

  static void writeMigrationRequiredResponse(JsonWriter json) {
    json.beginObject()
      .prop(FIELD_STATE, MIGRATION_REQUIRED.toString())
      .prop(FIELD_MESSAGE, MIGRATION_REQUIRED.getMessage())
      .endObject();
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
