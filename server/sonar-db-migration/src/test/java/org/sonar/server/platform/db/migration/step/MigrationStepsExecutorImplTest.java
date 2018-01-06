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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.platform.db.migration.engine.MigrationContainer;
import org.sonar.server.platform.db.migration.engine.SimpleMigrationContainer;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class MigrationStepsExecutorImplTest {
  @Rule
  public LogTester logTester = new LogTester();

  private MigrationContainer migrationContainer = new SimpleMigrationContainer();
  private MigrationHistory migrationHistor = mock(MigrationHistory.class);
  private MigrationStepsExecutorImpl underTest = new MigrationStepsExecutorImpl(migrationContainer, migrationHistor);

  @Test
  public void execute_does_not_fail_when_stream_is_empty_and_log_start_stop_INFO() {
    underTest.execute(Stream.empty());

    assertThat(logTester.logs()).hasSize(2);
    assertLogLevel(LoggerLevel.INFO, "Executing DB migrations...", "Executed DB migrations: success | time=");
  }

  @Test
  public void execute_fails_with_ISE_if_no_instance_of_computation_step_exist_in_container() {
    Stream<RegisteredMigrationStep> steps = Stream.of(registeredStepOf(1, MigrationStep1.class));

    try {
      underTest.execute(steps);
      fail("execute should have thrown a IllegalStateException");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Can not find instance of " + MigrationStep1.class);
    } finally {
      assertThat(logTester.logs()).hasSize(2);
      assertLogLevel(LoggerLevel.INFO, "Executing DB migrations...");
      assertLogLevel(LoggerLevel.ERROR, "Executed DB migrations: failure | time=");
    }
  }

  private void assertLogLevel(LoggerLevel level, String... expected) {
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
  public void execute_execute_the_instance_of_type_specified_in_step_in_stream_order() {
    migrationContainer.add(MigrationStep1.class, MigrationStep2.class, MigrationStep3.class);

    underTest.execute(Stream.of(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, MigrationStep1.class),
      registeredStepOf(3, MigrationStep3.class)));

    assertThat(SingleCallCheckerMigrationStep.calledSteps)
      .containsExactly(MigrationStep2.class, MigrationStep1.class, MigrationStep3.class);
    assertThat(logTester.logs()).hasSize(8);
    assertLogLevel(LoggerLevel.INFO,
      "Executing DB migrations...",
      "#1 '1-MigrationStep2'...",
      "#1 '1-MigrationStep2': success | time=",
      "#2 '2-MigrationStep1'...",
      "#2 '2-MigrationStep1': success | time=",
      "#3 '3-MigrationStep3'...",
      "#3 '3-MigrationStep3': success | time=",
      "Executed DB migrations: success | time=");

    assertThat(migrationContainer.getComponentByType(MigrationStep1.class).isCalled()).isTrue();
    assertThat(migrationContainer.getComponentByType(MigrationStep2.class).isCalled()).isTrue();
    assertThat(migrationContainer.getComponentByType(MigrationStep3.class).isCalled()).isTrue();
  }

  @Test
  public void execute_throws_MigrationStepExecutionException_on_first_failing_step_execution_throws_SQLException() {
    migrationContainer.add(MigrationStep2.class, SqlExceptionFailingMigrationStep.class, MigrationStep3.class);
    Stream<RegisteredMigrationStep> steps = Stream.of(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, SqlExceptionFailingMigrationStep.class),
      registeredStepOf(3, MigrationStep3.class));

    try {
      underTest.execute(steps);
      fail("a MigrationStepExecutionException should have been thrown");
    } catch (MigrationStepExecutionException e) {
      assertThat(e).hasMessage("Execution of migration step #2 '2-SqlExceptionFailingMigrationStep' failed");
      assertThat(e).hasCause(SqlExceptionFailingMigrationStep.THROWN_EXCEPTION);
    } finally {
      assertThat(logTester.logs()).hasSize(6);
      assertLogLevel(LoggerLevel.INFO,
        "Executing DB migrations...",
        "#1 '1-MigrationStep2'...",
        "#1 '1-MigrationStep2': success | time=",
        "#2 '2-SqlExceptionFailingMigrationStep'...");
      assertLogLevel(LoggerLevel.ERROR,
        "#2 '2-SqlExceptionFailingMigrationStep': failure | time=",
        "Executed DB migrations: failure | time=");
    }
  }

  @Test
  public void execute_throws_MigrationStepExecutionException_on_first_failing_step_execution_throws_any_exception() {
    migrationContainer.add(MigrationStep2.class, RuntimeExceptionFailingMigrationStep.class, MigrationStep3.class);

    Stream<RegisteredMigrationStep> steps = Stream.of(
      registeredStepOf(1, MigrationStep2.class),
      registeredStepOf(2, RuntimeExceptionFailingMigrationStep.class),
      registeredStepOf(3, MigrationStep3.class));

    try {
      underTest.execute(steps);
      fail("should throw MigrationStepExecutionException");
    } catch (MigrationStepExecutionException e) {
      assertThat(e).hasMessage("Execution of migration step #2 '2-RuntimeExceptionFailingMigrationStep' failed");
      assertThat(e.getCause()).isSameAs(RuntimeExceptionFailingMigrationStep.THROWN_EXCEPTION);
    }
  }

  private static RegisteredMigrationStep registeredStepOf(int migrationNumber, Class<? extends MigrationStep> migrationStep1Class) {
    return new RegisteredMigrationStep(migrationNumber, migrationNumber + "-" + migrationStep1Class.getSimpleName(), migrationStep1Class);
  }

  private static abstract class SingleCallCheckerMigrationStep implements MigrationStep {
    private static List<Class<? extends MigrationStep>> calledSteps = new ArrayList<>();
    private boolean called = false;

    @Override
    public void execute() throws SQLException {
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
    public void execute() throws SQLException {
      throw THROWN_EXCEPTION;
    }
  }

}
