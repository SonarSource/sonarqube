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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.util.concurrent.TimeUnit.SECONDS;

public class CeProcessingSchedulerImpl implements CeProcessingScheduler, Startable {
  private static final Logger LOG = Loggers.get(CeProcessingSchedulerImpl.class);

  private final CeProcessingSchedulerExecutorService executorService;
  private final CeWorkerCallable workerRunnable;

  private final long delayBetweenTasks;
  private final TimeUnit timeUnit;
  // warning: using a single ChainingCallback object for chaining works and is thread safe only because we use a single Thread in CeProcessingSchedulerExecutorService
  private final ChainingCallback chainingCallback = new ChainingCallback();

  public CeProcessingSchedulerImpl(CeProcessingSchedulerExecutorService processingExecutorService, CeWorkerCallable workerRunnable) {
    this.executorService = processingExecutorService;
    this.workerRunnable = workerRunnable;

    this.delayBetweenTasks = 2;
    this.timeUnit = SECONDS;
  }

  @Override
  public void start() {
    // nothing to do at component startup, startScheduling will be called by CeQueueInitializer
  }

  @Override
  public void startScheduling() {
    ListenableScheduledFuture<Boolean> future = executorService.schedule(workerRunnable, delayBetweenTasks, timeUnit);

    addCallback(future, chainingCallback, executorService);
  }

  @Override
  public void stop() {
    this.chainingCallback.stop();
  }

  private class ChainingCallback implements FutureCallback<Boolean> {
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    @CheckForNull
    private ListenableFuture<Boolean> workerFuture;

    @Override
    public void onSuccess(@Nullable Boolean result) {
      if (result != null && result) {
        chainWithoutDelay();
      } else {
        chainWithDelay();
      }
    }

    @Override
    public void onFailure(Throwable t) {
      if (t instanceof Error) {
        LOG.error("Compute Engine execution failed. Scheduled processing interrupted.", t);
      } else {
        chainWithoutDelay();
      }
    }

    private void chainWithoutDelay() {
      if (keepRunning()) {
        workerFuture = executorService.submit(workerRunnable);
      }
      addCallback();
    }

    private void chainWithDelay() {
      if (keepRunning()) {
        workerFuture = executorService.schedule(workerRunnable, delayBetweenTasks, timeUnit);
      }
      addCallback();
    }

    private void addCallback() {
      if (workerFuture != null && keepRunning()) {
        Futures.addCallback(workerFuture, this, executorService);
      }
    }

    private boolean keepRunning() {
      return keepRunning.get();
    }

    public void stop() {
      this.keepRunning.set(false);
      if (workerFuture != null) {
        workerFuture.cancel(false);
      }
    }
  }
}
