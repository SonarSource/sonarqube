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
package org.sonar.server.computation.task.step;

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.computation.task.ChangeLogLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ComputationStepExecutorTest {
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final ComputationStepExecutor.Listener listener = mock(ComputationStepExecutor.Listener.class);
  private final ComputationStep computationStep1 = mockComputationStep("step1");
  private final ComputationStep computationStep2 = mockComputationStep("step2");
  private final ComputationStep computationStep3 = mockComputationStep("step3");

  @Test
  public void execute_call_execute_on_each_ComputationStep_in_order_returned_by_instances_method() {
    new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2, computationStep3))
      .execute();

    InOrder inOrder = inOrder(computationStep1, computationStep2, computationStep3);
    inOrder.verify(computationStep1).execute();
    inOrder.verify(computationStep1).getDescription();
    inOrder.verify(computationStep2).execute();
    inOrder.verify(computationStep2).getDescription();
    inOrder.verify(computationStep3).execute();
    inOrder.verify(computationStep3).getDescription();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void execute_let_exception_thrown_by_ComputationStep_go_up_as_is() {
    String message = "Exception should go up";

    ComputationStep computationStep = mockComputationStep("step1");
    doThrow(new RuntimeException(message))
      .when(computationStep)
      .execute();

    ComputationStepExecutor computationStepExecutor = new ComputationStepExecutor(mockComputationSteps(computationStep));

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage(message);

    computationStepExecutor.execute();
  }

  @Test
  public void execute_does_not_log_end_timing_for_each_ComputationStep_called_when_level_is_INFO() {
    List<String> infoLogs = execute_logs_end_timing_for_each_ComputationStep_called_when_(LoggerLevel.INFO);
    assertThat(infoLogs).isEmpty();
  }

  @Test
  public void execute_logs_end_timing_for_each_ComputationStep_called_when_level_is_DEBUG() {
    List<String> infoLogs = execute_logs_end_timing_for_each_ComputationStep_called_when_(LoggerLevel.DEBUG);
    assertThat(infoLogs).hasSize(2);
    assertThat(infoLogs.get(0)).contains("step1 | time=");
    assertThat(infoLogs.get(1)).contains("step2 | time=");
  }

  @Test
  public void execute_logs_end_timing_for_each_ComputationStep_called_when_level_is_TRACE() {
    List<String> infoLogs = execute_logs_end_timing_for_each_ComputationStep_called_when_(LoggerLevel.TRACE);
    assertThat(infoLogs).hasSize(2);
    assertThat(infoLogs.get(0)).contains("step1 | time=");
    assertThat(infoLogs.get(1)).contains("step2 | time=");
  }

  private List<String> execute_logs_end_timing_for_each_ComputationStep_called_when_(LoggerLevel level) {
    try (ChangeLogLevel executor = new ChangeLogLevel(ComputationStepExecutor.class, level);
      ChangeLogLevel step1 = new ChangeLogLevel(computationStep1.getClass(), level);
      ChangeLogLevel step2 = new ChangeLogLevel(computationStep2.getClass(), level)) {
      new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2))
        .execute();

      return logTester.logs(LoggerLevel.DEBUG);
    }
  }

  @Test
  public void execute_calls_listener_finished_method_with_all_step_runs() {
    new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2), listener)
      .execute();

    verify(listener).finished(true);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void execute_calls_listener_finished_method_even_if_a_step_throws_an_exception() {
    RuntimeException toBeThrown = new RuntimeException("simulating failing execute Step method");
    doThrow(toBeThrown)
      .when(computationStep1)
      .execute();

    try {
      new ComputationStepExecutor(mockComputationSteps(computationStep1, computationStep2), listener)
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

    new ComputationStepExecutor(mockComputationSteps(computationStep1), listener).execute();
  }

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
}
