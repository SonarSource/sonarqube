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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.platform.db.migration.DatabaseMigrationStateImpl;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.FAILED;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.NONE;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.RUNNING;
import static org.sonar.server.v2.WebApiEndpoints.DATABASE_MIGRATIONS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DatabaseMigrationsControllerTest {

  private static final Instant SOME_DATE = Instant.now();
  private static final String SOME_DATE_STRING = DateTimeFormatter.ISO_DATE_TIME.format(SOME_DATE.atZone(ZoneOffset.UTC));
  private final DatabaseVersion databaseVersion = mock();
  private final DatabaseMigrationStateImpl migrationState = mock();
  private final Dialect dialect = mock(Dialect.class);
  private final Database database = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DatabaseMigrationsController(databaseVersion, migrationState, database));

  @BeforeEach
  public void before() {
    when(database.getDialect()).thenReturn(dialect);
    when(databaseVersion.getVersion()).thenReturn(Optional.of(1L));
  }

  @Test
  void getStatus_whenDatabaseHasNoVersion_return500() throws Exception {
    Mockito.reset(databaseVersion);
    when(databaseVersion.getVersion()).thenReturn(Optional.empty());

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().is5xxServerError(),
      content().json("{\"message\":\"Cannot connect to Database.\"}"));
  }

  @Test
  void getStatus_migrationNotNeeded_returnUpToDateStatus() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    when(migrationState.getStatus()).thenReturn(NONE);

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"NO_MIGRATION\",\"message\":\"Database is up-to-date, no migration needed.\"}"));
  }

  @Test
  void getStatus_whenDowngradeRequired_returnNone() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
    when(migrationState.getStatus()).thenReturn(NONE);

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"NO_MIGRATION\",\"message\":\"Database is up-to-date, no migration needed.\"}"));
  }

  @Test
  void getStatus_whenDbRequiresUpgradeButDialectIsNotSupported_returnNotSupported() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.FRESH_INSTALL);
    when(dialect.supportsMigration()).thenReturn(false);

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"NOT_SUPPORTED\",\"message\":\"Upgrade is not supported on embedded database.\"}"));
  }

  @Test
  void getStatus_whenDbMigrationsRunning_returnRunning() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(RUNNING);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));
    when(migrationState.getExpectedFinishDate(any())).thenReturn(Optional.of(SOME_DATE));
    when(migrationState.getCompletedMigrations()).thenReturn(1);
    when(migrationState.getTotalMigrations()).thenReturn(10);

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"MIGRATION_RUNNING\",\"completedSteps\":1,\"totalSteps\":10," +
        "\"message\":\"Database migration is running.\",\"expectedFinishTimestamp\":\"" + SOME_DATE_STRING + "\"}"));
  }

  @Test
  void getStatus_whenDbMigrationsFailed_returnFailed() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(DatabaseMigrationState.Status.FAILED);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"MIGRATION_FAILED\",\"message\":\"Migration failed: %s.<br/> Please check logs.\"}"));
  }

  @Test
  void getStatus_whenDbMigrationsSucceeded_returnSucceeded() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(DatabaseMigrationState.Status.SUCCEEDED);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"MIGRATION_SUCCEEDED\",\"message\":\"Migration succeeded.\"}"));
  }

  @Test
  void getStatus_whenMigrationRequired_returnMigrationRequired() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_UPGRADE);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(NONE);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"MIGRATION_REQUIRED\",\"message\":\"Database migration is required. DB migration " +
        "can be started using WS /api/system/migrate_db.\"}"));
  }

  @Test
  void getStatus_whenMigrationFailedWithError_IncludeErrorInResponse() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.FRESH_INSTALL);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(FAILED);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));
    when(migrationState.getError()).thenReturn(Optional.of(new UnsupportedOperationException("error message")));

    mockMvc.perform(get(DATABASE_MIGRATIONS_ENDPOINT)).andExpectAll(status().isOk(),
      content().json("{\"status\":\"MIGRATION_FAILED\",\"message\":\"error message\"}"));
  }
}
