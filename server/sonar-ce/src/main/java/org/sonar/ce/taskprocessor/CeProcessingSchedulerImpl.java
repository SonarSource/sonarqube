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
package org.sonar.ce.taskprocessor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.configuration.CeConfiguration;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CeProcessingSchedulerImpl implements CeProcessingScheduler {
  private static final Logger LOG = Loggers.get(CeProcessingSchedulerImpl.class);
  private static final long DELAY_BETWEEN_DISABLED_TASKS = 30 * 1000L; // 30 seconds

  private final CeProcessingSchedulerExecutorService executorService;
  private final long delayBetweenEnabledTasks;
  private final TimeUnit timeUnit;
  private final ChainingCallback[] chainingCallbacks;
  private final EnabledCeWorkerController ceWorkerController;
  private final int gracefulStopTimeoutInMs;

  public CeProcessingSchedulerImpl(CeConfiguration ceConfiguration,
    CeProcessingSchedulerExecutorService processingExecutorService, CeWorkerFactory ceCeWorkerFactory,
    EnabledCeWorkerController ceWorkerController) {
    this.executorService = processingExecutorService;

    this.delayBetweenEnabledTasks = ceConfiguration.getQueuePollingDelay();
    this.gracefulStopTimeoutInMs = ceConfiguration.getGracefulStopTimeoutInMs();
    this.ceWorkerController = ceWorkerController;
    this.timeUnit = MILLISECONDS;

    int threadWorkerCount = ceConfiguration.getWorkerMaxCount();
    this.chainingCallbacks = new ChainingCallback[threadWorkerCount];
    for (int i = 0; i < threadWorkerCount; i++) {
      CeWorker worker = ceCeWorkerFactory.create(i);
      chainingCallbacks[i] = new ChainingCallback(worker);
    }
  }

  @Override
  public void startScheduling() {
    for (ChainingCallback chainingCallback : chainingCallbacks) {
      ListenableScheduledFuture<CeWorker.Result> future = executorService.schedule(chainingCallback.worker, delayBetweenEnabledTasks, timeUnit);
      addCallback(future, chainingCallback);
    }
  }

  /**
   * This method is stopping all the workers giving them a delay before killing them.
   */
  @Override
  public void stopScheduling() {
    LOG.debug("Stopping compute engine");
    // Requesting all workers to stop
    for (ChainingCallback chainingCallback : chainingCallbacks) {
      chainingCallback.stop(false);
    }

    // Workers have 40s to gracefully stop processing tasks
    long until = System.currentTimeMillis() + gracefulStopTimeoutInMs;
    LOG.info("Waiting for workers to finish in-progress tasks");
    while (System.currentTimeMillis() < until && ceWorkerController.hasAtLeastOneProcessingWorker()) {
      try {
        Thread.sleep(200L);
      } catch (InterruptedException e) {
        LOG.debug("Graceful stop period has been interrupted: {}", e);
        Thread.currentThread().interrupt();
        break;
      }
    }

    if (ceWorkerController.hasAtLeastOneProcessingWorker()) {
      LOG.info("Some in-progress tasks did not finish in due time. Tasks will be stopped.");
    }

    // Interrupting the tasks
    for (ChainingCallback chainingCallback : chainingCallbacks) {
      chainingCallback.stop(true);
    }
  }

  private class ChainingCallback implements FutureCallback<CeWorker.Result> {
    private volatile boolean keepRunning = true;
    private final CeWorker worker;

    @CheckForNull
    private ListenableFuture<CeWorker.Result> workerFuture;

    public ChainingCallback(CeWorker worker) {
      this.worker = worker;
    }

    @Override
    public void onSuccess(@Nullable CeWorker.Result result) {
      if (keepRunning) {
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
    }

    @Override
    public void onFailure(Throwable t) {
      if (t instanceof Error) {
        LOG.error("Compute Engine execution failed. Scheduled processing interrupted.", t);
      } else if (keepRunning) {
        chainWithoutDelay();
      }
    }

    private void chainWithoutDelay() {
      workerFuture = executorService.submit(worker);
      addCallback();
    }

    private void chainWithEnabledTaskDelay() {
      workerFuture = executorService.schedule(worker, delayBetweenEnabledTasks, timeUnit);
      addCallback();
    }

    private void chainWithDisabledTaskDelay() {
      workerFuture = executorService.schedule(worker, DELAY_BETWEEN_DISABLED_TASKS, timeUnit);
      addCallback();
    }

    private void addCallback() {
      if (workerFuture != null) {
        Futures.addCallback(workerFuture, this);
      }
    }

    public void stop(boolean interrupt) {
      keepRunning = false;
      if (workerFuture != null) {
        workerFuture.cancel(interrupt);
      }
    }
  }
}
