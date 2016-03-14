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
package org.sonar.server.computation.taskprocessor;

import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.queue.CeTask;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.queue.InternalCeQueue;
import org.sonar.server.computation.taskprocessor.report.ReportTaskProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CeWorkerCallableImplTest {

  @Rule
  public CeTaskProcessorRepositoryRule taskProcessorRepository = new CeTaskProcessorRepositoryRule();

  InternalCeQueue queue = mock(InternalCeQueue.class);
  ReportTaskProcessor taskProcessor = mock(ReportTaskProcessor.class);
  CeLogging ceLogging = mock(CeLogging.class);
  CeWorkerCallable underTest = new CeWorkerCallableImpl(queue, ceLogging, taskProcessorRepository);
  InOrder inOrder = Mockito.inOrder(ceLogging, taskProcessor, queue);

  @Test
  public void no_pending_tasks_in_queue() throws Exception {
    when(queue.peek()).thenReturn(Optional.<CeTask>absent());

    assertThat(underTest.call()).isFalse();

    verifyZeroInteractions(taskProcessor, ceLogging);
  }

  @Test
  public void fail_when_no_CeTaskProcessor_is_found_in_repository() throws Exception {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin(null).build();
    taskProcessorRepository.setNoProcessorForTask(CeTaskTypes.REPORT);
    when(queue.peek()).thenReturn(Optional.of(task));

    assertThat(underTest.call()).isTrue();

    inOrder.verify(ceLogging).initForTask(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null);
    inOrder.verify(ceLogging).clearForTask();
  }

  @Test
  public void peek_and_process_task() throws Exception {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin(null).build();
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    when(queue.peek()).thenReturn(Optional.of(task));

    assertThat(underTest.call()).isTrue();

    inOrder.verify(ceLogging).initForTask(task);
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.SUCCESS, null);
    inOrder.verify(ceLogging).clearForTask();
  }

  @Test
  public void fail_to_process_task() throws Exception {
    CeTask task = new CeTask.Builder().setUuid("TASK_1").setType(CeTaskTypes.REPORT).setComponentUuid("PROJECT_1").setSubmitterLogin(null).build();
    when(queue.peek()).thenReturn(Optional.of(task));
    taskProcessorRepository.setProcessorForTask(task.getType(), taskProcessor);
    doThrow(new IllegalStateException("simulate exception thrown by TaskProcessor#process")).when(taskProcessor).process(task);

    assertThat(underTest.call()).isTrue();

    inOrder.verify(ceLogging).initForTask(task);
    inOrder.verify(taskProcessor).process(task);
    inOrder.verify(queue).remove(task, CeActivityDto.Status.FAILED, null);
    inOrder.verify(ceLogging).clearForTask();
  }
}
