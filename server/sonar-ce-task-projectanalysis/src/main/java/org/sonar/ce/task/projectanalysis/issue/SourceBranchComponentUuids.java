/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

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

/**
 * Cache a map between component keys and uuids in the source branch of a pull request
 */
public class SourceBranchComponentUuids {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private Map<String, String> sourceBranchComponentsUuidsByKey;
  private boolean hasSourceBranchAnalysis;

  public SourceBranchComponentUuids(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
  }

  private void lazyInit() {
    if (sourceBranchComponentsUuidsByKey == null) {
      sourceBranchComponentsUuidsByKey = new HashMap<>();

      if (analysisMetadataHolder.isPullRequest()) {
        try (DbSession dbSession = dbClient.openSession(false)) {
          initForSourceBranch(dbSession);
        }
      } else {
        hasSourceBranchAnalysis = false;
      }
    }
  }

  private void initForSourceBranch(DbSession dbSession) {
    Optional<BranchDto> branchDtoOpt = dbClient.branchDao().selectByBranchKey(dbSession, analysisMetadataHolder.getProject().getUuid(),
      analysisMetadataHolder.getBranch().getName());
    String sourceBranchUuid = branchDtoOpt.map(BranchDto::getUuid).orElse(null);
    hasSourceBranchAnalysis = sourceBranchUuid != null && dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, sourceBranchUuid).isPresent();
    if (hasSourceBranchAnalysis) {
      List<ComponentDto> targetComponents = dbClient.componentDao().selectByBranchUuid(sourceBranchUuid, dbSession);
      for (ComponentDto dto : targetComponents) {
        sourceBranchComponentsUuidsByKey.put(dto.getKey(), dto.uuid());
      }
    }
  }

  public boolean hasSourceBranchAnalysis() {
    lazyInit();
    return hasSourceBranchAnalysis;
  }

  @CheckForNull
  public String getSourceBranchComponentUuid(String key) {
    lazyInit();
    return sourceBranchComponentsUuidsByKey.get(key);
  }
}
