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
package org.sonar.ce.taskprocessor;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.InternalCeQueue;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.task.projectanalysis.taskprocessor.ReportTaskProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.taskprocessor.CeWorker.Result.DISABLED;
import static org.sonar.ce.taskprocessor.CeWorker.Result.NO_TASK;
import static org.sonar.ce.taskprocessor.CeWorker.Result.TASK_PROCESSED;

public class CeWorkerImplTest {

  @Rule
  public CeTaskProcessorRepositoryRule taskProcessorRepository = new CeTaskProcessorRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private InternalCeQueue queue = mock(InternalCeQueue.class);
  private ReportTaskProcessor taskProcessor = mock(ReportTaskProcessor.class);
  private CeWorker.ExecutionListener executionListener1 = mock(CeWorker.ExecutionListener.class);
  private CeWorker.ExecutionListener executionListener2 = mock(CeWorker.ExecutionListener.class);
  private EnabledCeWorkerController enabledCeWorkerController = mock(EnabledCeWorkerController.class);
  private ArgumentCaptor<String> workerUuidCaptor = ArgumentCaptor.forClass(String.class);
  private int randomOrdinal = new Random().nextInt(50);
  private String workerUuid = UUID.randomUUID().toString();
  private CeWorker underTest = new CeWorkerImpl(randomOrdinal, workerUuid, queue, taskProcessorRepository, enabledCeWorkerController,
    executionListener1, executionListener2);
  private CeWorker underTestNoListener = new CeWorkerImpl(randomOrdinal, workerUuid, queue, taskProcessorRepository, enabledCeWorkerController);
  private InOrder inOrder = Mockito.inOrder(taskProcessor, queue, executionListener1, executionListener2);

  @Before
  public void setUp() {
    when(enabledCeWorkerController.isEnabled(any(CeWorker.class))).thenReturn(true);
  }

  @Test
  public void constructor_throws_IAE_if_ordinal_is_less_than_zero() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Ordinal must be >= 0");

