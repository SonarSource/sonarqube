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
package org.sonar.server.db.migrations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.server.ruby.RubyBridge;
import org.sonar.server.ruby.RubyDatabaseMigration;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for PlatformDatabaseMigration which does not test any of its concurrency management and asynchronous execution code.
 */
public class PlatformDatabaseMigrationTest {
  private static final Throwable AN_ERROR = new RuntimeException();

  /**
   * Implementation of execute runs Runnable synchronously.
   */
  private PlatformDatabaseMigrationExecutorService executorService = new PlatformDatabaseMigrationExecutorServiceAdaptor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };
  @Mock
  private RubyDatabaseMigration rubyDatabaseMigration;
  @Mock
  private RubyBridge rubyBridge;
  private PlatformDatabaseMigration underTest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    underTest = new PlatformDatabaseMigration(rubyBridge, executorService);
  }

  @Test
  public void status_is_NONE_when_component_is_created() throws Exception {
    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.NONE);
  }

  @Test
  public void startedAt_is_null_when_component_is_created() throws Exception {
    assertThat(underTest.startedAt()).isNull();
  }

  @Test
  public void failureError_is_null_when_component_is_created() throws Exception {
    assertThat(underTest.failureError()).isNull();
  }

  @Test
  public void startit_calls_databasemigration_trigger_in_a_separate_thread() throws Exception {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);

    underTest.startIt();

    verify(rubyBridge).databaseMigration();
    verify(rubyDatabaseMigration).trigger();
  }

  @Test
  public void status_is_SUCCEEDED_and_failure_is_null_when_trigger_runs_without_an_exception() throws Exception {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);

    underTest.startIt();

    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.SUCCEEDED);
    assertThat(underTest.failureError()).isNull();
    assertThat(underTest.startedAt()).isNotNull();
  }

  @Test
  public void status_is_FAILED_and_failure_stores_the_exception_when_trigger_throws_an_exception() throws Exception {
    mockTriggerThrowsError();

    underTest.startIt();

    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.FAILED);
    assertThat(underTest.failureError()).isSameAs(AN_ERROR);
    assertThat(underTest.startedAt()).isNotNull();
  }

  @Test
  public void successive_calls_to_startIt_reset_status_startedAt_and_failureError() throws Exception {
    mockTriggerThrowsError();

    underTest.startIt();

    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.FAILED);
    assertThat(underTest.failureError()).isSameAs(AN_ERROR);
    Date firstStartDate = underTest.startedAt();
    assertThat(firstStartDate).isNotNull();

    mockTriggerDoesNothing();

    underTest.startIt();

    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.SUCCEEDED);
    assertThat(underTest.failureError()).isNull();
    assertThat(underTest.startedAt()).isNotSameAs(firstStartDate);
  }

  private void mockTriggerThrowsError() {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);
    doThrow(AN_ERROR).when(rubyDatabaseMigration).trigger();
  }

  private void mockTriggerDoesNothing() {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);
    doNothing().when(rubyDatabaseMigration).trigger();
  }
}
