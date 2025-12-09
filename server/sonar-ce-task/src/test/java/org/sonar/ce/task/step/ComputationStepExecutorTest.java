/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.step;

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.CeTaskInterrupter;
import org.sonar.ce.task.ChangeLogLevel;
import org.sonar.ce.task.telemetry.MutableStepsTelemetryHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ComputationStepExecutorTest {
  private final ComputationStepExecutor.Listener listener = mock(ComputationStepExecutor.Listener.class);
  private final MutableStepsTelemetryHolder stepsTelemetryHolder = new TestComputationStepContext.TestTelemetryMetrics();
  private final CeTaskInterrupter taskInterrupter = mock(CeTaskInterrupter.class);
  private final ComputationStep computationStep1 = mockComputationStep("step1");
  private final ComputationStep computationStep2 = mockComputationStep("step2");
  private final ComputationStep computationStep3 = mockComputationStep("step3");
  @Rule
  public LogTester logTester = new LogTester();

  private static ComputationSteps mockComputationSteps(ComputationStep... computationSteps) {
    ComputationSteps steps = mock(ComputationSteps.class);
    when(steps.instances()).thenReturn(Arrays.asList(computationSteps));
    return steps;
  }

  private static ComputationStep mockComputationStep(String desc) {
    ComputationStep mock = mock(ComputationStep.class);
    when(mock.getDescription()).thenReturn(desc);
    return mock;
  }

  @Test
  public void execute_call_execute_on_each_ComputationStep_in_order_returned_by_instances_method() {
    new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2, computationStep3), taskInterrupter)
      .execute();

    InOrder inOrder = inOrder(computationStep1, computationStep2, computationStep3);
    inOrder.verify(computationStep1).execute(any());
    inOrder.verify(computationStep1).getDescription();
    inOrder.verify(computationStep2).execute(any());
    inOrder.verify(computationStep2).getDescription();
    inOrder.verify(computationStep3).execute(any());
    inOrder.verify(computationStep3).getDescription();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void execute_call_execute_on_ComputationStepWithTelemetryMetricOnly() {
    ComputationStep step = new StepWithTelemetryMetricOnly("Step", "step.foo", "100", "step.bar", "20");

    new ComputationStepExecutor(mockComputationSteps(step), taskInterrupter, stepsTelemetryHolder, listener)
      .execute();

    assertThat(stepsTelemetryHolder.getTelemetryMetrics()).containsEntry("prefix.step.foo", "100");
    assertThat(stepsTelemetryHolder.getTelemetryMetrics()).containsEntry("prefix.step.bar", "20");
  }

  @Test
  public void execute_call_execute_on_ComputationStepWithTelemetryWithStatistic() {
    ComputationStep step = new StepWithTelemetrywithStatistic("Step", "step.foo", "100", "step.bar", "20");

    new ComputationStepExecutor(mockComputationSteps(step), taskInterrupter, stepsTelemetryHolder, listener)
      .execute();

    assertThat(stepsTelemetryHolder.getTelemetryMetrics()).containsEntry("prefix.step.foo", "100");
    assertThat(stepsTelemetryHolder.getTelemetryMetrics()).containsEntry("prefix.step.bar", "20");
    List<String> infoLogs = logTester.logs(Level.INFO);
    assertThat(infoLogs).hasSize(1);
    assertThat(infoLogs.get(0)).contains("Step | step.foo=100 | step.bar=20 | status=SUCCESS | time=");
  }

  @Test
  public void execute_let_exception_thrown_by_ComputationStep_go_up_as_is() {
    String message = "Exception should go up";

    ComputationStep computationStep = mockComputationStep("step1");
    doThrow(new RuntimeException(message))
      .when(computationStep)
      .execute(any());

    ComputationStepExecutor computationStepExecutor = new ComputationStepExecutor(mockComputationSteps(computationStep), taskInterrupter);

    assertThatThrownBy(computationStepExecutor::execute)
      .isInstanceOf(RuntimeException.class)
      .hasMessage(message);
  }

  @Test
  public void execute_logs_end_timing_and_statistics_for_each_ComputationStep_in_INFO_level() {
    ComputationStep step1 = new StepWithStatistics("Step One", "foo", "100", "bar", "20");
    ComputationStep step2 = new StepWithStatistics("Step Two", "foo", "50", "baz", "10");
    ComputationStep step3 = new StepWithStatistics("Step Three");

    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, LoggerLevel.INFO);
      ChangeLogLevel logLevel1 = new ChangeLogLevel(step1.getClass(), LoggerLevel.INFO);
      ChangeLogLevel logLevel2 = new ChangeLogLevel(step2.getClass(), LoggerLevel.INFO);
      ChangeLogLevel logLevel3 = new ChangeLogLevel(step3.getClass(), LoggerLevel.INFO)) {
      new ComputationStepExecutor(mockComputationSteps(step1, step2, step3), taskInterrupter).execute();

      List<String> infoLogs = logTester.logs(Level.INFO);
      assertThat(infoLogs).hasSize(3);
      assertThat(infoLogs.get(0)).contains("Step One | foo=100 | bar=20 | status=SUCCESS | time=");
      assertThat(infoLogs.get(1)).contains("Step Two | foo=50 | baz=10 | status=SUCCESS | time=");
      assertThat(infoLogs.get(2)).contains("Step Three | status=SUCCESS | time=");
    }
  }

  @Test
  public void execute_logs_end_timing_and_statistics_for_each_ComputationStep_in_INFO_level_even_if_failed() {
    RuntimeException expected = new RuntimeException("faking step failing with RuntimeException");
    ComputationStep step1 = new StepWithStatistics("Step One", "foo", "100", "bar", "20");
    ComputationStep step2 = new StepWithStatistics("Step Two", "foo", "50", "baz", "10");
    ComputationStep step3 = new StepWithStatistics("Step Three", "donut", "crash") {
      @Override
      public void execute(Context context) {
        super.execute(context);
        throw expected;
      }
    };

    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, LoggerLevel.INFO);
      ChangeLogLevel logLevel1 = new ChangeLogLevel(step1.getClass(), LoggerLevel.INFO);
      ChangeLogLevel logLevel2 = new ChangeLogLevel(step2.getClass(), LoggerLevel.INFO);
      ChangeLogLevel logLevel3 = new ChangeLogLevel(step3.getClass(), LoggerLevel.INFO)) {

      try {
        new ComputationStepExecutor(mockComputationSteps(step1, step2, step3), taskInterrupter).execute();
        fail("a RuntimeException should have been thrown");
      } catch (RuntimeException e) {
        List<String> infoLogs = logTester.logs(Level.INFO);
        assertThat(infoLogs).hasSize(3);
        assertThat(infoLogs.get(0)).contains("Step One | foo=100 | bar=20 | status=SUCCESS | time=");
        assertThat(infoLogs.get(1)).contains("Step Two | foo=50 | baz=10 | status=SUCCESS | time=");
        assertThat(infoLogs.get(2)).contains("Step Three | donut=crash | status=FAILED | time=");
      }
    }
  }

  @Test
  public void execute_throws_IAE_if_step_adds_time_statistic() {
    ComputationStep step = new StepWithStatistics("A Step", "foo", "100", "time", "20");

    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, LoggerLevel.INFO)) {
      assertThatThrownBy(() -> new ComputationStepExecutor(mockComputationSteps(step), taskInterrupter).execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Statistic with key [time] is not accepted");
    }
  }

  @Test
  public void execute_throws_IAE_if_step_adds_statistic_multiple_times() {
    ComputationStep step = new StepWithStatistics("A Step", "foo", "100", "foo", "20");

    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, LoggerLevel.INFO)) {
      assertThatThrownBy(() -> new ComputationStepExecutor(mockComputationSteps(step), taskInterrupter).execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Statistic with key [foo] is already present");
    }
  }

  @Test
  public void execute_throws_NPE_if_step_adds_statistic_with_null_key() {
    ComputationStep step = new StepWithStatistics("A Step", "foo", "100", null, "bar");

    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, LoggerLevel.INFO)) {
      assertThatThrownBy(() -> new ComputationStepExecutor(mockComputationSteps(step), taskInterrupter).execute())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Statistic has null key");
    }
  }

  @Test
  public void execute_throws_NPE_if_step_adds_statistic_with_null_value() {
    ComputationStep step = new StepWithStatistics("A Step", "foo", "100", "bar", null);

    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, LoggerLevel.INFO)) {
      assertThatThrownBy(() -> new ComputationStepExecutor(mockComputationSteps(step), taskInterrupter).execute())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Statistic with key [bar] has null value");
    }
  }

  @Test
  public void execute_calls_listener_finished_method_with_all_step_runs() {
    new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2), taskInterrupter, stepsTelemetryHolder, listener)
      .execute();

    verify(listener).finished(true);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void execute_calls_listener_finished_method_even_if_a_step_throws_an_exception() {
    RuntimeException toBeThrown = new RuntimeException("simulating failing execute Step method");
    doThrow(toBeThrown)
      .when(computationStep1)
      .execute(any());

    try {
      new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2), taskInterrupter, stepsTelemetryHolder, listener)
        .execute();
      fail("exception toBeThrown should have been raised");
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(toBeThrown);
      verify(listener).finished(false);
      verifyNoMoreInteractions(listener);
    }
  }

  @Test
  public void execute_does_not_fail_if_listener_throws_Throwable() {
    ComputationStepExecutor.Listener listener = mock(ComputationStepExecutor.Listener.class);
    doThrow(new Error("Facking error thrown by Listener"))
      .when(listener)
      .finished(anyBoolean());

    new ComputationStepExecutor(mockComputationSteps(computationStep1), taskInterrupter, stepsTelemetryHolder, listener).execute();
  }

  @Test
  public void execute_fails_with_exception_thrown_by_interrupter() throws Throwable {
    executeFailsWithExceptionThrownByInterrupter();

    reset(computationStep1, computationStep2, computationStep3, taskInterrupter);
    runInOtherThread(this::executeFailsWithExceptionThrownByInterrupter);
  }

  private void executeFailsWithExceptionThrownByInterrupter() {
    Thread currentThread = Thread.currentThread();
    ComputationStepExecutor underTest = new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2, computationStep3), taskInterrupter);
    RuntimeException exception = new RuntimeException("mocking fail of method check()");
    doNothing()
      .doNothing()
      .doThrow(exception)
      .when(taskInterrupter)
      .check(currentThread);

    try {
      underTest.execute();
      fail("execute should have thrown an exception");
    } catch (Exception e) {
      assertThat(e).isSameAs(exception);
    }
  }

  @Test
  public void execute_calls_interrupter_with_current_thread_before_each_step() throws Throwable {
    executeCallsInterrupterWithCurrentThreadBeforeEachStep();

    reset(computationStep1, computationStep2, computationStep3, taskInterrupter);
    runInOtherThread(this::executeCallsInterrupterWithCurrentThreadBeforeEachStep);
  }

  private void executeCallsInterrupterWithCurrentThreadBeforeEachStep() {
    InOrder inOrder = inOrder(computationStep1, computationStep2, computationStep3, taskInterrupter);
    ComputationStepExecutor underTest = new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2, computationStep3), taskInterrupter);

    underTest.execute();

    inOrder.verify(taskInterrupter).check(Thread.currentThread());
    inOrder.verify(computationStep1).execute(any());
    inOrder.verify(computationStep1).getDescription();
    inOrder.verify(taskInterrupter).check(Thread.currentThread());
    inOrder.verify(computationStep2).execute(any());
    inOrder.verify(computationStep2).getDescription();
    inOrder.verify(taskInterrupter).check(Thread.currentThread());
    inOrder.verify(computationStep3).execute(any());
    inOrder.verify(computationStep3).getDescription();
    inOrder.verifyNoMoreInteractions();
  }

  private void runInOtherThread(Runnable r) throws Throwable {
    Throwable[] otherThreadException = new Throwable[1];
    Thread t = new Thread(() -> {
      try {
        r.run();
      } catch (Throwable e) {
        otherThreadException[0] = e;
      }
    });
    t.start();
    t.join();

    if (otherThreadException[0] != null) {
      throw otherThreadException[0];
    }
  }

  private static class StepWithStatistics implements ComputationStep {
    private final String description;
    private final String[] statistics;

    private StepWithStatistics(String description, String... statistics) {
      this.description = description;
      this.statistics = statistics;
    }

    @Override
    public void execute(Context context) {
      for (int i = 0; i < statistics.length; i += 2) {
        context.getStatistics().add(statistics[i], statistics[i + 1]);
      }
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  private static class StepWithTelemetryMetricOnly implements ComputationStep {
    private final String description;
    private final String[] metrics;

    private StepWithTelemetryMetricOnly(String description, String... metrics) {
      this.description = description;
      this.metrics = metrics;
    }

    @Override
    public void execute(Context context) {
      for (int i = 0; i < metrics.length; i += 2) {
        context.addTelemetryMetricOnly("prefix", metrics[i], metrics[i + 1]);
      }
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  private static class StepWithTelemetrywithStatistic implements ComputationStep {
    private final String description;
    private final String[] metrics;

    private StepWithTelemetrywithStatistic(String description, String... metrics) {
      this.description = description;
      this.metrics = metrics;
    }

    @Override
    public void execute(Context context) {
      for (int i = 0; i < metrics.length; i += 2) {
        context.addTelemetryWithStatistic("prefix", metrics[i], metrics[i + 1]);
      }
    }

    @Override
    public String getDescription() {
      return description;
    }
  }
}
