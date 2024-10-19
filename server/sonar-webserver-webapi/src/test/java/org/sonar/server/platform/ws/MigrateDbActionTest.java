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

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.server.platform.db.migration.DatabaseMigration;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.platform.db.migration.DatabaseMigrationState.Status;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.FAILED;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.NONE;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.RUNNING;
import static org.sonar.server.platform.db.migration.DatabaseMigrationState.Status.SUCCEEDED;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class MigrateDbActionTest {

  private static final Instant SOME_DATE = Instant.now();
  private static final String SOME_THROWABLE_MSG = "blablabla pop !";
  private static final String DEFAULT_ERROR_MSG = "No failure error";

  private static final String STATUS_NO_MIGRATION = "NO_MIGRATION";
  private static final String STATUS_NOT_SUPPORTED = "NOT_SUPPORTED";
  private static final String STATUS_MIGRATION_RUNNING = "MIGRATION_RUNNING";
  private static final String STATUS_MIGRATION_FAILED = "MIGRATION_FAILED";
  private static final String STATUS_MIGRATION_SUCCEEDED = "MIGRATION_SUCCEEDED";

  private static final String MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE = "Upgrade is not supported on embedded database.";
  private static final String MESSAGE_STATUS_NONE = "Database is up-to-date, no migration needed.";
  private static final String MESSAGE_STATUS_RUNNING = "Database migration is running.";
  private static final String MESSAGE_STATUS_SUCCEEDED = "Migration succeeded.";
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

  private DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  private Database database = mock(Database.class);
  private Dialect dialect = mock(Dialect.class);
  private DatabaseMigration databaseMigration = mock(DatabaseMigration.class);
  private DatabaseMigrationState migrationState = mock(DatabaseMigrationState.class);
  private MigrateDbAction underTest = new MigrateDbAction(databaseVersion, database, migrationState, databaseMigration);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void wireMocksTogether() {
    when(database.getDialect()).thenReturn(dialect);
    when(databaseVersion.getVersion()).thenReturn(Optional.of(150L));
  }

  @Test
  public void ISE_is_thrown_when_version_can_not_be_retrieved_from_database() {
    reset(databaseVersion);
    when(databaseVersion.getVersion()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot connect to Database.");
  }

  @Test
  public void verify_example() {
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(RUNNING);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(DATE_TIME_FORMATTER.parse("2015-02-23T18:54:23+0100", Instant::from)));

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(getClass().getResource("example-migrate_db.json"));
  }

  @Test
  public void msg_is_operational_and_state_from_database_migration_when_databaseversion_status_is_UP_TO_DATE() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    when(migrationState.getStatus()).thenReturn(NONE);

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_NO_MIGRATION, MESSAGE_STATUS_NONE));
  }

  // this test will raise a IllegalArgumentException when an unsupported value is added to the Status enum
  @Test
  public void defensive_test_all_values_of_migration_Status_must_be_supported() {
    for (Status status : filter(Arrays.asList(DatabaseMigrationState.Status.values()), not(in(ImmutableList.of(NONE, RUNNING, FAILED, SUCCEEDED))))) {
      when(migrationState.getStatus()).thenReturn(status);

      tester.newRequest().execute();
    }
  }

  @Test
  public void state_from_database_migration_when_databaseversion_status_is_REQUIRES_DOWNGRADE() {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
    when(migrationState.getStatus()).thenReturn(NONE);

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_NO_MIGRATION, MESSAGE_STATUS_NONE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_is_NONE_with_specific_msg_when_db_requires_upgrade_but_dialect_does_not_support_migration(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(false);

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_NOT_SUPPORTED, MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_when_dbmigration_status_is_RUNNING(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(RUNNING);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_MIGRATION_RUNNING, MESSAGE_STATUS_RUNNING, SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_and_msg_includes_error_when_dbmigration_status_is_FAILED(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(FAILED);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));
    when(migrationState.getError()).thenReturn(Optional.of(new UnsupportedOperationException(SOME_THROWABLE_MSG)));

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_MIGRATION_FAILED, failedMsg(SOME_THROWABLE_MSG), SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_and_msg_has_default_msg_when_dbmigration_status_is_FAILED(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(FAILED);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));
    when(migrationState.getError()).thenReturn(Optional.empty()); // no failure throwable caught

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_MIGRATION_FAILED, failedMsg(DEFAULT_ERROR_MSG), SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_and_msg_has_default_msg_when_dbmigration_status_is_SUCCEEDED(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(SUCCEEDED);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_MIGRATION_SUCCEEDED, MESSAGE_STATUS_SUCCEEDED, SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void start_migration_and_return_state_from_databasemigration_when_dbmigration_status_is_NONE(DatabaseVersion.Status status) {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(NONE);
    when(migrationState.getStartedAt()).thenReturn(Optional.of(SOME_DATE));

    TestResponse response = tester.newRequest().execute();

    verify(databaseMigration).startIt();
    assertJson(response.getInput()).isSimilarTo(expectedResponse(STATUS_MIGRATION_RUNNING, MESSAGE_STATUS_RUNNING, SOME_DATE));
  }

  @DataProvider
  public static Object[][] statusRequiringDbMigration() {
    return new Object[][]{
      {DatabaseVersion.Status.FRESH_INSTALL},
      {DatabaseVersion.Status.REQUIRES_UPGRADE},
    };
  }

  private static String failedMsg(@Nullable String t) {
    return "Migration failed: " + t + ".<br/> Please check logs.";
  }

  private static String expectedResponse(String status, String msg) {
    return "{" +
      "\"state\":\"" + status + "\"," +
      "\"message\":\"" + msg + "\"" +
      "}";
  }

  private static String expectedResponse(String status, String msg, Instant date) {
    return "{" +
      "\"state\":\"" + status + "\"," +
      "\"message\":\"" + msg + "\"," +
      "\"startedAt\":\"" + DATE_TIME_FORMATTER.format(date.atZone(ZoneOffset.UTC)) + "\"" +
      "}";
  }
}
