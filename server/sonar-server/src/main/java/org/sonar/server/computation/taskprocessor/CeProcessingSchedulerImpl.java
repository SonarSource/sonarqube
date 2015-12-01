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
package org.sonar.server.computation.taskprocessor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class CeProcessingSchedulerImpl implements CeProcessingScheduler {
  private static final Logger LOG = Loggers.get(CeProcessingSchedulerImpl.class);

  private final CeProcessingSchedulerExecutorService executorService;
  private final CeWorkerRunnable workerRunnable;

  private final long delayBetweenTasks;
  private final TimeUnit timeUnit;

  public CeProcessingSchedulerImpl(CeProcessingSchedulerExecutorService processingExecutorService, CeWorkerRunnable workerRunnable) {
    this.executorService = processingExecutorService;
    this.workerRunnable = workerRunnable;

    this.delayBetweenTasks = 2;
    this.timeUnit = SECONDS;
  }

  @Override
  public void startScheduling() {
    ListenableScheduledFuture<Boolean> future = executorService.schedule(workerRunnable, delayBetweenTasks, timeUnit);

    FutureCallback<Boolean> chainingCallback = new ChainingCallback();
    addCallback(future, chainingCallback, executorService);
  }

  private class ChainingCallback implements FutureCallback<Boolean> {
    @Override
    public void onSuccess(@Nullable Boolean result) {
      if (result != null && result) {
        chainWithoutDelay();
      } else {
        chainTask(delayBetweenTasks, timeUnit);
      }
    }

    @Override
    public void onFailure(Throwable t) {
      if (!(t instanceof Error)) {
        chainWithoutDelay();
      } else {
        LOG.error("Compute Engine execution failed. Scheduled processing interrupted.", t);
      }
    }

    private void chainWithoutDelay() {
      chainTask(1, MILLISECONDS);
    }

    private void chainTask(long delay, TimeUnit unit) {
      ListenableScheduledFuture<Boolean> future = executorService.schedule(workerRunnable, delay, unit);
      addCallback(future, this, executorService);
    }
  }
}
