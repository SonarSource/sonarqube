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

/**
 * Cache a map between component keys and uuids in the reference branch
 */
public class ReferenceBranchComponentUuids {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private Map<String, String> referenceBranchComponentsUuidsByKey;
  private String referenceBranchName;
  private boolean hasReferenceBranchAnalysis;

  public ReferenceBranchComponentUuids(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
  }

  private void lazyInit() {
    if (referenceBranchComponentsUuidsByKey == null) {
      String referenceBranchUuid = analysisMetadataHolder.getBranch().getReferenceBranchUuid();

      referenceBranchComponentsUuidsByKey = new HashMap<>();

      try (DbSession dbSession = dbClient.openSession(false)) {

        Optional<BranchDto> opt = dbClient.branchDao().selectByUuid(dbSession, referenceBranchUuid);
        checkState(opt.isPresent(), "Reference branch '%s' does not exist", referenceBranchUuid);
        referenceBranchName = opt.get().getKey();

        init(referenceBranchUuid, dbSession);
      }
    }
  }

  private void init(String referenceBranchUuid, DbSession dbSession) {
    hasReferenceBranchAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, referenceBranchUuid).isPresent();

    if (hasReferenceBranchAnalysis) {
      List<ComponentDto> components = dbClient.componentDao().selectByBranchUuid(referenceBranchUuid, dbSession);
      for (ComponentDto dto : components) {
        referenceBranchComponentsUuidsByKey.put(dto.getKey(), dto.uuid());
      }
    }
  }

  public boolean hasReferenceBranchAnalysis() {
    lazyInit();
    return hasReferenceBranchAnalysis;
  }

  public String getReferenceBranchName() {
    lazyInit();
    return referenceBranchName;
  }

  @CheckForNull
  public String getComponentUuid(String key) {
    lazyInit();
    return referenceBranchComponentsUuidsByKey.get(key);
  }
}
