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
package org.sonar.ce.task.projectanalysis.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.db.component.ComponentDto.removeBranchAndPullRequestFromKey;

/**
 * Cache a map between component keys and uuids in the merge branch and optionally the target branch (for PR and SLB, and only if this target branch is analyzed)
 */
public class MergeAndTargetBranchComponentUuids {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private Map<String, String> mergeBranchComponentsUuidsByKey;
  private Map<String, String> targetBranchComponentsUuidsByKey;
  private String mergeBranchName;
  private boolean hasMergeBranchAnalysis;
  private boolean hasTargetBranchAnalysis;
  @CheckForNull
  private String targetBranchUuid;

  public MergeAndTargetBranchComponentUuids(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
  }

  private void lazyInit() {
    if (mergeBranchComponentsUuidsByKey == null) {
      String mergeBranchUuid = analysisMetadataHolder.getBranch().getMergeBranchUuid();

      mergeBranchComponentsUuidsByKey = new HashMap<>();
      targetBranchComponentsUuidsByKey = new HashMap<>();

      try (DbSession dbSession = dbClient.openSession(false)) {

        Optional<BranchDto> opt = dbClient.branchDao().selectByUuid(dbSession, mergeBranchUuid);
        checkState(opt.isPresent(), "Merge branch '%s' does not exist", mergeBranchUuid);
        mergeBranchName = opt.get().getKey();

        initForMergeBranch(mergeBranchUuid, dbSession);

        if (analysisMetadataHolder.isSLBorPR()) {
          initForTargetBranch(mergeBranchUuid, dbSession);
        } else {
          hasTargetBranchAnalysis = false;
        }
      }
    }
  }

  private void initForTargetBranch(String mergeBranchUuid, DbSession dbSession) {
    Optional<BranchDto> branchDtoOpt = dbClient.branchDao().selectByBranchKey(dbSession, analysisMetadataHolder.getProject().getUuid(),
      analysisMetadataHolder.getBranch().getTargetBranchName());
    targetBranchUuid = branchDtoOpt.map(BranchDto::getUuid).orElse(null);
    hasTargetBranchAnalysis = targetBranchUuid != null && dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, targetBranchUuid).isPresent();
    if (hasTargetBranchAnalysis && !targetBranchUuid.equals(mergeBranchUuid)) {
      List<ComponentDto> targetComponents = dbClient.componentDao().selectByProjectUuid(targetBranchUuid, dbSession);
      for (ComponentDto dto : targetComponents) {
        targetBranchComponentsUuidsByKey.put(dto.getKey(), dto.uuid());
      }
    }
  }

  private void initForMergeBranch(String mergeBranchUuid, DbSession dbSession) {
    hasMergeBranchAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, mergeBranchUuid).isPresent();

    if (hasMergeBranchAnalysis) {
      List<ComponentDto> components = dbClient.componentDao().selectByProjectUuid(mergeBranchUuid, dbSession);
      for (ComponentDto dto : components) {
        mergeBranchComponentsUuidsByKey.put(dto.getKey(), dto.uuid());
      }
    }
  }

  public boolean hasMergeBranchAnalysis() {
    lazyInit();
    return hasMergeBranchAnalysis;
  }

  public boolean hasTargetBranchAnalysis() {
    lazyInit();
    return hasTargetBranchAnalysis;
  }

  public String getMergeBranchName() {
    lazyInit();
    return mergeBranchName;
  }

  public boolean areTargetAndMergeBranchesDifferent() {
    lazyInit();
    return targetBranchUuid == null || !analysisMetadataHolder.getBranch().getMergeBranchUuid().equals(targetBranchUuid);
  }

  @CheckForNull
  public String getMergeBranchComponentUuid(String dbKey) {
    lazyInit();
    String cleanComponentKey = removeBranchAndPullRequestFromKey(dbKey);
    return mergeBranchComponentsUuidsByKey.get(cleanComponentKey);
  }

  @CheckForNull
  public String getTargetBranchComponentUuid(String dbKey) {
    lazyInit();
    String cleanComponentKey = removeBranchAndPullRequestFromKey(dbKey);
    return targetBranchComponentsUuidsByKey.get(cleanComponentKey);
  }
}
