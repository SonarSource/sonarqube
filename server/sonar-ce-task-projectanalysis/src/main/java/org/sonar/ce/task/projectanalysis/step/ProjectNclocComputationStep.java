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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;

public class ProjectNclocComputationStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;

  public ProjectNclocComputationStep(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectUuid = analysisMetadataHolder.getProject().getUuid();
      List<String> branchUuids = dbClient.branchDao().selectByProjectUuid(dbSession, projectUuid).stream()
        .map(BranchDto::getUuid)
        .toList();
      long maxncloc = dbClient.measureDao().findNclocOfBiggestBranch(dbSession, branchUuids);
      dbClient.projectDao().updateNcloc(dbSession, projectUuid, maxncloc);
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Compute total Project ncloc";
  }
}
