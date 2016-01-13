/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Arrays;
import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.version.DatabaseMigration;
import org.sonar.db.version.DatabaseMigration.Status;
import org.sonar.db.version.DatabaseVersion;
import org.sonar.server.ws.WsTester;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.version.DatabaseMigration.Status.FAILED;
import static org.sonar.db.version.DatabaseMigration.Status.NONE;
import static org.sonar.db.version.DatabaseMigration.Status.RUNNING;
import static org.sonar.db.version.DatabaseMigration.Status.SUCCEEDED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.MESSAGE_MIGRATION_REQUIRED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.MESSAGE_STATUS_NONE;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.MESSAGE_STATUS_RUNNING;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.MESSAGE_STATUS_SUCCEEDED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.STATUS_MIGRATION_FAILED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.STATUS_MIGRATION_REQUIRED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.STATUS_MIGRATION_RUNNING;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.STATUS_MIGRATION_SUCCEEDED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.STATUS_NOT_SUPPORTED;
import static org.sonar.server.platform.ws.DbMigrationJsonWriter.STATUS_NO_MIGRATION;
import static org.sonar.test.JsonAssert.assertJson;

public class DbMigrationStatusActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final Date SOME_DATE = new Date();
  private static final String SOME_THROWABLE_MSG = "blablabla pop !";
  private static final String DEFAULT_ERROR_MSG = "No failure error";
  private static final int CURRENT_VERSION = DatabaseVersion.LAST_VERSION;
  private static final int OLD_VERSION = CURRENT_VERSION - 1;
  private static final int NEWER_VERSION = CURRENT_VERSION + 1;

  DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  Database database = mock(Database.class);
  Dialect dialect = mock(Dialect.class);
  DatabaseMigration databaseMigration = mock(DatabaseMigration.class);
  DbMigrationStatusAction underTest = new DbMigrationStatusAction(databaseVersion, database, databaseMigration);

  Request request = mock(Request.class);
  WsTester.TestResponse response = new WsTester.TestResponse();

  @Before
  public void wireMocksTogether() {
    when(database.getDialect()).thenReturn(dialect);
  }

  @Test
  public void verify_example() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(RUNNING);
    when(databaseMigration.startedAt()).thenReturn(DateUtils.parseDateTime("2015-02-23T18:54:23+0100"));
    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(getClass().getResource("example-migrate_db.json"));
  }

  @Test
  public void throws_ISE_when_databaseVersion_can_not_be_determined() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cannot connect to Database.");

    underTest.handle(request, response);
  }

  @Test
  public void msg_is_operational_and_state_from_databasemigration_when_databaseversion_is_equal_to_currentversion() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(CURRENT_VERSION);
    when(databaseMigration.status()).thenReturn(NONE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_NO_MIGRATION, MESSAGE_STATUS_NONE));
  }

  // this test will raise a IllegalArgumentException when an unsupported value is added to the Status enum
  @Test
  public void defensive_test_all_values_of_Status_must_be_supported() throws Exception {
    for (Status status : filter(Arrays.asList(Status.values()), not(in(ImmutableList.of(NONE, RUNNING, FAILED, SUCCEEDED))))) {
      when(databaseVersion.getVersion()).thenReturn(CURRENT_VERSION);
      when(databaseMigration.status()).thenReturn(status);

      underTest.handle(request, response);
    }
  }

  @Test
  public void state_from_databasemigration_when_databaseversion_greater_than_currentversion() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(NEWER_VERSION);
    when(databaseMigration.status()).thenReturn(NONE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_NO_MIGRATION, MESSAGE_STATUS_NONE));
  }

  @Test
  public void state_is_NONE_with_specific_msg_when_version_is_less_than_current_version_and_dialect_does_not_support_migration() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(false);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_NOT_SUPPORTED, MESSAGE_NO_MIGRATION_ON_EMBEDDED_DATABASE));
  }

  @Test
  public void state_from_databasemigration_when_dbmigration_status_is_RUNNING() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(RUNNING);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_RUNNING, MESSAGE_STATUS_RUNNING, SOME_DATE));
  }

  @Test
  public void state_from_databasemigration_and_msg_includes_error_when_dbmigration_status_is_FAILED() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(FAILED);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);
    when(databaseMigration.failureError()).thenReturn(new UnsupportedOperationException(SOME_THROWABLE_MSG));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_FAILED, failedMsg(SOME_THROWABLE_MSG), SOME_DATE));
  }

  @Test
  public void state_from_databasemigration_and_msg_has_default_msg_when_dbmigration_status_is_FAILED() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(FAILED);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);
    when(databaseMigration.failureError()).thenReturn(null); // no failure throwable caught

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_FAILED, failedMsg(DEFAULT_ERROR_MSG), SOME_DATE));
  }

  @Test
  public void state_from_databasemigration_and_msg_has_default_msg_when_dbmigration_status_is_SUCCEEDED() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(SUCCEEDED);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_SUCCEEDED, MESSAGE_STATUS_SUCCEEDED, SOME_DATE));
  }

  @Test
  public void start_migration_and_return_state_from_databasemigration_when_dbmigration_status_is_NONE() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(NONE);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(STATUS_MIGRATION_REQUIRED, MESSAGE_MIGRATION_REQUIRED));
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
