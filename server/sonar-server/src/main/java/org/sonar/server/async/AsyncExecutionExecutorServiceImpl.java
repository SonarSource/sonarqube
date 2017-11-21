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
package org.sonar.server.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.util.AbstractStoppableExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class AsyncExecutionExecutorServiceImpl
  extends AbstractStoppableExecutorService<ExecutorService>
  implements AsyncExecutionExecutorService {
  private static final Logger LOG = Loggers.get(AsyncExecutionExecutorServiceImpl.class);

  private static final int MIN_THREAD_COUNT = 1;
  private static final int MAX_THREAD_COUNT = 10;
  private static final int MAX_QUEUE_SIZE = Integer.MAX_VALUE;
  private static final long KEEP_ALIVE_TIME_IN_MILLISECONDS = 0L;

  public AsyncExecutionExecutorServiceImpl() {
    super(
      new ThreadPoolExecutor(
        MIN_THREAD_COUNT, MAX_THREAD_COUNT,
        KEEP_ALIVE_TIME_IN_MILLISECONDS, MILLISECONDS,
        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
        new ThreadFactoryBuilder()
          .setDaemon(false)
          .setNameFormat("SQ_async-%d")
          .setUncaughtExceptionHandler(((t, e) -> LOG.error("Thread " + t + " failed unexpectedly", e)))
          .build()));
  }

  @Override
  public void addToQueue(Runnable r) {
    this.submit(r);
  }
}
