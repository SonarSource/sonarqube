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
package org.sonar.ce.cleaning;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import org.junit.Test;
import org.sonar.ce.CeDistributedInformation;
import org.sonar.ce.configuration.CeConfiguration;
import org.sonar.ce.queue.InternalCeQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CeCleaningSchedulerImplTest {

  private Lock jobLock = mock(Lock.class);

  @Test
  public void startScheduling_does_not_fail_if_cleaning_methods_send_even_an_Exception() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeDistributedInformation mockedCeDistributedInformation = mockCeDistributedInformation(jobLock);
    CeCleaningSchedulerImpl underTest = mockCeCleaningSchedulerImpl(mockedInternalCeQueue, mockedCeDistributedInformation);
    Exception exception = new IllegalArgumentException("faking unchecked exception thrown by cancelWornOuts");
    doThrow(exception).when(mockedInternalCeQueue).cancelWornOuts();
    doThrow(exception).when(mockedInternalCeQueue).resetTasksWithUnknownWorkerUUIDs(any());

    underTest.startScheduling();

    verify(mockedInternalCeQueue).cancelWornOuts();
    verify(mockedInternalCeQueue).resetTasksWithUnknownWorkerUUIDs(any());
  }

  @Test
  public void startScheduling_fails_if_cancelWornOuts_send_an_Error() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeDistributedInformation mockedCeDistributedInformation = mockCeDistributedInformation(jobLock);
    CeCleaningSchedulerImpl underTest = mockCeCleaningSchedulerImpl(mockedInternalCeQueue, mockedCeDistributedInformation);
    Error expected = new Error("faking Error thrown by cancelWornOuts");
    doThrow(expected).when(mockedInternalCeQueue).cancelWornOuts();

    try {
      underTest.startScheduling();
      fail("the error should have been thrown");
    } catch (Error e) {
      assertThat(e).isSameAs(expected);
    }
    verify(mockedInternalCeQueue).cancelWornOuts();
  }

  @Test
  public void startScheduling_fails_if_resetTasksWithUnknownWorkerUUIDs_send_an_Error() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeDistributedInformation mockedCeDistributedInformation = mockCeDistributedInformation(jobLock);
    CeCleaningSchedulerImpl underTest = mockCeCleaningSchedulerImpl(mockedInternalCeQueue, mockedCeDistributedInformation);
    Error expected = new Error("faking Error thrown by cancelWornOuts");
    doThrow(expected).when(mockedInternalCeQueue).resetTasksWithUnknownWorkerUUIDs(any());

    try {
      underTest.startScheduling();
      fail("the error should have been thrown");
    } catch (Error e) {
      assertThat(e).isSameAs(expected);
    }
    verify(mockedInternalCeQueue).resetTasksWithUnknownWorkerUUIDs(any());
  }

  @Test
  public void startScheduling_must_call_the_lock_methods() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeDistributedInformation mockedCeDistributedInformation = mockCeDistributedInformation(jobLock);
    CeCleaningSchedulerImpl underTest = mockCeCleaningSchedulerImpl(mockedInternalCeQueue, mockedCeDistributedInformation);
    underTest.startScheduling();

    verify(mockedCeDistributedInformation, times(1)).acquireCleanJobLock();
    verify(jobLock, times(1)).tryLock();
    verify(jobLock, times(1)).unlock();
  }

  @Test
  public void startScheduling_must_not_execute_method_if_lock_is_already_acquired() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    CeDistributedInformation mockedCeDistributedInformation = mockCeDistributedInformation(jobLock);
    when(jobLock.tryLock()).thenReturn(false);

    CeCleaningSchedulerImpl underTest = mockCeCleaningSchedulerImpl(mockedInternalCeQueue, mockedCeDistributedInformation);
    underTest.startScheduling();

    verify(mockedCeDistributedInformation, times(1)).acquireCleanJobLock();
    verify(jobLock, times(1)).tryLock();
    // since lock cannot be locked, unlock method is not been called
    verify(jobLock, times(0)).unlock();
    // since lock cannot be locked, cleaning job methods must not be called
    verify(mockedInternalCeQueue, times(0)).resetTasksWithUnknownWorkerUUIDs(any());
    verify(mockedInternalCeQueue, times(0)).cancelWornOuts();
  }

  @Test
  public void startScheduling_calls_cleaning_methods_of_internalCeQueue_at_fixed_rate_with_value_from_CeConfiguration() {
    InternalCeQueue mockedInternalCeQueue = mock(InternalCeQueue.class);
    long wornOutInitialDelay = 10L;
    long wornOutDelay = 20L;
    long unknownWorkerInitialDelay = 11L;
    long unknownWorkerDelay = 21L;
    CeConfiguration mockedCeConfiguration = mockCeConfiguration(wornOutInitialDelay, wornOutDelay);
    CeCleaningAdapter executorService = new CeCleaningAdapter() {
      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initDelay, long period, TimeUnit unit) {
        schedulerCounter++;
        switch(schedulerCounter) {
          case 1:
            assertThat(initDelay).isEqualTo(wornOutInitialDelay);
            assertThat(period).isEqualTo(wornOutDelay);
            assertThat(unit).isEqualTo(TimeUnit.MINUTES);
            break;
          case 2:
            assertThat(initDelay).isEqualTo(unknownWorkerInitialDelay);
            assertThat(period).isEqualTo(unknownWorkerDelay);
            assertThat(unit).isEqualTo(TimeUnit.MINUTES);
            break;
          default:
            fail("Unknwon call of scheduleWithFixedDelay");
        }
        // synchronously execute command
        command.run();
        return null;
      }
    };
    CeCleaningSchedulerImpl underTest = new CeCleaningSchedulerImpl(executorService, mockedCeConfiguration,
      mockedInternalCeQueue, mockCeDistributedInformation(jobLock));

    underTest.startScheduling();
    assertThat(executorService.schedulerCounter).isEqualTo(1);
    verify(mockedInternalCeQueue).cancelWornOuts();
  }

  private CeConfiguration mockCeConfiguration(long cleanCeTasksInitialDelay, long cleanCeTasksDelay) {
    CeConfiguration mockedCeConfiguration = mock(CeConfiguration.class);
    when(mockedCeConfiguration.getCleanCeTasksInitialDelay()).thenReturn(cleanCeTasksInitialDelay);
    when(mockedCeConfiguration.getCleanCeTasksDelay()).thenReturn(cleanCeTasksDelay);
    return mockedCeConfiguration;
  }

  private CeCleaningSchedulerImpl mockCeCleaningSchedulerImpl(InternalCeQueue internalCeQueue, CeDistributedInformation ceDistributedInformation) {
    return new CeCleaningSchedulerImpl(new CeCleaningAdapter() {
      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        // synchronously execute command
        command.run();
        return null;
      }
    }, mockCeConfiguration(1, 10), internalCeQueue, ceDistributedInformation);
  }

  private CeDistributedInformation mockCeDistributedInformation(Lock result) {
    CeDistributedInformation mocked = mock(CeDistributedInformation.class);
    when(mocked.acquireCleanJobLock()).thenReturn(result);
    when(result.tryLock()).thenReturn(true);
    return mocked;
  }

  /**
   * Implementation of {@link CeCleaningExecutorService} which throws {@link UnsupportedOperationException} for every
   * method.
   */
  private static class CeCleaningAdapter implements CeCleaningExecutorService {
    protected int schedulerCounter = 0;

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
    public boolean awaitTermination(long timeout, TimeUnit unit) {
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
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
      throw createUnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
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
