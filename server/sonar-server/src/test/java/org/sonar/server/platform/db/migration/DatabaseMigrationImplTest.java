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
package org.sonar.server.platform.db.migration;

import java.util.Date;
import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Unit test for DatabaseMigrationImpl which does not test any of its concurrency management and asynchronous execution code.
 */
public class DatabaseMigrationImplTest {
  private static final Throwable AN_ERROR = new RuntimeException("runtime exception created on purpose");

  /**
   * Implementation of execute runs Runnable synchronously.
   */
  private DatabaseMigrationExecutorService executorService = new DatabaseMigrationExecutorServiceAdaptor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };
  private MutableDatabaseMigrationState migrationState = new DatabaseMigrationStateImpl();
  private Platform platform = mock(Platform.class);
  private MigrationEngine migrationEngine = mock(MigrationEngine.class);
  private InOrder inOrder = inOrder(platform, migrationEngine);

  private DatabaseMigrationImpl underTest = new DatabaseMigrationImpl(executorService, migrationState, migrationEngine, platform);

  @Test
  public void startit_calls_MigrationEngine_execute() {
    underTest.startIt();

    inOrder.verify(migrationEngine).execute();
    inOrder.verify(platform).doStart();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void status_is_SUCCEEDED_and_failure_is_null_when_trigger_runs_without_an_exception() {
    underTest.startIt();

    assertThat(migrationState.getStatus()).isEqualTo(DatabaseMigrationState.Status.SUCCEEDED);
    assertThat(migrationState.getError()).isNull();
    assertThat(migrationState.getStartedAt()).isNotNull();
  }

  @Test
  public void status_is_FAILED_and_failure_stores_the_exception_when_trigger_throws_an_exception() {
    mockMigrationThrowsError();

    underTest.startIt();

    assertThat(migrationState.getStatus()).isEqualTo(DatabaseMigrationState.Status.FAILED);
    assertThat(migrationState.getError()).isSameAs(AN_ERROR);
    assertThat(migrationState.getStartedAt()).isNotNull();
  }

  @Test
  public void successive_calls_to_startIt_reset_status_startedAt_and_failureError() {
    mockMigrationThrowsError();

    underTest.startIt();

    assertThat(migrationState.getStatus()).isEqualTo(DatabaseMigrationState.Status.FAILED);
    assertThat(migrationState.getError()).isSameAs(AN_ERROR);
    Date firstStartDate = migrationState.getStartedAt();
    assertThat(firstStartDate).isNotNull();

    mockMigrationDoesNothing();

    underTest.startIt();

    assertThat(migrationState.getStatus()).isEqualTo(DatabaseMigrationState.Status.SUCCEEDED);
    assertThat(migrationState.getError()).isNull();
    assertThat(migrationState.getStartedAt()).isNotSameAs(firstStartDate);
  }

  private void mockMigrationThrowsError() {
    doThrow(AN_ERROR).when(migrationEngine).execute();
  }

  private void mockMigrationDoesNothing() {
    doNothing().when(migrationEngine).execute();
  }
}
