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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.ws.WsTester;

import javax.annotation.Nullable;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.db.migrations.DatabaseMigration.Status.FAILED;
import static org.sonar.server.db.migrations.DatabaseMigration.Status.NONE;
import static org.sonar.server.db.migrations.DatabaseMigration.Status.RUNNING;
import static org.sonar.server.db.migrations.DatabaseMigration.Status.SUCCEEDED;
import static org.sonar.test.JsonAssert.assertJson;

public class ServerMigrateWsActionTest {

  private static final String UPTODATE_MSG = "Database is up-to-date, no migration needed.";
  private static final String MIG_NOT_SUPPORTED_MSG = "Upgrade is not supported. Please use a <a href=\\\"http://redirect.sonarsource.com/doc/requirements.html\\\">production-ready database</a>.";
  private static final String RUNNING_MSG = "Database migration is running.";
  private static final Date SOME_DATE = new Date();
  private static final String SOME_THROWABLE_MSG = "blablabla pop !";
  private static final String DEFAULT_ERROR_MSG = "No failure error";
  private static final String MIG_SUCCESS_MSG = "Migration succeeded.";
  private static final int CURRENT_VERSION = DatabaseVersion.LAST_VERSION;
  private static final int OLD_VERSION = CURRENT_VERSION - 1;
  private static final int NEWER_VERSION = CURRENT_VERSION + 1;
  private static final String STATUS_RUNNING = "MIGRATION_RUNNING";
  private static final String STATUS_SUCCEEDED = "MIGRATION_SUCCEEDED";
  private static final String STATUS_FAILED = "MIGRATION_FAILED";
  private static final String STATUS_NONE = "NO_MIGRATION";

  DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  Database database = mock(Database.class);
  Dialect dialect = mock(Dialect.class);
  DatabaseMigration databaseMigration = mock(DatabaseMigration.class);
  ServerMigrateWsAction underTest = new ServerMigrateWsAction(databaseVersion, database, databaseMigration);

  Request request = mock(Request.class);
  WsTester.TestResponse response = new WsTester.TestResponse();

  @Before
  public void wireMocksTogether() throws Exception {
    when(database.getDialect()).thenReturn(dialect);
  }

  @Test(expected = IllegalStateException.class)
  public void ISE_is_thrown_when_version_can_not_be_retrieved_from_database() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(null);

    underTest.handle(request, response);
  }

  @Test
  public void msg_is_operational_and_state_from_databasemigration_when_databaseversion_is_equal_to_currentversion() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(CURRENT_VERSION);
    when(databaseMigration.status()).thenReturn(NONE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(true, STATUS_NONE, UPTODATE_MSG));
  }

  @Test
  public void msg_is_operational_and_state_from_databasemigration_when_databaseversion_greater_than_currentversion() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(NEWER_VERSION);
    when(databaseMigration.status()).thenReturn(NONE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(true, STATUS_NONE, UPTODATE_MSG));
  }

  @Test
  public void msg_is_not_operational_and_state_is_NONE_with_specific_msg_when_version_is_less_than_current_version_and_dialect_does_not_support_migration() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(false);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(false, STATUS_NONE, MIG_NOT_SUPPORTED_MSG));
  }

  @Test
  public void msg_is_not_operational_and_state_from_databasemigration_when_dbmigration_status_is_RUNNING() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(RUNNING);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(false, STATUS_RUNNING, RUNNING_MSG, SOME_DATE));
  }

  @Test
  public void msg_is_not_operational_and_state_from_databasemigration_and_msg_includes_error_when_dbmigration_status_is_FAILED() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(FAILED);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);
    when(databaseMigration.failureError()).thenReturn(new UnsupportedOperationException(SOME_THROWABLE_MSG));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(false, STATUS_FAILED, failedMsg(SOME_THROWABLE_MSG), SOME_DATE));
  }

  @Test
  public void msg_is_not_operational_and_state_from_databasemigration_and_msg_has_default_msg_when_dbmigration_status_is_FAILED() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(FAILED);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);
    when(databaseMigration.failureError()).thenReturn(null); // no failure throwable caught

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(false, STATUS_FAILED, failedMsg(DEFAULT_ERROR_MSG), SOME_DATE));
  }

  @Test
  public void msg_is_operational_and_state_from_databasemigration_and_msg_has_default_msg_when_dbmigration_status_is_FAILED() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(SUCCEEDED);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(true, STATUS_SUCCEEDED, MIG_SUCCESS_MSG, SOME_DATE));
  }

  @Test
  public void start_migration_and_return_msg_is_not_operational_and_state_from_databasemigration_when_dbmigration_status_is_NONE() throws Exception {
    when(databaseVersion.getVersion()).thenReturn(OLD_VERSION);
    when(dialect.supportsMigration()).thenReturn(true);
    when(databaseMigration.status()).thenReturn(NONE).thenReturn(RUNNING);
    when(databaseMigration.startedAt()).thenReturn(SOME_DATE);

    underTest.handle(request, response);

    verify(databaseMigration).startIt();
    assertJson(response.outputAsString()).isSimilarTo(expectedResponse(false, STATUS_RUNNING, RUNNING_MSG, SOME_DATE));
  }

  private static String failedMsg(@Nullable String t) {
    return "Migration failed: " + t + ".<br/> Please check logs.";
  }

  private static String expectedResponse(boolean operational, String status, String msg) {
    return "{" +
      "\"operational\":" + operational + "," +
      "\"state\":\"" + status + "\"," +
      "\"message\":\"" + msg + "\"" +
      "}";
  }

  private static String expectedResponse(boolean operational, String status, String msg, Date date) {
    return "{" +
      "\"operational\":" + operational + "," +
      "\"state\":\"" + status + "\"," +
      "\"message\":\"" + msg + "\"," +
      "\"startedAt\":\"" + DateUtils.formatDateTime(date) + "\"" +
      "}";
  }
}
