/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.Optional;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

public class UpdateMainBranchStep implements ComputationStep {

  private static final Logger LOGGER = Loggers.get(UpdateMainBranchStep.class);

  private final BatchReportReader batchReportReader;
  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public UpdateMainBranchStep(BatchReportReader batchReportReader, DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder) {
    this.batchReportReader = batchReportReader;
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(Context context) {

    if (!analysisMetadataHolder.isFirstAnalysis()) {
      return;
    }

    String gitDefaultMainBranch = batchReportReader.readMetadata().getGitDefaultMainBranch();
    if (gitDefaultMainBranch.isEmpty()) {
      LOGGER.debug("GIT default main branch detected is empty");
      return;
    }
    LOGGER.debug(String.format("GIT default main branch detected is [%s]", gitDefaultMainBranch));
    updateProjectDefaultMainBranch(gitDefaultMainBranch);
  }

  private void updateProjectDefaultMainBranch(String gitDefaultMainBranch) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = analysisMetadataHolder.getProject().getKey();
      Optional<ProjectDto> projectDto = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
      if (!projectDto.isPresent()) {
        throw new IllegalStateException(String.format("root component key [%s] is not a project", projectKey));
      }
      LOGGER.info(String.format("updating project [%s] default main branch to [%s]", projectKey, gitDefaultMainBranch));
      dbClient.branchDao().updateMainBranchName(dbSession, projectDto.get().getUuid(), gitDefaultMainBranch);
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Update the project main branch name, based on GIT information. Only for the first project's analysis.";
  }
}
