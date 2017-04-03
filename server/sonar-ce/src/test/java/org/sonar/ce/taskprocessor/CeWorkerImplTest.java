/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.UUID;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.InternalCeQueue;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.task.projectanalysis.taskprocessor.ReportTaskProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CeWorkerImplTest {

  @Rule
  public CeTaskProcessorRepositoryRule taskProcessorRepository = new CeTaskProcessorRepositoryRule();
  @Rule
  public LogTester logTester = new LogTester();

  private InternalCeQueue queue = mock(InternalCeQueue.class);
  private ReportTaskProcessor taskProcessor = mock(ReportTaskProcessor.class);
  private CeLogging ceLogging = spy(CeLogging.class);
  private ArgumentCaptor<String> workerUuid = ArgumentCaptor.forClass(String.class);
  private CeWorker underTest = new CeWorkerImpl(queue, ceLogging, taskProcessorRepository, UUID.randomUUID().toString());
  private InOrder inOrder = Mockito.inOrder(ceLogging, taskProcessor, queue);

  @Test
  public void getUUID_must_return_the_uuid_of_constructor() {
    String uuid = UUID.randomUUID().toString();
    CeWorker underTest = new CeWorkerImpl(queue, ceLogging, taskProcessorRepository, uuid);
    assertThat(underTest.getUUID()).isEqualTo(uuid);
  }

  @Test
  public void no_pending_tasks_in_queue() throws Exception {
    when(queue.peek(anyString())).thenReturn(Optional.empty());

    assertThat(underTest.call()).isFalse();

    verifyZeroInteractions(taskProcessor, ceLogging);
  }

  @Test
  public void fail_when_no_CeTaskProcessor_is_found_in_repository() throws Exception {
    CeTask task = createCeTask(null);
    taskProcessorRepository.setNoProcessorForTask(CeTaskTypes.REPORT);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));

    assertThat(underTest.call()).isTrue();

    verifyWorkerUuid();
    inOrder.verify(ceLogging).initForTask(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null, null);
    inOrder.verify(ceLogging).clearForTask();
  }

  @Test
  public void peek_and_process_task() throws Exception {
    CeTask task = createCeTask(null);
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));

    assertThat(underTest.call()).isTrue();

    verifyWorkerUuid();
    inOrder.verify(ceLogging).initForTask(task);
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.SUCCESS, null, null);
    inOrder.verify(ceLogging).clearForTask();
  }

  @Test
  public void fail_to_process_task() throws Exception {
    CeTask task = createCeTask(null);
    when(queue.peek(anyString())).thenReturn(Optional.of(task));
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    Throwable error = makeTaskProcessorFail(task);

    assertThat(underTest.call()).isTrue();

    verifyWorkerUuid();
    inOrder.verify(ceLogging).initForTask(task);
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null, error);
    inOrder.verify(ceLogging).clearForTask();
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
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).doesNotContain(" | submitter=");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(2);
    for (int i = 0; i < 2; i++) {
      assertThat(logs.get(i)).doesNotContain(" | submitter=");
    }
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
    assertThat(logs.get(1)).contains(" | submitter=FooBar | time=");
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
    assertThat(logs).hasSize(1);
    assertThat(logs.iterator().next()).contains(" | submitter=FooBar");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("Failed to execute task " + ceTask.getUuid());
    assertThat(logs.get(1)).contains(" | submitter=FooBar | time=");
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
    assertThat(logs.get(1)).contains(" | submitter=FooBar | time=");
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
    assertThat(logs).hasSize(1);
    assertThat(logs.iterator().next()).contains(" | submitter=FooBar");
    logs = logTester.logs(LoggerLevel.ERROR);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).isEqualTo("Failed to execute task " + ceTask.getUuid());
    assertThat(logs.get(1)).contains(" | submitter=FooBar | time=");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  private void verifyWorkerUuid() {
    verify(queue).peek(workerUuid.capture());
    assertThat(workerUuid.getValue()).startsWith(workerUuid.getValue());
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
    IllegalStateException error = new IllegalStateException("simulate exception thrown by TaskProcessor#process");
    doThrow(error).when(taskProcessor).process(task);
    return error;
  }
}
