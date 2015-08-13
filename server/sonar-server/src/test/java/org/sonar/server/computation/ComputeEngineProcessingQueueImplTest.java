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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ComputeEngineProcessingQueueImplTest {

  @Test
  public void onServerStart_schedules_at_fixed_rate_run_head_of_queue() {
    ComputeEngineProcessingExecutorService processingExecutorService = mock(ComputeEngineProcessingExecutorService.class);

    ComputeEngineProcessingQueueImpl underTest = new ComputeEngineProcessingQueueImpl(processingExecutorService);
    underTest.onServerStart(mock(Server.class));

    verify(processingExecutorService).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS));
    verifyNoMoreInteractions(processingExecutorService);
  }

  @Test
  public void task_in_queue_is_called_run_only_once() {
    ComputeEngineProcessingExecutorServiceAdapter processingExecutorService = new SimulateFixedRateCallsProcessingExecutorService(10);
    CallCounterComputeEngineTask task = new CallCounterComputeEngineTask();

    ComputeEngineProcessingQueueImpl underTest = new ComputeEngineProcessingQueueImpl(processingExecutorService);
    underTest.addTask(task);
    underTest.onServerStart(mock(Server.class));

    assertThat(task.calls).isEqualTo(1);
  }

  @Test
  public void tasks_are_executed_in_order_of_addition() {
    ComputeEngineProcessingExecutorServiceAdapter processingExecutorService = new SimulateFixedRateCallsProcessingExecutorService(10);

    final List<Integer> nameList = new ArrayList<>();

    ComputeEngineProcessingQueueImpl underTest = new ComputeEngineProcessingQueueImpl(processingExecutorService);
    underTest.addTask(new ComputeEngineTask() {
      @Override
      public void run() {
        nameList.add(1);
      }
    });
    underTest.addTask(new ComputeEngineTask() {
      @Override
      public void run() {
        nameList.add(2);
      }
    });
    underTest.addTask(new ComputeEngineTask() {
      @Override
      public void run() {
        nameList.add(3);
      }
    });
    underTest.addTask(new ComputeEngineTask() {
      @Override
      public void run() {
        nameList.add(4);
      }
    });

    underTest.onServerStart(mock(Server.class));

    assertThat(nameList).containsExactly(1, 2, 3, 4);
  }

  @Test
  public void throwable_raised_by_a_ComputeEngineTask_must_be_caught() {
    ComputeEngineProcessingExecutorServiceAdapter processingExecutorService = new SimulateFixedRateCallsProcessingExecutorService(1);

    ComputeEngineProcessingQueueImpl underTest = new ComputeEngineProcessingQueueImpl(processingExecutorService);
    underTest.addTask(new ComputeEngineTask() {
      @Override
      public void run() {
        throw new RuntimeException("This should be caught by the processing queue");
      }
    });

    underTest.onServerStart(mock(Server.class));
  }

  private static class CallCounterComputeEngineTask implements ComputeEngineTask {
    int calls = 0;

    @Override
    public void run() {
      calls++;
    }
  }

  private static class ComputeEngineProcessingExecutorServiceAdapter implements ComputeEngineProcessingExecutorService {
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public void stop() {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public boolean isShutdown() {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public boolean isTerminated() {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public Future<?> submit(Runnable task) {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public void execute(Runnable command) {
      throw new UnsupportedOperationException("Not implemented!");
    }
  }

  private static class SimulateFixedRateCallsProcessingExecutorService extends ComputeEngineProcessingExecutorServiceAdapter {
    private final int simulatedCalls;

    private SimulateFixedRateCallsProcessingExecutorService(int simulatedCalls) {
      this.simulatedCalls = simulatedCalls;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      // calling the runnable any number of times will only get a task run only once
      for (int i = 0; i < simulatedCalls ; i++) {
        command.run();
      }
      return null;
    }
  }
}
