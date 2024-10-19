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
package org.sonar.ce.task.projectanalysis.period;

import com.google.common.base.Preconditions;
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
import org.sonar.db.newcodeperiod.NewCodePeriodType;

/**
 * Cache a map between component keys and uuids in the reference branch
 */
public class NewCodeReferenceBranchComponentUuids {
  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodHolder periodHolder;
  private Map<String, String> referenceBranchComponentsUuidsByKey;

  public NewCodeReferenceBranchComponentUuids(AnalysisMetadataHolder analysisMetadataHolder, PeriodHolder periodHolder, DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodHolder = periodHolder;
    this.dbClient = dbClient;
  }

  private void lazyInit() {
    if (referenceBranchComponentsUuidsByKey == null) {
      Preconditions.checkState(periodHolder.hasPeriod() && periodHolder.getPeriod().getMode().equals(NewCodePeriodType.REFERENCE_BRANCH.name()));
      referenceBranchComponentsUuidsByKey = new HashMap<>();

      try (DbSession dbSession = dbClient.openSession(false)) {
        String referenceKey = periodHolder.getPeriod().getModeParameter() != null ? periodHolder.getPeriod().getModeParameter() : "";
        Optional<BranchDto> opt = dbClient.branchDao().selectByBranchKey(dbSession, analysisMetadataHolder.getProject().getUuid(), referenceKey);
        if (opt.isPresent()) {
          init(opt.get().getUuid(), dbSession);
        }
      }
    }
  }

  private void init(String referenceBranchUuid, DbSession dbSession) {
    boolean hasReferenceBranchAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, referenceBranchUuid).isPresent();

    if (hasReferenceBranchAnalysis) {
      List<ComponentDto> components = dbClient.componentDao().selectByBranchUuid(referenceBranchUuid, dbSession);
      for (ComponentDto dto : components) {
        referenceBranchComponentsUuidsByKey.put(dto.getKey(), dto.uuid());
      }
    }
  }

  @CheckForNull
  public String getComponentUuid(String key) {
    lazyInit();
    return referenceBranchComponentsUuidsByKey.get(key);
  }
}
