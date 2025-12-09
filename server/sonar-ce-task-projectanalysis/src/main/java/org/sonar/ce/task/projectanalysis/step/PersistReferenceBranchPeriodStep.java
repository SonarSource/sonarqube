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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.projectanalysis.period.PeriodOrigin;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

public class PersistReferenceBranchPeriodStep implements ComputationStep {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersistReferenceBranchPeriodStep.class);

  private final PeriodHolder periodHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;

  public PersistReferenceBranchPeriodStep(PeriodHolder periodHolder, AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, TreeRootHolder treeRootHolder) {
    this.periodHolder = periodHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public String getDescription() {
    return "Persist or update reference branch new code period";
  }

  @Override
  public void execute(Context context) {
    if (shouldExecute()) {
      executePersistPeriodStep();
    }
  }

  private boolean shouldExecute() {
    return analysisMetadataHolder.isBranch() && periodHolder.hasPeriod()
      && periodHolder.getPeriodOrigin() == PeriodOrigin.SCANNER;
  }

  void executePersistPeriodStep() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectUuid = analysisMetadataHolder.getProject().getUuid();
      String branchUuid = treeRootHolder.getRoot().getUuid();

      dbClient.newCodePeriodDao()
        .selectByBranch(dbSession, projectUuid, branchUuid)
        .ifPresentOrElse(
          existingNewCodePeriod -> updateNewCodePeriodIfNeeded(dbSession, existingNewCodePeriod),
          () -> createNewCodePeriod(dbSession, branchUuid)
        );
    }
  }

  private void updateNewCodePeriodIfNeeded(DbSession dbSession, NewCodePeriodDto newCodePeriodDto) {
    if (shouldUpdateNewCodePeriod(newCodePeriodDto)) {
      LOGGER.debug("Updating reference branch new code period '{}' for project '{}' and branch '{}'",
        periodHolder.getPeriod().getModeParameter(), analysisMetadataHolder.getProject().getName(), analysisMetadataHolder.getBranch().getName());
      newCodePeriodDto.setValue(periodHolder.getPeriod().getModeParameter());
      newCodePeriodDto.setType(NewCodePeriodType.REFERENCE_BRANCH);
      dbClient.newCodePeriodDao().update(dbSession, newCodePeriodDto);
      dbSession.commit();
    }
  }

  private boolean shouldUpdateNewCodePeriod(NewCodePeriodDto existingNewCodePeriod) {
    return existingNewCodePeriod.getType() != NewCodePeriodType.REFERENCE_BRANCH
      || !Objects.equals(existingNewCodePeriod.getValue(), periodHolder.getPeriod().getModeParameter());
  }

  private void createNewCodePeriod(DbSession dbSession, String branchUuid) {
    LOGGER.debug("Persisting reference branch new code period '{}' for project '{}' and branch '{}'",
      periodHolder.getPeriod().getModeParameter(), analysisMetadataHolder.getProject().getName(), analysisMetadataHolder.getBranch().getName());
    dbClient.newCodePeriodDao().insert(dbSession, buildNewCodePeriodDto(branchUuid));
    dbSession.commit();
  }

  private NewCodePeriodDto buildNewCodePeriodDto(String branchUuid) {
    return new NewCodePeriodDto()
      .setProjectUuid(analysisMetadataHolder.getProject().getUuid())
      .setBranchUuid(branchUuid)
      .setType(NewCodePeriodType.REFERENCE_BRANCH)
      .setValue(periodHolder.getPeriod().getModeParameter());
  }

}
