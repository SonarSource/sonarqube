/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.EsIndexSyncInProgressException;

import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.SUBVIEW;
import static org.sonar.db.component.ComponentQualifiers.VIEW;

public class IssueIndexSyncProgressChecker {
  private static final ImmutableSet<String> APP_VIEW_OR_SUBVIEW = ImmutableSet.<String>builder().add(VIEW, SUBVIEW, APP).build();
  private final DbClient dbClient;

  public IssueIndexSyncProgressChecker(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public IssueSyncProgress getIssueSyncProgress(DbSession dbSession) {
    int completedCount = dbClient.projectDao().countIndexedProjects(dbSession);
    int total = dbClient.projectDao().countProjects(dbSession);
    boolean hasFailures = dbClient.ceActivityDao().hasAnyFailedOrCancelledIssueSyncTask(dbSession);
    boolean isCompleted = !dbClient.ceQueueDao().hasAnyIssueSyncTaskPendingOrInProgress(dbSession);
    return new IssueSyncProgress(isCompleted, completedCount, total, hasFailures);
  }

  public void checkIfAnyComponentsNeedIssueSync(DbSession dbSession, List<String> componentKeys) {
    boolean isAppOrViewOrSubview = dbClient.componentDao().existAnyOfComponentsWithQualifiers(dbSession, componentKeys, APP_VIEW_OR_SUBVIEW);
    boolean needIssueSync;
    if (isAppOrViewOrSubview) {
      needIssueSync = dbClient.branchDao().hasAnyBranchWhereNeedIssueSync(dbSession, true);
    } else {
      needIssueSync = dbClient.branchDao().doAnyOfComponentsNeedIssueSync(dbSession, componentKeys);
    }
    if (needIssueSync) {
      throw new EsIndexSyncInProgressException(IssueIndexDefinition.TYPE_ISSUE.getMainType(),
          "Results are temporarily unavailable. Indexing of issues is in progress.");
    }
  }

  public void checkIfComponentNeedIssueSync(DbSession dbSession, String componentKey) {
    checkIfAnyComponentsNeedIssueSync(dbSession, Collections.singletonList(componentKey));
  }

  /**
   * Checks if issue index sync is in progress, if it is, method throws exception org.sonar.server.es.EsIndexSyncInProgressException
   */
  public void checkIfIssueSyncInProgress(DbSession dbSession) {
    if (isIssueSyncInProgress(dbSession)) {
      throw new EsIndexSyncInProgressException(IssueIndexDefinition.TYPE_ISSUE.getMainType(),
          "Results are temporarily unavailable. Indexing of issues is in progress.");
    }
  }

  public boolean isIssueSyncInProgress(DbSession dbSession) {
    return dbClient.branchDao().hasAnyBranchWhereNeedIssueSync(dbSession, true);
  }

  public boolean doProjectNeedIssueSync(DbSession dbSession, String projectUuid) {
    return !findProjectUuidsWithIssuesSyncNeed(dbSession, Sets.newHashSet(projectUuid)).isEmpty();
  }

  public List<String> findProjectUuidsWithIssuesSyncNeed(DbSession dbSession, Collection<String> projectUuids) {
    return dbClient.branchDao().selectProjectUuidsWithIssuesNeedSync(dbSession, projectUuids);
  }
}
