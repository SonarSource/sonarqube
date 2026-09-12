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
package org.sonar.server.history;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.issue.IssueCountDimensionDto;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener;
import org.sonarsource.history.model.IssueCountDimensionKey;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;

/** Refreshes branch issue-count history after issue changes, without waiting for another analysis. */
public class IssueCountHistoryQGChangeEventListener implements QGChangeEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(IssueCountHistoryQGChangeEventListener.class);

  // Entries stay present during execution; a new event marks the branch dirty for another pass.
  private final Map<String, PendingBranch> pendingBranches = new HashMap<>();
  private final IssueCountHistoryExecutor executor;
  private final DbClient dbClient;
  private final IssueCountHistoryRecordingService historyRecordingService;
  private final System2 system2;

  public IssueCountHistoryQGChangeEventListener(DbClient dbClient, IssueCountHistoryRecordingService historyRecordingService, System2 system2,
    IssueCountHistoryExecutor executor) {
    this.executor = executor;
    this.dbClient = dbClient;
    this.historyRecordingService = historyRecordingService;
    this.system2 = system2;
  }

  @Override
  public void onIssueChanges(QGChangeEvent qualityGateEvent, Set<ChangedIssue> changedIssues) {
    BranchDto branch = qualityGateEvent.getBranch();
    if (changedIssues.isEmpty() || branch.getBranchType() != BranchType.BRANCH) {
      return;
    }

    String branchUuid = branch.getUuid();
    synchronized (pendingBranches) {
      PendingBranch pendingBranch = pendingBranches.get(branchUuid);
      if (pendingBranch != null) {
        pendingBranch.refreshRequested = true;
        return;
      }
      PendingBranch scheduledBranch = new PendingBranch();
      pendingBranches.put(branchUuid, scheduledBranch);
      boolean submitted = false;
      try {
        executor.execute(() -> refreshBranch(branchUuid, scheduledBranch));
        submitted = true;
      } finally {
        if (!submitted) {
          pendingBranches.remove(branchUuid, scheduledBranch);
        }
      }
    }
  }

  private void refreshBranch(String branchUuid, PendingBranch pendingBranch) {
    try {
      synchronized (pendingBranches) {
        pendingBranch.refreshRequested = false;
      }
      try {
        recordSnapshot(branchUuid);
      } catch (Exception e) {
        LOG.warn("Failed to refresh issue count history for branch {}", branchUuid, e);
      }
    } finally {
      scheduleNextRefresh(branchUuid, pendingBranch);
    }
  }

  private void scheduleNextRefresh(String branchUuid, PendingBranch pendingBranch) {
    synchronized (pendingBranches) {
      boolean refreshAgain = pendingBranch.refreshRequested && pendingBranches.get(branchUuid) == pendingBranch;
      if (!refreshAgain) {
        pendingBranches.remove(branchUuid, pendingBranch);
        return;
      }
    }
    try {
      executor.execute(() -> refreshBranch(branchUuid, pendingBranch));
    } catch (RuntimeException e) {
      synchronized (pendingBranches) {
        pendingBranches.remove(branchUuid, pendingBranch);
      }
    }
  }

  private void recordSnapshot(String branchUuid) {
    // Refresh the complete snapshot: event deltas would lose dimensions or double-count repeated notifications.
    Map<IssueCountDimensionKey, Integer> counts = new HashMap<>();
    try (DbSession session = dbClient.openSession(false)) {
      // Use the primary and hold the branch lock through the history commit. CE uses the same lock.
      if (!dbClient.branchDao().lockForIssueCountHistory(session, branchUuid)) {
        return;
      }
      for (IssueCountDimensionDto row : dbClient.issueDao().selectIssueCountDimensionsForBranches(session, List.of(branchUuid))) {
        if (row.issueType() != RuleType.SECURITY_HOTSPOT.getDbConstant()) {
          IssueCountDimensionKey key = new IssueCountDimensionKey(
            row.hotspotResolution(), row.codeScope(), row.severity(), row.issueStatus(), row.status(), row.issueType(), row.ruleKey(), row.effectiveImpacts());
          counts.merge(key, row.issueCount(), Integer::sum);
        }
      }
      LocalDate today = Instant.ofEpochMilli(system2.now()).atZone(ZoneOffset.UTC).toLocalDate();
      historyRecordingService.recordIssueHistoryForBranch(branchUuid, counts, today);
    }
  }

  private static final class PendingBranch {
    private boolean refreshRequested = true;
  }
}
