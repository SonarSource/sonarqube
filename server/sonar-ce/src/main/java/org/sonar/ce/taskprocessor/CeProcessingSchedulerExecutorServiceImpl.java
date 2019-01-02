/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.sonar.ce.configuration.CeConfiguration;
import org.sonar.server.util.AbstractStoppableExecutorService;

public class CeProcessingSchedulerExecutorServiceImpl extends AbstractStoppableExecutorService<ListeningScheduledExecutorService>
  implements CeProcessingSchedulerExecutorService {
  private static final String THREAD_NAME_PREFIX = "ce-worker-";

  public CeProcessingSchedulerExecutorServiceImpl(CeConfiguration ceConfiguration) {
    super(
      MoreExecutors.listeningDecorator(
        Executors.newScheduledThreadPool(ceConfiguration.getWorkerMaxCount(),
          new ThreadFactoryBuilder()
            .setNameFormat(THREAD_NAME_PREFIX + "%d")
            .setPriority(Thread.MIN_PRIORITY)
            .build())));
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> task) {
    return delegate.submit(task);
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable task, T result) {
    return delegate.submit(task, result);
  }

  @Override
  public ListenableFuture<?> submit(Runnable task) {
    return delegate.submit(task);
  }

  @Override
  public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return delegate.schedule(command, delay, unit);
  }

  @Override
  public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return delegate.schedule(callable, delay, unit);
  }

  @Override
  public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @Override
  public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }
}
