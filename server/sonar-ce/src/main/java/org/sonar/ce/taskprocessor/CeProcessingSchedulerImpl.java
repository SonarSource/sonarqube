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
package org.sonar.ce.taskprocessor;

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
import org.sonar.ce.configuration.CeConfiguration;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CeProcessingSchedulerImpl implements CeProcessingScheduler, Startable {
  private static final Logger LOG = Loggers.get(CeProcessingSchedulerImpl.class);
  private static final long DELAY_BETWEEN_DISABLED_TASKS = 30 * 1000L; // 30 seconds

  private final CeProcessingSchedulerExecutorService executorService;
  private final long delayBetweenEnabledTasks;
  private final TimeUnit timeUnit;
  private final ChainingCallback[] chainingCallbacks;

  public CeProcessingSchedulerImpl(CeConfiguration ceConfiguration,
    CeProcessingSchedulerExecutorService processingExecutorService, CeWorkerFactory ceCeWorkerFactory) {
    this.executorService = processingExecutorService;

    this.delayBetweenEnabledTasks = ceConfiguration.getQueuePollingDelay();
    this.timeUnit = MILLISECONDS;

    int threadWorkerCount = ceConfiguration.getWorkerMaxCount();
    this.chainingCallbacks = new ChainingCallback[threadWorkerCount];
    for (int i = 0; i < threadWorkerCount; i++) {
      CeWorker worker = ceCeWorkerFactory.create(i);
      chainingCallbacks[i] = new ChainingCallback(worker);
    }
  }

  @Override
  public void start() {
    // nothing to do at component startup, startScheduling will be called by CeQueueInitializer
  }

  @Override
  public void startScheduling() {
    for (ChainingCallback chainingCallback : chainingCallbacks) {
      ListenableScheduledFuture<CeWorker.Result> future = executorService.schedule(chainingCallback.worker, delayBetweenEnabledTasks, timeUnit);
      addCallback(future, chainingCallback, executorService);
    }
  }

  @Override
  public void stop() {
    for (ChainingCallback chainingCallback : chainingCallbacks) {
      chainingCallback.stop();
    }
  }

  private class ChainingCallback implements FutureCallback<CeWorker.Result> {
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final CeWorker worker;

    @CheckForNull
    private ListenableFuture<CeWorker.Result> workerFuture;

    public ChainingCallback(CeWorker worker) {
      this.worker = worker;
    }

    @Override
    public void onSuccess(@Nullable CeWorker.Result result) {
      if (result == null) {
        chainWithEnabledTaskDelay();
      } else {
        switch (result) {
          case DISABLED:
            chainWithDisabledTaskDelay();
            break;
          case NO_TASK:
            chainWithEnabledTaskDelay();
            break;
          case TASK_PROCESSED:
          default:
            chainWithoutDelay();
        }
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
        workerFuture = executorService.submit(worker);
      }
      addCallback();
    }

    private void chainWithEnabledTaskDelay() {
      if (keepRunning()) {
        workerFuture = executorService.schedule(worker, delayBetweenEnabledTasks, timeUnit);
      }
      addCallback();
    }

    private void chainWithDisabledTaskDelay() {
      if (keepRunning()) {
        workerFuture = executorService.schedule(worker, DELAY_BETWEEN_DISABLED_TASKS, timeUnit);
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
