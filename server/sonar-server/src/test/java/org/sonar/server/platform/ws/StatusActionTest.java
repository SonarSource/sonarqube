/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.File;
import java.util.Date;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.app.RestartFlagHolderImpl;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Iterables.filter;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class StatusActionTest {
  private static final String SERVER_ID = "20150504120436";
  private static final String SERVER_VERSION = "5.1";
  private static final String STATUS_UP = "UP";
  private static final String STATUS_STARTING = "STARTING";
  private static final String STATUS_DOWN = "DOWN";
  private static final String STATUS_MIGRATION_NEEDED = "DB_MIGRATION_NEEDED";
  private static final String STATUS_MIGRATION_RUNNING = "DB_MIGRATION_RUNNING";
  private static final String STATUS_RESTARTING = "RESTARTING";
  private static final Set<DatabaseMigrationState.Status> SUPPORTED_DATABASE_MIGRATION_STATUSES = of(DatabaseMigrationState.Status.FAILED, DatabaseMigrationState.Status.NONE,
    DatabaseMigrationState.Status.SUCCEEDED, DatabaseMigrationState.Status.RUNNING);
  private static final Set<Platform.Status> SUPPORTED_PLATFORM_STATUSES = of(Platform.Status.BOOTING, Platform.Status.SAFEMODE, Platform.Status.STARTING, Platform.Status.UP);

  private static Server server = new Dummy51Server();
  private DatabaseMigrationState migrationState = mock(DatabaseMigrationState.class);
  private Platform platform = mock(Platform.class);
  private RestartFlagHolder restartFlagHolder = new RestartFlagHolderImpl();

  private WsActionTester underTest = new WsActionTester(new StatusAction(server, migrationState, platform, restartFlagHolder));

  @Test
  public void action_status_is_defined() {
    WebService.Action action = underTest.getDef();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();

    assertThat(action.params()).isEmpty();
  }

  @Test
  public void verify_example() {
    when(platform.status()).thenReturn(Platform.Status.UP);
    restartFlagHolder.unset();

    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo(getClass().getResource("example-status.json"));
  }

  @Test
  public void status_is_UP_if_platform_is_UP_and_restartFlag_is_false_whatever_databaseMigration_status_is() throws Exception {
    for (DatabaseMigrationState.Status databaseMigrationStatus : DatabaseMigrationState.Status.values()) {
      verifyStatus(Platform.Status.UP, databaseMigrationStatus, STATUS_UP);
    }
  }

  @Test
  public void status_is_RESTARTING_if_platform_is_UP_and_restartFlag_is_true_whatever_databaseMigration_status_is() throws Exception {
    restartFlagHolder.set();

    for (DatabaseMigrationState.Status databaseMigrationStatus : DatabaseMigrationState.Status.values()) {
      verifyStatus(Platform.Status.UP, databaseMigrationStatus, STATUS_RESTARTING);
    }
  }

  @Test
  public void status_is_DOWN_if_platform_is_BOOTING_whatever_databaseMigration_status_is() throws Exception {
    for (DatabaseMigrationState.Status databaseMigrationStatus : DatabaseMigrationState.Status.values()) {
      verifyStatus(Platform.Status.BOOTING, databaseMigrationStatus, STATUS_DOWN);
    }
  }

  @Test
  public void status_is_DB_MIGRATION_NEEDED_if_platform_is_SAFEMODE_and_databaseMigration_is_NONE() throws Exception {
    verifyStatus(Platform.Status.SAFEMODE, DatabaseMigrationState.Status.NONE, STATUS_MIGRATION_NEEDED);
  }

  @Test
  public void status_is_DB_MIGRATION_RUNNING_if_platform_is_SAFEMODE_and_databaseMigration_is_RUNNING() throws Exception {
    verifyStatus(Platform.Status.SAFEMODE, DatabaseMigrationState.Status.RUNNING, STATUS_MIGRATION_RUNNING);
  }

  @Test
  public void status_is_STATUS_STARTING_if_platform_is_SAFEMODE_and_databaseMigration_is_SUCCEEDED() throws Exception {
    verifyStatus(Platform.Status.SAFEMODE, DatabaseMigrationState.Status.SUCCEEDED, STATUS_STARTING);
  }

  @Test
  public void status_is_DOWN_if_platform_is_SAFEMODE_and_databaseMigration_is_FAILED() throws Exception {
    verifyStatus(Platform.Status.SAFEMODE, DatabaseMigrationState.Status.FAILED, STATUS_DOWN);
  }

  @Test
  public void status_is_STARTING_if_platform_is_STARTING_and_databaseMigration_is_NONE() throws Exception {
    verifyStatus(Platform.Status.STARTING, DatabaseMigrationState.Status.NONE, STATUS_STARTING);
  }

  @Test
  public void status_is_DB_MIGRATION_RUNNING_if_platform_is_STARTING_and_databaseMigration_is_RUNNING() throws Exception {
    verifyStatus(Platform.Status.STARTING, DatabaseMigrationState.Status.RUNNING, STATUS_MIGRATION_RUNNING);
  }

  @Test
  public void status_is_STARTING_if_platform_is_STARTING_and_databaseMigration_is_SUCCEEDED() throws Exception {
    verifyStatus(Platform.Status.STARTING, DatabaseMigrationState.Status.SUCCEEDED, STATUS_STARTING);
  }

  @Test
  public void status_is_DOWN_if_platform_is_STARTING_and_databaseMigration_is_FAILED() throws Exception {
    verifyStatus(Platform.Status.STARTING, DatabaseMigrationState.Status.FAILED, STATUS_DOWN);
  }

  @Test
  public void safety_test_for_new_platform_status() throws Exception {
    for (Platform.Status platformStatus : filter(asList(Platform.Status.values()), not(in(SUPPORTED_PLATFORM_STATUSES)))) {
      for (DatabaseMigrationState.Status databaseMigrationStatus : DatabaseMigrationState.Status.values()) {
        verifyStatus(platformStatus, databaseMigrationStatus, STATUS_DOWN);
      }
    }
  }

  @Test
  public void safety_test_for_new_databaseMigration_status_when_platform_is_SAFEMODE() {
    for (DatabaseMigrationState.Status databaseMigrationStatus : filter(asList(DatabaseMigrationState.Status.values()), not(in(SUPPORTED_DATABASE_MIGRATION_STATUSES)))) {
      when(platform.status()).thenReturn(Platform.Status.SAFEMODE);
      when(migrationState.getStatus()).thenReturn(databaseMigrationStatus);

      underTest.newRequest().execute();
    }
  }

  private void verifyStatus(Platform.Status platformStatus, DatabaseMigrationState.Status databaseMigrationStatus, String expectedStatus) {
    when(platform.status()).thenReturn(platformStatus);
    when(migrationState.getStatus()).thenReturn(databaseMigrationStatus);

    assertJson(underTest.newRequest().execute().getInput()).isSimilarTo("{" +
      "  \"status\": \"" + expectedStatus + "\"\n" +
      "}");
  }

  private static class Dummy51Server extends Server {
    @Override
    public String getId() {
      return SERVER_ID;
    }

    @Override
    public String getVersion() {
      return SERVER_VERSION;
    }

    @Override
    public Date getStartedAt() {
      throw new UnsupportedOperationException();
    }

    @Override
    public File getRootDir() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getContextPath() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getPublicRootUrl() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDev() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSecured() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getURL() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getPermanentServerId() {
      throw new UnsupportedOperationException();
    }
  }
}
