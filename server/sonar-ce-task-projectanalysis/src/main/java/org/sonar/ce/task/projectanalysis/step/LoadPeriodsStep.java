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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.period.NewCodePeriodResolver;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;

/**
 * Populates the {@link PeriodHolder}
 * <p/>
 * Here is how these periods are computed :
 * - Read the new code period from DB
 * - Try to find the matching snapshots from the setting
 * - If a snapshot is found, a period is set to the repository, otherwise fail with MessageException
 */
public class LoadPeriodsStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final NewCodePeriodDao newCodePeriodDao;
  private final TreeRootHolder treeRootHolder;
  private final PeriodHolderImpl periodsHolder;
  private final DbClient dbClient;
  private final NewCodePeriodResolver resolver;

  public LoadPeriodsStep(AnalysisMetadataHolder analysisMetadataHolder, NewCodePeriodDao newCodePeriodDao, TreeRootHolder treeRootHolder,
                         PeriodHolderImpl periodsHolder, DbClient dbClient, NewCodePeriodResolver resolver) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.newCodePeriodDao = newCodePeriodDao;
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.dbClient = dbClient;
    this.resolver = resolver;
  }

  @Override
  public String getDescription() {
    return "Load new code period";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (analysisMetadataHolder.isFirstAnalysis() || !analysisMetadataHolder.isBranch()) {
      periodsHolder.setPeriod(null);
      return;
    }

    String projectUuid = getProjectBranchUuid();
    String branchUuid = treeRootHolder.getRoot().getUuid();
    String projectVersion = treeRootHolder.getRoot().getProjectAttributes().getProjectVersion();

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<NewCodePeriodDto> dto = firstPresent(Arrays.asList(
        () -> getBranchSetting(dbSession, projectUuid, branchUuid),
        () -> getProjectSetting(dbSession, projectUuid),
        () -> getGlobalSetting(dbSession)
      ));

      long analysisDate = analysisMetadataHolder.getAnalysisDate();
      Period period = dto.map(d -> resolver.resolve(dbSession, branchUuid, d, analysisDate, projectVersion))
        .orElseGet(() -> resolver.resolve(dbSession, branchUuid, NewCodePeriodDto.defaultInstance(), analysisDate, projectVersion));
      periodsHolder.setPeriod(period);
    }
  }

  private static <T> Optional<T> firstPresent(Collection<Supplier<Optional<T>>> suppliers) {
    for (Supplier<Optional<T>> supplier : suppliers) {
      Optional<T> result = supplier.get();
      if (result.isPresent()) {
        return result;
      }
    }
    return Optional.empty();
  }

  private Optional<NewCodePeriodDto> getBranchSetting(DbSession dbSession, String projectUuid, String branchUuid) {
    return newCodePeriodDao.selectByBranch(dbSession, projectUuid, branchUuid);
  }

  private Optional<NewCodePeriodDto> getProjectSetting(DbSession dbSession, String projectUuid) {
    return newCodePeriodDao.selectByProject(dbSession, projectUuid);
  }

  private Optional<NewCodePeriodDto> getGlobalSetting(DbSession dbSession) {
    return newCodePeriodDao.selectGlobal(dbSession);
  }

  private String getProjectBranchUuid() {
    return analysisMetadataHolder.getProject().getUuid();
  }
}
