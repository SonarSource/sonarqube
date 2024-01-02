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
package org.sonar.server.issue;

import java.time.Clock;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.api.measures.CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

public class NewCodePeriodResolver {

  private final DbClient dbClient;
  private final Clock clock;

  public NewCodePeriodResolver(DbClient dbClient, Clock clock) {
    this.dbClient = dbClient;
    this.clock = clock;
  }

  public ResolvedNewCodePeriod resolveForProjectAndBranch(DbSession dbSession, String projectKey, String branchKey) {
    ComponentDto componentDto = dbClient.componentDao().selectByKeyAndBranch(dbSession, projectKey, branchKey)
      .orElseThrow(() -> new IllegalStateException(format("Could not find component for project: %s,  branch: %s", projectKey, branchKey)));
    Optional<SnapshotDto> snapshot = getLastAnalysis(dbSession, componentDto);
    if (snapshot.isPresent() && isLastAnalysisFromReAnalyzedReferenceBranch(dbSession, snapshot.get())) {
      return new ResolvedNewCodePeriod(REFERENCE_BRANCH, null);
    } else {
      // if last analysis has no period date, then no issue should be considered new.
      long createdAfterFromSnapshot = snapshot.map(SnapshotDto::getPeriodDate).orElse(clock.millis());
      return new ResolvedNewCodePeriod(snapshot.map(SnapshotDto::getPeriodMode).map(NewCodePeriodType::valueOf).orElse(null), createdAfterFromSnapshot);
    }
  }

  private Optional<SnapshotDto> getLastAnalysis(DbSession dbSession, ComponentDto component) {
    return dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.uuid());
  }

  private boolean isLastAnalysisFromReAnalyzedReferenceBranch(DbSession dbSession, SnapshotDto snapshot) {
    return isLastAnalysisUsingReferenceBranch(snapshot) &&
      isLastAnalysisFromSonarQube94Onwards(dbSession, snapshot.getRootComponentUuid());
  }

  private boolean isLastAnalysisFromSonarQube94Onwards(DbSession dbSession, String componentUuid) {
    return dbClient.liveMeasureDao().selectMeasure(dbSession, componentUuid, ANALYSIS_FROM_SONARQUBE_9_4_KEY).isPresent();
  }

  private static boolean isLastAnalysisUsingReferenceBranch(SnapshotDto snapshot) {
    return !isNullOrEmpty(snapshot.getPeriodMode()) && REFERENCE_BRANCH.name().equals(snapshot.getPeriodMode());
  }

  public record ResolvedNewCodePeriod(@Nullable NewCodePeriodType type, @Nullable Long periodDate) {

  }
}
