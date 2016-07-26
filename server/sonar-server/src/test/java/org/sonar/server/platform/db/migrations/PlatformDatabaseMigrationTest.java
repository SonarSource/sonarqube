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
package org.sonar.server.platform.db.migrations;

import java.util.Date;
import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.db.version.DatabaseMigration;
import org.sonar.server.platform.Platform;
import org.sonar.server.ruby.RubyBridge;
import org.sonar.server.ruby.RubyDatabaseMigration;
import org.sonar.server.ruby.RubyRailsRoutes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for PlatformDatabaseMigration which does not test any of its concurrency management and asynchronous execution code.
 */
public class PlatformDatabaseMigrationTest {
  private static final Throwable AN_ERROR = new RuntimeException("runtime exception created on purpose");

  /**
   * Implementation of execute runs Runnable synchronously.
   */
  PlatformDatabaseMigrationExecutorService executorService = new PlatformDatabaseMigrationExecutorServiceAdaptor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };
  RubyBridge rubyBridge = mock(RubyBridge.class);
  RubyDatabaseMigration rubyDatabaseMigration = mock(RubyDatabaseMigration.class);
  RubyRailsRoutes rubyRailsRoutes = mock(RubyRailsRoutes.class);
  Platform platform = mock(Platform.class);
  InOrder inOrder = inOrder(rubyDatabaseMigration, rubyBridge, rubyRailsRoutes, platform);

  PlatformDatabaseMigration underTest = new PlatformDatabaseMigration(rubyBridge, executorService, platform);

  @Test
  public void status_is_NONE_when_component_is_created() {
    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.NONE);
  }

  @Test
  public void startedAt_is_null_when_component_is_created() {
    assertThat(underTest.startedAt()).isNull();
  }

  @Test
  public void failureError_is_null_when_component_is_created() {
    assertThat(underTest.failureError()).isNull();
  }

  @Test
  public void startit_calls_databasemigration_trigger_in_a_separate_thread() {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);
    when(rubyBridge.railsRoutes()).thenReturn(rubyRailsRoutes);

    underTest.startIt();

    inOrder.verify(rubyBridge).databaseMigration();
    inOrder.verify(rubyDatabaseMigration).trigger();
    inOrder.verify(platform).doStart();
    inOrder.verify(rubyBridge).railsRoutes();
    inOrder.verify(rubyRailsRoutes).recreate();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void status_is_SUCCEEDED_and_failure_is_null_when_trigger_runs_without_an_exception() {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);
    when(rubyBridge.railsRoutes()).thenReturn(rubyRailsRoutes);

    underTest.startIt();

    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.SUCCEEDED);
    assertThat(underTest.failureError()).isNull();
    assertThat(underTest.startedAt()).isNotNull();
  }

  @Test
  public void status_is_FAILED_and_failure_stores_the_exception_when_trigger_throws_an_exception() {
    mockTriggerThrowsError();

    underTest.startIt();

    assertThat(underTest.status()).isEqualTo(DatabaseMigration.Status.FAILED);
    assertThat(underTest.failureError()).isSameAs(AN_ERROR);
    assertThat(underTest.startedAt()).isNotNull();
  }

  @Test
  public void successive_calls_to_startIt_reset_status_startedAt_and_failureError() {
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
    when(rubyBridge.railsRoutes()).thenReturn(rubyRailsRoutes);
  }

  private void mockTriggerDoesNothing() {
    when(rubyBridge.databaseMigration()).thenReturn(rubyDatabaseMigration);
    doNothing().when(rubyDatabaseMigration).trigger();
    when(rubyBridge.railsRoutes()).thenReturn(rubyRailsRoutes);
  }
}
