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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.GlobalPropertyChangeHandler.PropertyChange;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.server.util.GlobalLockManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.PurgeConstants.DAYS_BEFORE_DELETING_HISTORY;
import static org.sonar.core.config.PurgeConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY;
import static org.sonar.db.ce.CeTaskTypes.HISTORY_PURGE;

class HistoryPurgePropertyChangeHandlerTest {

  private final CeQueue ceQueue = mock();
  private final GlobalLockManager lockManager = mock();
  private final HistoryPurgePropertyChangeHandler underTest = new HistoryPurgePropertyChangeHandler(ceQueue, lockManager);

  @BeforeEach
  void setup() {
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    when(ceQueue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder("uuid"));
  }

  @Test
  void onChange_whenPropertyIsDaysBeforeDeletingHistory_shouldKickOffHistoryPurge() {
    underTest.onChange(PropertyChange.create(DAYS_BEFORE_DELETING_HISTORY, "100"));

    verify(ceQueue).submit(argThat(task -> task.getType().equals(HISTORY_PURGE)));
  }

  @Test
  void onChange_whenPropertyIsNotDaysBeforeDeletingHistory_shouldDoNothing() {
    underTest.onChange(PropertyChange.create(HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY, "100"));

    verify(ceQueue, never()).submit(any());
  }
}