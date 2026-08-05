/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.purgehistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.queue.CeQueue;
import org.sonar.server.util.GlobalLockManager;

import static java.util.Collections.emptyMap;
import static org.sonar.db.ce.CeTaskTypes.HISTORY_PURGE;

public class HistoryPurgeTaskLimiter {

  private static final Logger LOG = LoggerFactory.getLogger(HistoryPurgeTaskLimiter.class);
  private static final String LOCK_NAME = "HistoryPurgeCheck";
  private static final int ENQUEUE_LOCK_DELAY_IN_SECONDS = 60;

  private final CeQueue ceQueue;
  private final GlobalLockManager lockManager;

  public HistoryPurgeTaskLimiter(CeQueue ceQueue, GlobalLockManager lockManager) {
    this.ceQueue = ceQueue;
    this.lockManager = lockManager;
  }

  public void tryEnqueue() {
    try {
      // Avoid enqueueing history purge task multiple times
      if (!lockManager.tryLock(LOCK_NAME, ENQUEUE_LOCK_DELAY_IN_SECONDS)) {
        LOG.warn("Unable to acquire lock {}, skipping enqueue of {} task", LOCK_NAME, HISTORY_PURGE);
        return;
      }
      enqueueHistoryPurgeTask();
    } catch (Exception e) {
      LOG.error("Error in History Purge scheduler", e);
    }
  }

  private void enqueueHistoryPurgeTask() {
    ceQueue.submit(ceQueue.prepareSubmit()
      .setType(HISTORY_PURGE)
      .setCharacteristics(emptyMap())
      .build());
  }
}
