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

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.server.platform.db.migration.DatabaseMigration;
import org.sonar.server.platform.db.migration.DatabaseMigrationState.Status;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.ws.WsTester;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
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
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final Date SOME_DATE = new Date();
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

  private DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  private Database database = mock(Database.class);
  private Dialect dialect = mock(Dialect.class);
  private DatabaseMigration databaseMigration = mock(DatabaseMigration.class);
  private DatabaseMigrationState migrationState = mock(DatabaseMigrationState.class);
  private MigrateDbAction underTest = new MigrateDbAction(databaseVersion, database, migrationState, databaseMigration);

  private Request request = mock(Request.class);
  private WsTester.TestResponse response = new WsTester.TestResponse();

  @Before
  public void wireMocksTogether() {
    when(database.getDialect()).thenReturn(dialect);
    when(databaseVersion.getVersion()).thenReturn(Optional.of(150L));
  }

  @Test
  public void ISE_is_thrown_when_version_can_not_be_retrieved_from_database() throws Exception {
    reset(databaseVersion);
    when(databaseVersion.getVersion()).thenReturn(Optional.empty());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cannot connect to Database.");

    underTest.handle(request, response);
  }

  @Test
  public void verify_example() throws Exception {
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(RUNNING);
    when(migrationState.getStartedAt()).thenReturn(DateUtils.parseDateTime("2015-02-23T18:54:23+0100"));
    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(getClass().getResource("example-migrate_db.json"));
  }

  @Test
  public void msg_is_operational_and_state_from_database_migration_when_databaseversion_status_is_UP_TO_DATE() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.UP_TO_DATE);
    when(migrationState.getStatus()).thenReturn(NONE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_NO_MIGRATION, MESSAGE_STATUS_NONE));
  }

  // this test will raise a IllegalArgumentException when an unsupported value is added to the Status enum
  @Test
  public void defensive_test_all_values_of_migration_Status_must_be_supported() throws Exception {
    for (Status status : filter(Arrays.asList(DatabaseMigrationState.Status.values()), not(in(ImmutableList.of(NONE, RUNNING, FAILED, SUCCEEDED))))) {
      when(migrationState.getStatus()).thenReturn(status);

      underTest.handle(request, response);
    }
  }

  @Test
  public void state_from_database_migration_when_databaseversion_status_is_REQUIRES_DOWNGRADE() throws Exception {
    when(databaseVersion.getStatus()).thenReturn(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
    when(migrationState.getStatus()).thenReturn(NONE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_NO_MIGRATION, MESSAGE_STATUS_NONE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_is_NONE_with_specific_msg_when_db_requires_upgrade_but_dialect_does_not_support_migration(DatabaseVersion.Status status) throws Exception {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(false);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_NOT_SUPPORTED, MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_when_dbmigration_status_is_RUNNING(DatabaseVersion.Status status) throws Exception {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(RUNNING);
    when(migrationState.getStartedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_RUNNING, MESSAGE_STATUS_RUNNING, SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_and_msg_includes_error_when_dbmigration_status_is_FAILED(DatabaseVersion.Status status) throws Exception {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(FAILED);
    when(migrationState.getStartedAt()).thenReturn(SOME_DATE);
    when(migrationState.getError()).thenReturn(new UnsupportedOperationException(SOME_THROWABLE_MSG));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_FAILED, failedMsg(SOME_THROWABLE_MSG), SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_and_msg_has_default_msg_when_dbmigration_status_is_FAILED(DatabaseVersion.Status status) throws Exception {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(FAILED);
    when(migrationState.getStartedAt()).thenReturn(SOME_DATE);
    when(migrationState.getError()).thenReturn(null); // no failure throwable caught

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_FAILED, failedMsg(DEFAULT_ERROR_MSG), SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void state_from_database_migration_and_msg_has_default_msg_when_dbmigration_status_is_SUCCEEDED(DatabaseVersion.Status status) throws Exception {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(SUCCEEDED);
    when(migrationState.getStartedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_SUCCEEDED, MESSAGE_STATUS_SUCCEEDED, SOME_DATE));
  }

  @Test
  @UseDataProvider("statusRequiringDbMigration")
  public void start_migration_and_return_state_from_databasemigration_when_dbmigration_status_is_NONE(DatabaseVersion.Status status) throws Exception {
    when(databaseVersion.getStatus()).thenReturn(status);
    when(dialect.supportsMigration()).thenReturn(true);
    when(migrationState.getStatus()).thenReturn(NONE);
    when(migrationState.getStartedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    verify(databaseMigration).startIt();
    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_RUNNING, MESSAGE_STATUS_RUNNING, SOME_DATE));
  }

  @DataProvider
  public static Object[][] statusRequiringDbMigration() {
    return new Object[][] {
        { DatabaseVersion.Status.FRESH_INSTALL },
        { DatabaseVersion.Status.REQUIRES_UPGRADE },
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

  private static String expectedResponse(String status, String msg, Date date) {
    return "{" +
      "\"state\":\"" + status + "\"," +
      "\"message\":\"" + msg + "\"," +
      "\"startedAt\":\"" + DateUtils.formatDateTime(date) + "\"" +
      "}";
  }
}
