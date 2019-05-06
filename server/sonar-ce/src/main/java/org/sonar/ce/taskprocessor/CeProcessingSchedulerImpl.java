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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.Arrays;
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
  // 30 seconds
  private static final long DELAY_BETWEEN_DISABLED_TASKS = 30 * 1000L;

  private final CeProcessingSchedulerExecutorService executorService;
  private final long delayBetweenEnabledTasks;
  private final TimeUnit timeUnit;
  private final ChainingCallback[] chainingCallbacks;
  private final CeWorkerController ceWorkerController;
  private final long gracefulStopTimeoutInMs;

  public CeProcessingSchedulerImpl(CeConfiguration ceConfiguration,
    CeProcessingSchedulerExecutorService processingExecutorService, CeWorkerFactory ceCeWorkerFactory,
    CeWorkerController ceWorkerController) {
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
   * This method is stopping all the workers and giving them a very large delay before killing them.
   * <p>
   * It supports being interrupted (eg. by a hard stop).
   */
  public void gracefulStopScheduling() {
    LOG.info("Gracefully stopping workers...");
    requestAllWorkersToStop();
    try {
      waitForInProgressWorkersToFinish(gracefulStopTimeoutInMs);

      if (ceWorkerController.hasAtLeastOneProcessingWorker()) {
        LOG.info("Graceful stop period ended but some in-progress task did not finish. Tasks will be interrupted.");
      }

      interruptAllWorkers();
    } catch (InterruptedException e) {
      LOG.debug("Graceful stop was interrupted");
      Thread.currentThread().interrupt();
    }
  }

  /**
   * This method is stopping all the workers and hardly giving them a delay before killing them.
   * <p>
   * If interrupted, it will interrupt any worker still in-progress before returning.
   */
  public void hardStopScheduling() {
    // nothing to do if graceful stop went through
    if (Arrays.stream(chainingCallbacks).allMatch(ChainingCallback::isInterrupted)) {
      return;
    }

    LOG.info("Hard stopping workers...");
    requestAllWorkersToStop();
    try {
      waitForInProgressWorkersToFinish(350);
    } catch (InterruptedException e) {
      LOG.debug("Grace period of hard stop has been interrupted: {}", e);
      Thread.currentThread().interrupt();
    }

    if (ceWorkerController.hasAtLeastOneProcessingWorker()) {
      LOG.info("Some in-progress tasks are getting killed.");
    }

    // Interrupting the tasks
    interruptAllWorkers();
  }

  private void interruptAllWorkers() {
    // Interrupting the tasks
    Arrays.stream(chainingCallbacks).forEach(t -> t.stop(true));
  }

  private void waitForInProgressWorkersToFinish(long shutdownTimeoutInMs) throws InterruptedException {
    // Workers have some time to complete their in progress tasks
    long until = System.currentTimeMillis() + shutdownTimeoutInMs;
    LOG.debug("Waiting for workers to finish in-progress tasks for at most {}ms", shutdownTimeoutInMs);
    while (System.currentTimeMillis() < until && ceWorkerController.hasAtLeastOneProcessingWorker()) {
      Thread.sleep(200L);
    }
  }

  private void requestAllWorkersToStop() {
    // Requesting all workers to stop
    Arrays.stream(chainingCallbacks).forEach(t -> t.stop(false));
  }

  private class ChainingCallback implements FutureCallback<CeWorker.Result> {
    private volatile boolean keepRunning = true;
    private volatile boolean interrupted = false;
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
        interrupted = true;
        workerFuture.cancel(interrupt);
      }
    }

    public boolean isInterrupted() {
      return interrupted;
    }
  }
}
