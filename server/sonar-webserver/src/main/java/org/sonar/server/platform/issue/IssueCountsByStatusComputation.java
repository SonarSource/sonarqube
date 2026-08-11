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
package org.sonar.server.platform.issue;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueCountByStatusAndResolution;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.util.GlobalLockManager;

/**
 * Periodically computes the issue counts by status on main branches and stores them as JSON in an internal property,
 * so that the Support Info File can read this instead of running the expensive full-table aggregation on the
 * request thread (SONAR-31406). Running on a dedicated schedule ensures the
 * value is refreshed even when telemetry is disabled, which is common on air-gapped on-prem instances.
 */
public class IssueCountsByStatusComputation implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(IssueCountsByStatusComputation.class);

  private static final long PERIOD_IN_SECONDS = 60 * 60L;
  private static final String LOCK_NAME = "IssueCountsByStatus";
  // The guarded aggregation is a full-table scan that can take minutes on very large instances (measured ~166s on a
  // 15M-issue instance), so the lock must outlive it to prevent another cluster node from re-running it concurrently.
  private static final int LOCK_DURATION_SECONDS = 20 * 60;

  private static final Gson GSON = new Gson();

  private final IssueCountsByStatusComputationExecutorService executorService;
  private final DbClient dbClient;
  private final InternalProperties internalProperties;
  private final GlobalLockManager lockManager;

  public IssueCountsByStatusComputation(IssueCountsByStatusComputationExecutorService executorService, DbClient dbClient,
    InternalProperties internalProperties, GlobalLockManager lockManager) {
    this.executorService = executorService;
    this.dbClient = dbClient;
    this.internalProperties = internalProperties;
    this.lockManager = lockManager;
  }

  @Override
  public void start() {
    executorService.scheduleAtFixedRate(this::compute, 0, PERIOD_IN_SECONDS, TimeUnit.SECONDS);
  }

  private void compute() {
    if (!lockManager.tryLock(LOCK_NAME, LOCK_DURATION_SECONDS)) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      final List<IssueCountByStatusAndResolution> counts = dbClient.issueDao().countIssuesByStatusOnMainBranches(dbSession);
      final Map<String, Integer> statusMap = IssueCountByStatusAndResolution.toStatusMap(counts);
      internalProperties.write(InternalProperties.ISSUE_COUNTS_BY_STATUS, GSON.toJson(statusMap));
    } catch (RuntimeException e) {
      LOG.warn("Failed to compute issue counts by status for the Support Info File", e);
    }
  }

  @Override
  public void stop() {
    // nothing to do, the executor service is stopped separately
  }

}
