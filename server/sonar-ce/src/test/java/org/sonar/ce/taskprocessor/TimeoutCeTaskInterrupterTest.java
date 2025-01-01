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
package org.sonar.ce.taskprocessor;

import java.util.Optional;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskCanceledException;
import org.sonar.ce.task.CeTaskTimeoutException;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeoutCeTaskInterrupterTest {
  @Rule
  public LogTester logTester = new LogTester();

  private int timeoutInSeconds = 1 + new Random().nextInt(20);
  private int timeoutInMs = timeoutInSeconds * 1_000;
  private CeWorkerController ceWorkerController = mock(CeWorkerController.class);
  private System2 system2 = mock(System2.class);
  private CeWorker ceWorker = mock(CeWorker.class);
  private CeTask ceTask = mock(CeTask.class);
  private TimeoutCeTaskInterrupter underTest = new TimeoutCeTaskInterrupter(timeoutInMs, ceWorkerController, system2);

  @Test
  public void constructor_fails_with_IAE_if_timeout_is_0() {
    assertThatThrownBy(() -> new TimeoutCeTaskInterrupter(0, ceWorkerController, system2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("threshold must be >= 1");
  }

  @Test
  public void constructor_fails_with_IAE_if_timeout_is_less_than_0() {
    long timeout = - (1 + new Random().nextInt(299));

    assertThatThrownBy(() -> new TimeoutCeTaskInterrupter(timeout, ceWorkerController, system2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("threshold must be >= 1");
  }

  @Test
  public void constructor_log_timeout_in_ms_at_INFO_level() {
    int timeout = 1 + new Random().nextInt(9_999);

    new TimeoutCeTaskInterrupter(timeout, ceWorkerController, system2);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.INFO))
      .containsExactly("Compute Engine Task timeout enabled: " + timeout + " ms");
  }

  @Test
  public void check_fails_with_ISE_if_thread_is_not_running_a_CeWorker() {
    Thread t = newThreadWithRandomName();

    assertThatThrownBy(() -> underTest.check(t))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Could not find the CeTask being executed in thread '" + t.getName() + "'");
  }

  @Test
  public void check_fails_with_ISE_if_thread_is_not_running_a_CeWorker_with_no_current_CeTask() {
    Thread t = newThreadWithRandomName();
    mockWorkerOnThread(t, ceWorker);

    assertThatThrownBy(() -> underTest.check(t))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Could not find the CeTask being executed in thread '" + t.getName() + "'");
  }

  @Test
  public void check_fails_with_ISE_if_thread_is_executing_a_CeTask_but_on_start_has_not_been_called_on_it() {
    String taskUuid = secure().nextAlphabetic(15);
    Thread t = new Thread();
    mockWorkerOnThread(t, ceWorker);
    mockWorkerWithTask(ceTask);
    when(ceTask.getUuid()).thenReturn(taskUuid);

    assertThatThrownBy(() -> underTest.check(t))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No start time recorded for task " + taskUuid);
  }

  @Test
  public void check_fails_with_ISE_if_thread_is_executing_a_CeTask_but_on_start_and_on_end_have_not_been_called_on_it() {
    String taskUuid = secure().nextAlphabetic(15);
    Thread t = new Thread();
    mockWorkerOnThread(t, ceWorker);
    mockWorkerWithTask(ceTask);
    when(ceTask.getUuid()).thenReturn(taskUuid);
    underTest.onStart(this.ceTask);
    underTest.onEnd(this.ceTask);

    assertThatThrownBy(() -> underTest.check(t))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No start time recorded for task " + taskUuid);
  }

  @Test
  public void check_throws_CeTaskCanceledException_if_provided_thread_is_interrupted() throws InterruptedException {
    String threadName = secure().nextAlphabetic(30);
    ComputingThread t = new ComputingThread(threadName);
    mockWorkerOnThread(t, ceWorker);
    mockWorkerWithTask(ceTask);
    underTest.onStart(ceTask);

    try {
      t.start();

      // will not fail as thread is not interrupted nor timed out
      underTest.check(t);

      t.interrupt();

      assertThatThrownBy(() -> underTest.check(t))
        .isInstanceOf(CeTaskCanceledException.class)
        .hasMessage("CeWorker executing in Thread '" + threadName + "' has been interrupted");
    } finally {
      t.kill();
      t.join(1_000);
    }
  }

  @Test
  public void check_throws_CeTaskTimeoutException_if_check_called_later_than_timeout_milliseconds_after_on_start() {
    Thread thread = newThreadWithRandomName();
    mockWorkerOnThread(thread, ceWorker);
    mockWorkerWithTask(ceTask);
    long now = 3_776_663_999L;
    when(system2.now()).thenReturn(now);
    underTest.onStart(ceTask);

    // timeout not passed => no exception thrown
    int beforeTimeoutOffset = 1 + new Random().nextInt(timeoutInMs - 1);
    when(system2.now()).thenReturn(now + timeoutInMs - beforeTimeoutOffset);
    underTest.check(thread);

    int afterTimeoutOffset = new Random().nextInt(7_112);
    when(system2.now()).thenReturn(now + timeoutInMs + afterTimeoutOffset);

    assertThatThrownBy(() -> underTest.check(thread))
      .isInstanceOf(CeTaskTimeoutException.class)
      .hasMessage("Execution of task timed out after " + (timeoutInMs + afterTimeoutOffset) + " ms");
  }

  @Test
  public void check_throws_CeTaskCanceledException_if_provided_thread_is_interrupted_even_if_timed_out() throws InterruptedException {
    String threadName = secure().nextAlphabetic(30);
    ComputingThread t = new ComputingThread(threadName);
    mockWorkerOnThread(t, ceWorker);
    mockWorkerWithTask(ceTask);
    long now = 3_776_663_999L;
    when(system2.now()).thenReturn(now);
    underTest.onStart(ceTask);

    try {
      t.start();
      t.interrupt();

      // will not fail as thread is not interrupted nor timed out
      int afterTimeoutOffset = new Random().nextInt(7_112);
      when(system2.now()).thenReturn(now + timeoutInMs + afterTimeoutOffset);

      assertThatThrownBy(() -> underTest.check(t))
        .isInstanceOf(CeTaskCanceledException.class)
        .hasMessage("CeWorker executing in Thread '" + threadName + "' has been interrupted");
    } finally {
      t.kill();
      t.join(1_000);
    }
  }

  private static Thread newThreadWithRandomName() {
    String threadName = secure().nextAlphabetic(30);
    Thread t = new Thread();
    t.setName(threadName);
    return t;
  }

  private void mockWorkerOnThread(Thread t, CeWorker ceWorker) {
    when(ceWorkerController.getCeWorkerIn(t)).thenReturn(Optional.of(ceWorker));
  }

  private void mockWorkerWithTask(CeTask ceTask) {
    when(ceWorker.getCurrentTask()).thenReturn(Optional.of(ceTask));
  }
}
