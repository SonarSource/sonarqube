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
package org.sonar.server.v2.api.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.NO_CONNECTION_TO_DB;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.UNSUPPORTED_DATABASE_MIGRATION_STATUS;
import static org.sonar.server.v2.WebApiEndpoints.DATABASE_MIGRATIONS_ENDPOINT;

@RestController
@RequestMapping(DATABASE_MIGRATIONS_ENDPOINT)
public class DatabaseMigrationsController {

  private final DatabaseVersion databaseVersion;
  private final DatabaseMigrationState databaseMigrationState;
  private final Database database;

  private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  public DatabaseMigrationsController(DatabaseVersion databaseVersion, DatabaseMigrationState databaseMigrationState, Database database) {
    this.databaseVersion = databaseVersion;
    this.databaseMigrationState = databaseMigrationState;
    this.database = database;
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Operation(summary = "Gets the status of ongoing database migrations, if any", description = "Return the detailed status of ongoing database migrations" +
    " including starting date. If no migration is ongoing or needed it is still possible to call this endpoint and receive appropriate information.")
  @GetMapping
  public DatabaseMigrationsResponse getStatus() {
    Optional<Long> currentVersion = databaseVersion.getVersion();
    checkState(currentVersion.isPresent(), NO_CONNECTION_TO_DB);
    DatabaseVersion.Status status = databaseVersion.getStatus();
    if (status == DatabaseVersion.Status.UP_TO_DATE || status == DatabaseVersion.Status.REQUIRES_DOWNGRADE) {
      return new DatabaseMigrationsResponse(databaseMigrationState);
    } else if (!database.getDialect().supportsMigration()) {
      return new DatabaseMigrationsResponse(DatabaseMigrationState.Status.STATUS_NOT_SUPPORTED);
    } else {
      return switch (databaseMigrationState.getStatus()) {
        case RUNNING, FAILED, SUCCEEDED -> new DatabaseMigrationsResponse(databaseMigrationState);
        case NONE -> new DatabaseMigrationsResponse(DatabaseMigrationState.Status.MIGRATION_REQUIRED);
        default -> throw new IllegalArgumentException(UNSUPPORTED_DATABASE_MIGRATION_STATUS);
      };
    }

  }

  public record DatabaseMigrationsResponse(
    String status,
    @Nullable Integer completedSteps,
    @Nullable Integer totalSteps,
    @Nullable String startedAt,
    @Nullable String message,
    @Nullable String expectedFinishTimestamp) {

    public DatabaseMigrationsResponse(DatabaseMigrationState state) {
      this(state.getStatus().toString(),
        state.getCompletedMigrations(),
        state.getTotalMigrations(),
        state.getStartedAt() != null ? simpleDateFormat.format(state.getStartedAt()) : null,
        state.getError() != null ? state.getError().getMessage() : state.getStatus().getMessage(),
        state.getExpectedFinishDate() != null ? simpleDateFormat.format(state.getExpectedFinishDate()) : null);
    }

    public DatabaseMigrationsResponse(DatabaseMigrationState.Status status) {
      this(status.toString(), null, null, null, status.getMessage(), null);
    }
  }

}
