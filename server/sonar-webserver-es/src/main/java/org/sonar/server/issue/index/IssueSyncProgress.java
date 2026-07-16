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
package org.sonar.server.issue.index;

public class IssueSyncProgress {

  public static final String MSG_IN_PROGRESS = "Results are temporarily unavailable. Indexing of issues is in progress.";
  public static final String MSG_FAILURES = "Results are temporarily unavailable. Some issue sync tasks have failed or been cancelled. " +
    "A system administrator should review and retry the failed tasks.";
  public static final String MSG_STUCK = "Results are temporarily unavailable. Issue sync completed but some projects remain stuck. " +
    "To identify them, a database administrator can find projects with need_issue_sync=True. " +
    "Once identified, a system administrator can reindex each project via API Post request /api/issues/reindex.";

  private final int completedCount;
  private final int total;
  private final boolean hasFailures;
  private final boolean isQueueEmpty;

  public IssueSyncProgress(boolean isQueueEmpty, int completedCount, int total, boolean hasFailures) {
    this.completedCount = completedCount;
    this.hasFailures = hasFailures;
    this.isQueueEmpty = isQueueEmpty;
    this.total = total;
  }

  public int getCompletedCount() {
    return completedCount;
  }

  public boolean hasFailures() {
    return hasFailures;
  }

  public int getTotal() {
    return total;
  }

  public boolean isCompleted() {
    // Checking queue empty here is not necessary since all branches may be synced before the queue drains
    return completedCount == total;
  }

  public boolean hasInconsistencies() {
    return isQueueEmpty && completedCount < total && !hasFailures;
  }

  // Only meaningful while the sync is not completed
  public String getStatusMessage() {
    if(hasFailures) {
      return MSG_FAILURES;
    }

    if(hasInconsistencies()) {
      return MSG_STUCK;
    }

    return MSG_IN_PROGRESS;
  }
}
