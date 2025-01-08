/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public interface DatabaseMigrationState {

  String UNSUPPORTED_DATABASE_MIGRATION_STATUS = "Unsupported DatabaseMigration status";
  String NO_CONNECTION_TO_DB = "Cannot connect to Database.";

  enum Status {
    NONE("NO_MIGRATION", "Database is up-to-date, no migration needed."),
    RUNNING("MIGRATION_RUNNING", "Database migration is running."),
    FAILED("MIGRATION_FAILED", "Migration failed: %s.<br/> Please check logs."),
    SUCCEEDED("MIGRATION_SUCCEEDED", "Migration succeeded."),
    STATUS_NOT_SUPPORTED("NOT_SUPPORTED", "Upgrade is not supported on embedded database."),
    MIGRATION_REQUIRED("MIGRATION_REQUIRED", "Database migration is required. DB migration can be started using WS /api/system/migrate_db.");

    private final String stringRepresentation;
    private final String message;

    Status(String stringRepresentation, String message) {
      this.stringRepresentation = stringRepresentation;
      this.message = message;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public String toString() {
      return stringRepresentation;
    }

  }

  /**
   * Current status of the migration.
   */
  Status getStatus();

  /**
   * The time and day the last migration was started.
   * <p>
   * If no migration was ever started, the returned date is empty.
   * </p>
   *
   * @return a {@link Date} if present
   */
  Optional<Instant> getStartedAt();

  /**
   * The error of the last migration if it failed.
   *
   * @return a {@link Throwable} if present.
   */
  Optional<Throwable> getError();

  /**
   * The amount of migrations already completed.
   */
  int getCompletedMigrations();

  /**
   * The total amount of migrations to be performed.
   */
  int getTotalMigrations();

  /**
   * The expected finish timestamp of the migration if known.
   */
  Optional<Instant> getExpectedFinishDate(Instant now);

}
