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

package org.sonar.server.computation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.platform.Server;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportProcessingSchedulerTest {

  private ReportProcessingSchedulerExecutorService batchExecutorService = mock(ReportProcessingSchedulerExecutorService.class);
  private SimpleComputeEngineProcessingQueue processingQueue = new SimpleComputeEngineProcessingQueue();
  private ReportQueue queue = mock(ReportQueue.class);
  private ComponentContainer componentContainer = mock(ComponentContainer.class);

  private ReportProcessingScheduler underTest = new ReportProcessingScheduler(batchExecutorService, processingQueue, queue, componentContainer);

  @Test
  public void schedule_at_fixed_rate_adding_a_ReportProcessingTask_to_the_queue_if_there_is_item_in_ReportQueue() {
    ReportQueue.Item item = mock(ReportQueue.Item.class);
    when(batchExecutorService.scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS)))
      .thenAnswer(new ExecuteFirstArgAsRunnable());
    when(queue.pop()).thenReturn(item);

    underTest.onServerStart(mock(Server.class));

    assertThat(processingQueue.getTasks()).hasSize(1);
    assertThat(processingQueue.getTasks().iterator().next()).isInstanceOf(ReportProcessingTask.class);
  }

  @Test
  public void schedule_at_fixed_rate_does_not_add_ReportProcessingTask_to_the_queue_if_there_is_no_item_in_ReportQueue() {
    when(batchExecutorService.scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS)))
      .thenAnswer(new ExecuteFirstArgAsRunnable());

    underTest.onServerStart(mock(Server.class));

    assertThat(processingQueue.getTasks()).isEmpty();
  }

  @Test
  public void adds_immediately_a_ReportProcessingTask_to_the_queue_if_there_is_item_in_ReportQueue() {
    ReportQueue.Item item = mock(ReportQueue.Item.class);
    doAnswer(new ExecuteFirstArgAsRunnable()).when(batchExecutorService).execute(any(Runnable.class));
    when(queue.pop()).thenReturn(item);

    underTest.startAnalysisTaskNow();

    assertThat(processingQueue.getTasks()).hasSize(1);
    assertThat(processingQueue.getTasks().iterator().next()).isInstanceOf(ReportProcessingTask.class);
  }

  @Test
  public void adds_immediately_does_not_add_ReportProcessingTask_to_the_queue_if_there_is_item_in_ReportQueue() {
    doAnswer(new ExecuteFirstArgAsRunnable()).when(batchExecutorService).execute(any(Runnable.class));

    underTest.startAnalysisTaskNow();

    assertThat(processingQueue.getTasks()).isEmpty();
  }

  private static class SimpleComputeEngineProcessingQueue implements ComputeEngineProcessingQueue {
    private final List<ComputeEngineTask> tasks = new ArrayList<>();

    @Override
    public void addTask(ComputeEngineTask task) {
      tasks.add(task);
    }

    public List<ComputeEngineTask> getTasks() {
      return tasks;
    }
  }

  private static class ExecuteFirstArgAsRunnable implements Answer<Object> {
    @Override
    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
      Runnable runnable = (Runnable) invocationOnMock.getArguments()[0];
      runnable.run();
      return null;
    }
  }
}
