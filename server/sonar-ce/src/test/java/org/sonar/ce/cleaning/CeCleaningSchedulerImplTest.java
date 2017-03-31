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
package org.sonar.ce.cleaning;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.sonar.ce.configuration.CeConfiguration;
import org.sonar.ce.queue.InternalCeQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CeCleaningSchedulerImplTest {
  @Test
  public void startScheduling_does_not_fail_if_cancelWornOuts_send_even_an_Exception() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeCleaningSchedulerImpl underTest = new CeCleaningSchedulerImpl(new CeCleaningAdapter() {
      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        // synchronously execute command
        command.run();
        return null;
      }
    }, mockCeConfiguration(1, 10), mockedInternalCeQueue);
    doThrow(new IllegalArgumentException("faking unchecked exception thrown by cancelWornOuts")).when(mockedInternalCeQueue).cancelWornOuts();

    underTest.startScheduling();

    verify(mockedInternalCeQueue).cancelWornOuts();
  }

  @Test
  public void startScheduling_fails_if_cancelWornOuts_send_even_an_Error() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeCleaningSchedulerImpl underTest = new CeCleaningSchedulerImpl(new CeCleaningAdapter() {
      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        // synchronously execute command
        command.run();
        return null;
      }
    }, mockCeConfiguration(1, 10), mockedInternalCeQueue);
    Error expected = new Error("faking Error thrown by cancelWornOuts");
    doThrow(expected).when(mockedInternalCeQueue).cancelWornOuts();

    try {
      underTest.startScheduling();
      fail("the error should have been thrown");
    } catch (Error e) {
      assertThat(e).isSameAs(expected);
    }
  }

  @Test
  public void startScheduling_calls_cancelWornOuts_of_internalCeQueue_at_fixed_rate_with_value_from_CeConfiguration() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    long initialDelay = 10L;
    long delay = 20L;
    CeConfiguration mockedCeConfiguration = mockCeConfiguration(initialDelay, delay);
    CeCleaningAdapter executorService = new CeCleaningAdapter() {
      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initDelay, long period, TimeUnit unit) {
        assertThat(initDelay).isEqualTo(initialDelay);
        assertThat(period).isEqualTo(delay);
        assertThat(unit).isEqualTo(TimeUnit.MINUTES);
        // synchronously execute command
        command.run();
        return null;
      }
    };
    CeCleaningSchedulerImpl underTest = new CeCleaningSchedulerImpl(executorService, mockedCeConfiguration, mockedInternalCeQueue);

    underTest.startScheduling();

    verify(mockedInternalCeQueue).cancelWornOuts();
  }

  private CeConfiguration mockCeConfiguration(long initialDelay, long delay) {
    CeConfiguration mockedCeConfiguration = mock(CeConfiguration.class);
    when(mockedCeConfiguration.getCancelWornOutsInitialDelay()).thenReturn(initialDelay);
    when(mockedCeConfiguration.getCancelWornOutsDelay()).thenReturn(delay);
    return mockedCeConfiguration;
  }

  /**
   * Implementation of {@link CeCleaningExecutorService} which throws {@link UnsupportedOperationException} for every
   * method.
   */
  private static class CeCleaningAdapter implements CeCleaningExecutorService {

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      throw createUnsupportedOperationException();
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      throw createUnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      throw createUnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      throw createUnsupportedOperationException();
    }

    @Override
    public void shutdown() {
      throw createUnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw createUnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
      throw createUnsupportedOperationException();
    }

    @Override
    public boolean isTerminated() {
      throw createUnsupportedOperationException();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      throw createUnsupportedOperationException();
    }

    @Override
    public Future<?> submit(Runnable task) {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      throw createUnsupportedOperationException();
    }

    @Override
    public void execute(Runnable command) {
      throw createUnsupportedOperationException();
    }

    private UnsupportedOperationException createUnsupportedOperationException() {
      return new UnsupportedOperationException("Unexpected call");
    }
  }
}