    new CeWorkerImpl(-1 - new Random().nextInt(20), workerUuid, queue, taskProcessorRepository, enabledCeWorkerController);
  }

  @Test
  public void getUUID_must_return_the_uuid_of_constructor() {
    String uuid = UUID.randomUUID().toString();
    CeWorker underTest = new CeWorkerImpl(randomOrdinal, uuid, queue, taskProcessorRepository, enabledCeWorkerController);
    assertThat(underTest.getUUID()).isEqualTo(uuid);
  }

  @Test
  public void worker_disabled() throws Exception {
    reset(enabledCeWorkerController);
    when(enabledCeWorkerController.isEnabled(underTest)).thenReturn(false);

    assertThat(underTest.call()).isEqualTo(DISABLED);

    verifyZeroInteractions(taskProcessor, executionListener1, executionListener2);
  }

  @Test
  public void worker_disabled_no_listener() throws Exception {
    reset(enabledCeWorkerController);
    when(enabledCeWorkerController.isEnabled(underTest)).thenReturn(false);

    assertThat(underTestNoListener.call()).isEqualTo(DISABLED);

    verifyZeroInteractions(taskProcessor, executionListener1, executionListener2);
  }

  @Test
  public void no_pending_tasks_in_queue() throws Exception {
    when(queue.peek(anyString())).thenReturn(Optional.empty());

    assertThat(underTest.call()).isEqualTo(NO_TASK);

    verifyZeroInteractions(taskProcessor, executionListener1, executionListener2);
  }

  @Test
  public void no_pending_tasks_in_queue_without_listener() throws Exception {
    when(queue.peek(anyString())).thenReturn(Optional.empty());

    assertThat(underTestNoListener.call()).isEqualTo(NO_TASK);

    verifyZeroInteractions(taskProcessor, executionListener1, executionListener2);
  }

  @Test
  public void fail_when_no_CeTaskProcessor_is_found_in_repository() throws Exception {
    CeTask task = createCeTask(null);
    taskProcessorRepository.setNoProcessorForTask(CeTaskTypes.REPORT);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));

    assertThat(underTest.call()).isEqualTo(TASK_PROCESSED);

    verifyWorkerUuid();
    inOrder.verify(executionListener1).onStart(task);
    inOrder.verify(executionListener2).onStart(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null, null);
    inOrder.verify(executionListener1).onEnd(task, CeActivityDto.Status.FAILED, null, null);
    inOrder.verify(executionListener2).onEnd(task, CeActivityDto.Status.FAILED, null, null);
  }

  @Test
  public void fail_when_no_CeTaskProcessor_is_found_in_repository_without_listener() throws Exception {
    CeTask task = createCeTask(null);
    taskProcessorRepository.setNoProcessorForTask(CeTaskTypes.REPORT);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));

    assertThat(underTestNoListener.call()).isEqualTo(TASK_PROCESSED);

    verifyWorkerUuid();
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null, null);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void peek_and_process_task() throws Exception {
    CeTask task = createCeTask(null);
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));

    assertThat(underTest.call()).isEqualTo(TASK_PROCESSED);

    verifyWorkerUuid();
    inOrder.verify(executionListener1).onStart(task);
    inOrder.verify(executionListener2).onStart(task);
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.SUCCESS, null, null);
    inOrder.verify(executionListener1).onEnd(task, CeActivityDto.Status.SUCCESS, null, null);
    inOrder.verify(executionListener2).onEnd(task, CeActivityDto.Status.SUCCESS, null, null);
  }

  @Test
  public void peek_and_process_task_without_listeners() throws Exception {
    CeTask task = createCeTask(null);
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));

    assertThat(underTestNoListener.call()).isEqualTo(TASK_PROCESSED);

    verifyWorkerUuid();
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.SUCCESS, null, null);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void fail_to_process_task() throws Exception {
    CeTask task = createCeTask(null);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    Throwable error = makeTaskProcessorFail(task);

    assertThat(underTest.call()).isEqualTo(TASK_PROCESSED);

    verifyWorkerUuid();
    inOrder.verify(executionListener1).onStart(task);
    inOrder.verify(executionListener2).onStart(task);
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null, error);
    inOrder.verify(executionListener1).onEnd(task, CeActivityDto.Status.FAILED, null, error);
    inOrder.verify(executionListener2).onEnd(task, CeActivityDto.Status.FAILED, null, error);
  }

  @Test
  public void fail_to_process_task_without_listeners() throws Exception {
    CeTask task = createCeTask(null);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    Throwable error = makeTaskProcessorFail(task);

    assertThat(underTestNoListener.call()).isEqualTo(TASK_PROCESSED);

    verifyWorkerUuid();
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null, error);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void do_not_display_submitter_param_in_log_when_submitterLogin_is_not_set_in_case_of_success() throws Exception {
    when(queue.peek(anyString())).thenReturn(Optional.of(createCeTask(null)));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);

    underTest.call();

    verifyWorkerUuid();
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    for (int i = 0; i < 2; i++) {
      assertThat(logs.get(i)).doesNotContain(" | submitter=");
    }
  }

  @Test
  public void do_not_display_submitter_param_in_log_when_submitterLogin_is_not_set_in_case_of_error() throws Exception {
    CeTask ceTask = createCeTask(null);
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(ceTask.getType(), taskProcessor);
    makeTaskProcessorFail(ceTask);

    underTest.call();

    verifyWorkerUuid();
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).doesNotContain(" | submitter=");
    assertThat(logs.get(1)).doesNotContain(" | submitter=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(1);
    assertThat(logs.iterator().next()).doesNotContain(" | submitter=");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  @Test
  public void display_submitterLogin_in_logs_when_set_in_case_of_success() throws Exception {
    when(queue.peek(anyString())).thenReturn(Optional.of(createCeTask("FooBar")));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);

    underTest.call();

    verifyWorkerUuid();
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=SUCCESS | time=");
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  @Test
  public void display_submitterLogin_in_logs_when_set_in_case_of_error() throws Exception {
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(ceTask.getType(), taskProcessor);
    makeTaskProcessorFail(ceTask);

    underTest.call();

    verifyWorkerUuid();
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=FAILED | time=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("Failed to execute task " + ceTask.getUuid());
  }

  @Test
  public void display_start_stop_at_debug_level_for_console_if_DEBUG_is_enabled_and_task_successful() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);

    when(queue.peek(anyString())).thenReturn(Optional.of(createCeTask("FooBar")));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);

    underTest.call();

    verifyWorkerUuid();
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=SUCCESS | time=");
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  @Test
  public void display_start_at_debug_level_stop_at_error_level_for_console_if_DEBUG_is_enabled_and_task_failed() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);

    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    makeTaskProcessorFail(ceTask);

    underTest.call();

    verifyWorkerUuid();
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=FAILED | time=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(1);
    assertThat(logs.iterator().next()).isEqualTo("Failed to execute task " + ceTask.getUuid());
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  @Test
  public void call_sets_and_restores_thread_name_with_information_of_worker_when_there_is_no_task_to_process() throws Exception {
    String threadName = RandomStringUtils.randomAlphabetic(3);
    when(queue.peek(anyString())).thenAnswer(invocation -> {
      assertThat(Thread.currentThread().getName())
        .isEqualTo("Worker " + randomOrdinal + " (UUID=" + workerUuid + ") on " + threadName);
      return Optional.empty();
    });
    Thread newThread = createThreadNameVerifyingThread(threadName);

    newThread.start();
    newThread.join();
  }

  @Test
  public void call_sets_and_restores_thread_name_with_information_of_worker_when_a_task_is_processed() throws Exception {
    String threadName = RandomStringUtils.randomAlphabetic(3);
    when(queue.peek(anyString())).thenAnswer(invocation -> {
      assertThat(Thread.currentThread().getName())
        .isEqualTo("Worker " + randomOrdinal + " (UUID=" + workerUuid + ") on " + threadName);
      return Optional.of(createCeTask("FooBar"));
    });
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    Thread newThread = createThreadNameVerifyingThread(threadName);

    newThread.start();
    newThread.join();
  }

  @Test
  public void call_sets_and_restores_thread_name_with_information_of_worker_when_an_error_occurs() throws Exception {
    String threadName = RandomStringUtils.randomAlphabetic(3);
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenAnswer(invocation -> {
      assertThat(Thread.currentThread().getName())
        .isEqualTo("Worker " + randomOrdinal + " (UUID=" + workerUuid + ") on " + threadName);
      return Optional.of(ceTask);
    });
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    makeTaskProcessorFail(ceTask);
    Thread newThread = createThreadNameVerifyingThread(threadName);

    newThread.start();
    newThread.join();
  }

  @Test
  public void call_sets_and_restores_thread_name_with_information_of_worker_when_worker_is_disabled() throws Exception {
    reset(enabledCeWorkerController);
    when(enabledCeWorkerController.isEnabled(underTest)).thenReturn(false);

    String threadName = RandomStringUtils.randomAlphabetic(3);
    Thread newThread = createThreadNameVerifyingThread(threadName);

    newThread.start();
    newThread.join();
  }

  @Test
  public void log_error_when_task_fails_with_not_MessageException() throws Exception {
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    makeTaskProcessorFail(ceTask);

    underTest.call();

    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=FAILED | time=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(1);
    assertThat(logs.iterator().next()).isEqualTo("Failed to execute task " + ceTask.getUuid());
  }

  @Test
  public void do_no_log_error_when_task_fails_with_MessageException() throws Exception {
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    makeTaskProcessorFail(ceTask, MessageException.of("simulate MessageException thrown by TaskProcessor#process"));

    underTest.call();

    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(1)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=FAILED | time=");
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void log_error_when_task_was_successful_but_ending_state_can_not_be_persisted_to_db() throws Exception {
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    doThrow(new RuntimeException("Simulate queue#remove failing")).when(queue).remove(ceTask, CeActivityDto.Status.SUCCESS, null, null);

    underTest.call();

    assertThat(logTester.logs(LoggerLevel.ERROR)).containsOnly("Failed to finalize task with uuid '" + ceTask.getUuid() + "' and persist its state to db");
  }

  @Test
  public void log_error_when_task_failed_and_ending_state_can_not_be_persisted_to_db() throws Exception {
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    IllegalStateException ex = makeTaskProcessorFail(ceTask);
    doThrow(new RuntimeException("Simulate queue#remove failing")).when(queue).remove(ceTask, CeActivityDto.Status.FAILED, null, ex);

    underTest.call();

    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=FAILED | time=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("Failed to execute task " + ceTask.getUuid());
    assertThat(logs.get(1)).isEqualTo("Failed to finalize task with uuid '" + ceTask.getUuid() + "' and persist its state to db");
  }

  @Test
  public void log_error_when_task_failed_with_MessageException_and_ending_state_can_not_be_persisted_to_db() throws Exception {
    CeTask ceTask = createCeTask("FooBar");
    when(queue.peek(anyString())).thenReturn(Optional.of(ceTask));
    taskProcessorRepository.setProcessorForTask(CeTaskTypes.REPORT, taskProcessor);
    MessageException ex = makeTaskProcessorFail(ceTask, MessageException.of("simulate MessageException thrown by TaskProcessor#process"));
    doThrow(new RuntimeException("Simulate queue#remove failing")).when(queue).remove(ceTask, CeActivityDto.Status.FAILED, null, ex);

    underTest.call();

    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).contains(" | submitter=FooBar");
    assertThat(logs.get(1)).contains(" | submitter=FooBar | status=FAILED | time=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("Failed to finalize task with uuid '" + ceTask.getUuid() + "' and persist its state to db. " +
      "Task failed with MessageException \"" + ex.getMessage() + "\"");
  }

  private Thread createThreadNameVerifyingThread(String threadName) {
    return new Thread(() -> {
      verifyUnchangedThreadName(threadName);
      try {
        underTest.call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      verifyUnchangedThreadName(threadName);
    }, threadName);
  }

  private void verifyUnchangedThreadName(String threadName) {
    assertThat(Thread.currentThread().getName()).isEqualTo(threadName);
  }

  private void verifyWorkerUuid() {
    verify(queue).peek(workerUuidCaptor.capture());
    assertThat(workerUuidCaptor.getValue()).isEqualTo(workerUuid);
  }

  private static CeTask createCeTask(@Nullable String submitterLogin) {
    return new CeTask.Builder()
      .setOrganizationUuid("org1")
      .setUuid("TASK_1").setType(CeTaskTypes.REPORT)
      .setComponentUuid("PROJECT_1")
      .setSubmitterLogin(submitterLogin)
      .build();
  }

  private IllegalStateException makeTaskProcessorFail(CeTask task) {
    return makeTaskProcessorFail(task, new IllegalStateException("simulate exception thrown by TaskProcessor#process"));
  }

  private <T extends Throwable> T makeTaskProcessorFail(CeTask task, T t) {
    doThrow(t).when(taskProcessor).process(task);
    return t;
  }
}
