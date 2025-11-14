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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.KeyWithUuidDto;

/**
 * Cache a map of component key -> set&lt;uuid&gt; in:
 * - sibling PRs that have open issues
 * - branches that use reference branch as new code period setting and have it set to currently analysed branch
 */
public class SiblingComponentsWithOpenIssues {
  private final DbClient dbClient;
  private final AnalysisMetadataHolder metadataHolder;
  private final TreeRootHolder treeRootHolder;

  private Map<String, Set<String>> uuidsByKey;

  public SiblingComponentsWithOpenIssues(TreeRootHolder treeRootHolder, AnalysisMetadataHolder metadataHolder, DbClient dbClient) {
    this.treeRootHolder = treeRootHolder;
    this.metadataHolder = metadataHolder;
    this.dbClient = dbClient;
  }

  private void loadUuidsByKey() {
    String currentBranchUuid = treeRootHolder.getRoot().getUuid();
    String referenceBranchUuid;

    uuidsByKey = new HashMap<>();

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (metadataHolder.isPullRequest()) {
        referenceBranchUuid = metadataHolder.getBranch().getReferenceBranchUuid();
      } else {
        referenceBranchUuid = currentBranchUuid;
        addComponentsFromBranchesThatUseCurrentBranchAsNewCodePeriodReferenceAndHaveOpenIssues(dbSession);
      }

      addComponentsFromPullRequestsTargetingCurrentBranchThatHaveOpenIssues(dbSession, referenceBranchUuid, currentBranchUuid);
    }
  }

  private void addComponentsFromBranchesThatUseCurrentBranchAsNewCodePeriodReferenceAndHaveOpenIssues(DbSession dbSession) {
    String projectUuid = metadataHolder.getProject().getUuid();
    String currentBranchName = metadataHolder.getBranch().getName();

    Set<String> branchUuids = dbClient.newCodePeriodDao().selectBranchesReferencing(dbSession, projectUuid, currentBranchName);

    List<KeyWithUuidDto> components = dbClient.componentDao().selectComponentsFromBranchesThatHaveOpenIssues(dbSession, branchUuids);
    for (KeyWithUuidDto dto : components) {
      uuidsByKey.computeIfAbsent(dto.key(), s -> new HashSet<>()).add(dto.uuid());
    }
  }

  private void addComponentsFromPullRequestsTargetingCurrentBranchThatHaveOpenIssues(DbSession dbSession, String referenceBranchUuid, String currentBranchUuid) {
    List<KeyWithUuidDto> components = dbClient.componentDao().selectComponentsFromPullRequestsTargetingCurrentBranchThatHaveOpenIssues(
      dbSession, referenceBranchUuid, currentBranchUuid);
    for (KeyWithUuidDto dto : components) {
      uuidsByKey.computeIfAbsent(dto.key(), s -> new HashSet<>()).add(dto.uuid());
    }
  }

  public Set<String> getUuids(String componentKey) {
    if (uuidsByKey == null) {
      loadUuidsByKey();
    }

    return uuidsByKey.getOrDefault(componentKey, Collections.emptySet());
  }
}
