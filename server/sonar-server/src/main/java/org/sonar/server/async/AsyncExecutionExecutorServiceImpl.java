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
package org.sonar.server.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.util.AbstractStoppableExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

public class AsyncExecutionExecutorServiceImpl
  extends AbstractStoppableExecutorService<ThreadPoolExecutor>
  implements AsyncExecutionExecutorService, AsyncExecutionMonitoring {
  private static final Logger LOG = Loggers.get(AsyncExecutionExecutorServiceImpl.class);

  private static final int MAX_THREAD_COUNT = 10;
  private static final int UNLIMITED_QUEUE = Integer.MAX_VALUE;
  private static final long KEEP_ALIVE_TIME_IN_MINUTES = 5L;

  public AsyncExecutionExecutorServiceImpl() {
    super(createDelegate());
  }

  private static ThreadPoolExecutor createDelegate() {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
      MAX_THREAD_COUNT, MAX_THREAD_COUNT,
      KEEP_ALIVE_TIME_IN_MINUTES, MINUTES,
      new LinkedBlockingQueue<>(UNLIMITED_QUEUE),
      new ThreadFactoryBuilder()
        .setDaemon(false)
        .setNameFormat("SQ_async-%d")
        .setUncaughtExceptionHandler(((t, e) -> LOG.error("Thread " + t + " failed unexpectedly", e)))
        .build());
    threadPoolExecutor.allowCoreThreadTimeOut(true);
    return threadPoolExecutor;
  }

  @Override
  public void addToQueue(Runnable r) {
    this.submit(r);
  }

  @Override
  public int getQueueSize() {
    return delegate.getQueue().size();
  }

  @Override
  public int getWorkerCount() {
    return delegate.getPoolSize();
  }

  @Override
  public int getLargestWorkerCount() {
    return delegate.getLargestPoolSize();
  }
}
