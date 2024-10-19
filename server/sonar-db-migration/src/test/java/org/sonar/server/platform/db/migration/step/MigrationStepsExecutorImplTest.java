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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.db.migration.MutableDatabaseMigrationState;
import org.sonar.server.platform.db.migration.engine.MigrationContainer;
import org.sonar.server.platform.db.migration.engine.SimpleMigrationContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.telemetry.TelemetryDbMigrationStepDurationProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationSuccessProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationStepsProvider;
import org.sonar.server.telemetry.TelemetryDbMigrationTotalTimeProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MigrationStepsExecutorImplTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final MigrationContainer migrationContainer = new SimpleMigrationContainer();
  private final MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private final MutableDatabaseMigrationState databaseMigrationState = mock();
  private final TelemetryDbMigrationTotalTimeProvider telemetryDbMigrationTotalTimeProvider = new TelemetryDbMigrationTotalTimeProvider();
  private final TelemetryDbMigrationStepsProvider telemetryDbMigrationStepsProvider = new TelemetryDbMigrationStepsProvider();
  private final TelemetryDbMigrationSuccessProvider telemetryDbMigrationSuccessProvider = new TelemetryDbMigrationSuccessProvider();
  private final TelemetryDbMigrationStepDurationProvider telemetryDbMigrationStepDurationProvider = new TelemetryDbMigrationStepDurationProvider();
  private final MigrationStepsExecutorImpl underTest = new MigrationStepsExecutorImpl(migrationContainer, migrationHistory, databaseMigrationState,
    telemetryDbMigrationTotalTimeProvider, telemetryDbMigrationStepsProvider, telemetryDbMigrationSuccessProvider, telemetryDbMigrationStepDurationProvider);
  private final NoOpMigrationStatusListener migrationStatusListener = mock();

  @BeforeEach
  void setUp() {
    when(databaseMigrationState.getTotalMigrations()).thenReturn(5);
    when(databaseMigrationState.getCompletedMigrations()).thenReturn(2);
  }

  @Test
  void execute_does_not_fail_when_stream_is_empty_and_log_start_stop_INFO() {
    underTest.execute(Collections.emptyList(), migrationStatusListener);

    verifyNoInteractions(migrationStatusListener);
    assertThat(logTester.logs()).hasSize(2);
    assertLogLevel(Level.INFO, "Executing 5 DB migrations...", "Executed 2/5 DB migrations: success | time=");
  }

  @Test
  void execute_fails_with_ISE_if_no_instance_of_computation_step_exist_in_container() {
    List<RegisteredMigrationStep> steps = asList(registeredStepOf(1, MigrationStep1.class));

    ((SpringComponentContainer) migrationContainer).startComponents();
    try {
      underTest.execute(steps, migrationStatusListener);
      fail("execute should have thrown a IllegalStateException");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unable to load component " + MigrationStep1.class);
    } finally {
      verifyNoInteractions(migrationStatusListener);
      assertThat(logTester.logs()).hasSize(2);
      assertLogLevel(Level.INFO, "Executing 5 DB migrations...");
      assertLogLevel(Level.ERROR, "Executed 2/5 DB migrations: failure | time=");
    }
  }

  private void assertLogLevel(Level level, String... expected) {
    List<String> logs = logTester.logs(level);
    assertThat(logs).hasSize(expected.length);
    Iterator<String> iterator = logs.iterator();
    Arrays.stream(expected).forEachOrdered(log -> {
      if (log.endsWith(" | time=")) {
        assertThat(iterator.next()).startsWith(log);
      } else {
        assertThat(iterator.next()).isEqualTo(log);
      }
    });
  }

  @Test
  void execute_execute_the_instance_of_type_specified_in_step_in_stream_order() {
    migrationContainer.add(MigrationStep1.class, MigrationStep2.class, MigrationStep3.class);
    ((SpringComponentContainer) migrationContainer).startComponents();

    underTest.execute(asList(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, MigrationStep1.class),
      registeredStepOf(3, MigrationStep3.class)),
      migrationStatusListener);

    assertThat(SingleCallCheckerMigrationStep.calledSteps)
      .containsExactly(MigrationStep2.class, MigrationStep1.class, MigrationStep3.class);
    assertThat(logTester.logs()).hasSize(8);
    assertLogLevel(Level.INFO,
      "Executing 5 DB migrations...",
      "3/5 #1 '1-MigrationStep2'...",
      "3/5 #1 '1-MigrationStep2': success | time=",
      "3/5 #2 '2-MigrationStep1'...",
      "3/5 #2 '2-MigrationStep1': success | time=",
      "3/5 #3 '3-MigrationStep3'...",
      "3/5 #3 '3-MigrationStep3': success | time=",
      "Executed 2/5 DB migrations: success | time=");

    assertThat(migrationContainer.getComponentByType(MigrationStep1.class).isCalled()).isTrue();
    assertThat(migrationContainer.getComponentByType(MigrationStep2.class).isCalled()).isTrue();
    assertThat(migrationContainer.getComponentByType(MigrationStep3.class).isCalled()).isTrue();
    verify(migrationStatusListener, times(3)).onMigrationStepCompleted();

  }

  @Test
  void execute_throws_MigrationStepExecutionException_on_first_failing_step_execution_throws_SQLException() {
    migrationContainer.add(MigrationStep2.class, SqlExceptionFailingMigrationStep.class, MigrationStep3.class);
    List<RegisteredMigrationStep> steps = asList(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, SqlExceptionFailingMigrationStep.class),
      registeredStepOf(3, MigrationStep3.class));

    ((SpringComponentContainer) migrationContainer).startComponents();
    try {
      underTest.execute(steps, migrationStatusListener);
      fail("a MigrationStepExecutionException should have been thrown");
    } catch (MigrationStepExecutionException e) {
      assertThat(e).hasMessage("Execution of migration step #2 '2-SqlExceptionFailingMigrationStep' failed");
      assertThat(e).hasCause(SqlExceptionFailingMigrationStep.THROWN_EXCEPTION);
    } finally {
      assertThat(logTester.logs()).hasSize(6);
      assertLogLevel(Level.INFO,
        "Executing 5 DB migrations...",
        "3/5 #1 '1-MigrationStep2'...",
        "3/5 #1 '1-MigrationStep2': success | time=",
        "3/5 #2 '2-SqlExceptionFailingMigrationStep'...");
      assertLogLevel(Level.ERROR,
        "3/5 #2 '2-SqlExceptionFailingMigrationStep': failure | time=",
        "Executed 2/5 DB migrations: failure | time=");
    }
    verify(migrationStatusListener, times(1)).onMigrationStepCompleted();
  }

  @Test
  void execute_throws_MigrationStepExecutionException_on_first_failing_step_execution_throws_any_exception() {
    migrationContainer.add(MigrationStep2.class, RuntimeExceptionFailingMigrationStep.class, MigrationStep3.class);

    List<RegisteredMigrationStep> steps = asList(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, RuntimeExceptionFailingMigrationStep.class),
      registeredStepOf(3, MigrationStep3.class));

    ((SpringComponentContainer) migrationContainer).startComponents();
    try {
      underTest.execute(steps, migrationStatusListener);
      fail("should throw MigrationStepExecutionException");
    } catch (MigrationStepExecutionException e) {
      assertThat(e).hasMessage("Execution of migration step #2 '2-RuntimeExceptionFailingMigrationStep' failed");
      assertThat(e.getCause()).isSameAs(RuntimeExceptionFailingMigrationStep.THROWN_EXCEPTION);
      verify(migrationStatusListener, times(1)).onMigrationStepCompleted();
    }
  }

  @Test
  void whenExecute_TelemetryDataIsProperlyAdded() {
    migrationContainer.add(MigrationStep2.class, MigrationStep1.class, MigrationStep3.class);
    when(databaseMigrationState.getCompletedMigrations()).thenReturn(3);

    List<RegisteredMigrationStep> steps = asList(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, MigrationStep1.class),
      registeredStepOf(3, MigrationStep3.class));

    ((SpringComponentContainer) migrationContainer).startComponents();

    underTest.execute(steps, migrationStatusListener);

    assertThat(telemetryDbMigrationTotalTimeProvider.getValue().get()).isPositive();
    assertThat(telemetryDbMigrationStepsProvider.getValue()).hasValue(3);
    assertThat(telemetryDbMigrationSuccessProvider.getValue()).hasValue(true);

  }

  private static RegisteredMigrationStep registeredStepOf(int migrationNumber, Class<? extends MigrationStep> migrationStep1Class) {
    return new RegisteredMigrationStep(migrationNumber, migrationNumber + "-" + migrationStep1Class.getSimpleName(), migrationStep1Class);
  }

  private static abstract class SingleCallCheckerMigrationStep implements MigrationStep {
    private static List<Class<? extends MigrationStep>> calledSteps = new ArrayList<>();
    private boolean called = false;

    @Override
    public void execute() {
      checkState(!called, "execute must not be called twice");
      this.called = true;
      calledSteps.add(getClass());
    }

    public boolean isCalled() {
      return called;
    }

    public static List<Class<? extends MigrationStep>> getCalledSteps() {
      return calledSteps;
    }
  }

  public static final class MigrationStep1 extends SingleCallCheckerMigrationStep {

  }

  public static final class MigrationStep2 extends SingleCallCheckerMigrationStep {

  }

  public static final class MigrationStep3 extends SingleCallCheckerMigrationStep {

  }

  public static class SqlExceptionFailingMigrationStep implements MigrationStep {
    private static final SQLException THROWN_EXCEPTION = new SQLException("Faking SQL exception in MigrationStep#execute()");

    @Override
    public void execute() throws SQLException {
      throw THROWN_EXCEPTION;
    }
  }

  public static class RuntimeExceptionFailingMigrationStep implements MigrationStep {
    private static final RuntimeException THROWN_EXCEPTION = new RuntimeException("Faking failing migration step");

    @Override
    public void execute() {
      throw THROWN_EXCEPTION;
    }
  }

}
