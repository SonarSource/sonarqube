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
import org.sonar.api.config.GlobalPropertyChangeHandler;
import org.sonar.api.server.ServerSide;
import org.sonar.ce.queue.CeQueue;
import org.sonar.server.util.GlobalLockManager;

import static org.sonar.core.config.PurgeConstants.DAYS_BEFORE_DELETING_HISTORY;

@ServerSide
public class HistoryPurgePropertyChangeHandler extends GlobalPropertyChangeHandler {
  private static final Logger LOG = LoggerFactory.getLogger(HistoryPurgePropertyChangeHandler.class);

  private final HistoryPurgeTaskLimiter historyPurgeTaskLimiter;

  public HistoryPurgePropertyChangeHandler(CeQueue ceQueue, GlobalLockManager lockManager) {
    this.historyPurgeTaskLimiter = new HistoryPurgeTaskLimiter(ceQueue, lockManager);
  }

  @Override
  public void onChange(PropertyChange change) {
    if (DAYS_BEFORE_DELETING_HISTORY.equals(change.getKey())) {
      LOG.info("Detected value change of property {}; kicking off HISTORY_PURGE", DAYS_BEFORE_DELETING_HISTORY);
      historyPurgeTaskLimiter.tryEnqueue();
    }
  }
}
