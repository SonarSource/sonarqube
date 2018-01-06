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

import java.util.concurrent.locks.Lock;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.CeDistributedInformation;
import org.sonar.ce.configuration.CeConfiguration;
import org.sonar.ce.queue.InternalCeQueue;

import static java.util.concurrent.TimeUnit.MINUTES;

public class CeCleaningSchedulerImpl implements CeCleaningScheduler {
  private static final Logger LOG = Loggers.get(CeCleaningSchedulerImpl.class);

  private final CeCleaningExecutorService executorService;
  private final CeConfiguration ceConfiguration;
  private final InternalCeQueue internalCeQueue;
  private final CeDistributedInformation ceDistributedInformation;

  public CeCleaningSchedulerImpl(CeCleaningExecutorService executorService, CeConfiguration ceConfiguration,
    InternalCeQueue internalCeQueue, CeDistributedInformation ceDistributedInformation) {
    this.executorService = executorService;
    this.internalCeQueue = internalCeQueue;
    this.ceConfiguration = ceConfiguration;
    this.ceDistributedInformation = ceDistributedInformation;
  }

  @Override
  public void startScheduling() {
    executorService.scheduleWithFixedDelay(this::cleanCeQueue,
      ceConfiguration.getCleanCeTasksInitialDelay(),
      ceConfiguration.getCleanCeTasksDelay(),
      MINUTES);
  }

  private void cleanCeQueue() {
    Lock ceCleaningJobLock = ceDistributedInformation.acquireCleanJobLock();

    // If we cannot lock that means that another job is running
    // So we skip the cancelWornOuts() method
    if (ceCleaningJobLock.tryLock()) {
      try {
        cancelWornOuts();
        resetTasksWithUnknownWorkerUUIDs();
      } finally {
        ceCleaningJobLock.unlock();
      }
    }
  }

  private void cancelWornOuts() {
    try {
      LOG.debug("Deleting any worn out task");
      internalCeQueue.cancelWornOuts();
    } catch (Exception e) {
      LOG.warn("Failed to cancel worn out tasks", e);
    }
  }

  private void resetTasksWithUnknownWorkerUUIDs() {
    try {
      LOG.debug("Resetting state of tasks with unknown worker UUIDs");
      internalCeQueue.resetTasksWithUnknownWorkerUUIDs(ceDistributedInformation.getWorkerUUIDs());
    } catch (Exception e) {
      LOG.warn("Failed to reset tasks with unknown worker UUIDs", e);
    }
  }
}
