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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.db.event.EventDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodParser;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;

/**
 * Populates the {@link PeriodHolder}
 * <p/>
 * Here is how these periods are computed :
 * - Read the period property ${@link org.sonar.core.config.CorePropertyDefinitions#LEAK_PERIOD}
 * - Try to find the matching snapshots from the property
 * - If a snapshot is found, a period is set to the repository, otherwise fail with MessageException
 */
public class LoadPeriodsStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(LoadPeriodsStep.class);

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final NewCodePeriodDao newCodePeriodDao;
  private final TreeRootHolder treeRootHolder;
  private final PeriodHolderImpl periodsHolder;
  private final DbClient dbClient;

  public LoadPeriodsStep(AnalysisMetadataHolder analysisMetadataHolder, NewCodePeriodDao newCodePeriodDao, TreeRootHolder treeRootHolder,
    PeriodHolderImpl periodsHolder, DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.newCodePeriodDao = newCodePeriodDao;
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.dbClient = dbClient;
  }

  @Override
  public String getDescription() {
    return "Load new code period";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (analysisMetadataHolder.isFirstAnalysis() || !analysisMetadataHolder.isLongLivingBranch()) {
      periodsHolder.setPeriod(null);
      return;
    }

    String projectUuid = getProjectBranchUuid();
    String branchUuid = treeRootHolder.getRoot().getUuid();
    String projectVersion = treeRootHolder.getRoot().getProjectAttributes().getProjectVersion();

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<NewCodePeriodDto> dto = firstPresent(
        () -> getBranchSetting(dbSession, projectUuid, branchUuid),
        () -> getProjectSetting(dbSession, projectUuid),
        () -> getGlobalSetting(dbSession));

      Period period = dto.map(d -> toPeriod(d.getType(), d.getValue(), dbSession, projectVersion, branchUuid))
        .orElseGet(() -> toPeriod(NewCodePeriodType.PREVIOUS_VERSION, null, dbSession, projectVersion, branchUuid));
      periodsHolder.setPeriod(period);
    }
  }

  private <T> Optional<T> firstPresent(Supplier<Optional<T>>... suppliers) {
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

  private Period toPeriod(NewCodePeriodType type, @Nullable String value, DbSession dbSession, String analysisProjectVersion, String rootUuid) {
    switch (type) {
      case NUMBER_OF_DAYS:
        Integer days = NewCodePeriodParser.parseDays(value);
        checkNotNullValue(value, type);
        return resolveByDays(dbSession, rootUuid, days, value);
      case PREVIOUS_VERSION:
        return resolveByPreviousVersion(dbSession, rootUuid, analysisProjectVersion);
      case SPECIFIC_ANALYSIS:
        checkNotNullValue(value, type);
        return resolveBySpecificAnalysis(dbSession, rootUuid, value);
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  private String getProjectBranchUuid() {
    return analysisMetadataHolder.getProject().getUuid();
  }

  private Period resolveBySpecificAnalysis(DbSession dbSession, String rootUuid, String value) {
    SnapshotDto baseline = dbClient.snapshotDao().selectByUuid(dbSession, value)
      .filter(t -> t.getComponentUuid().equals(rootUuid))
      .orElseThrow(() -> new IllegalStateException("Analysis '" + value + "' of project '" + rootUuid
        + "' defined as the baseline does not exist"));
    LOG.debug("Resolving new code period with a specific analysis");
    return newPeriod(NewCodePeriodType.SPECIFIC_ANALYSIS, value, Instant.ofEpochMilli(baseline.getCreatedAt()));
  }

  private Period resolveByPreviousVersion(DbSession dbSession, String projectUuid, String analysisProjectVersion) {
    List<EventDto> versions = dbClient.eventDao().selectVersionsByMostRecentFirst(dbSession, projectUuid);
    if (versions.isEmpty()) {
      return findOldestAnalysis(dbSession, projectUuid);
    }

    String mostRecentVersion = Optional.ofNullable(versions.iterator().next().getName())
      .orElseThrow(() -> new IllegalStateException("selectVersionsByMostRecentFirst returned a DTO which didn't have a name"));

    if (versions.size() == 1) {
      return findOldestAnalysis(dbSession, projectUuid);
    }
    return resolvePreviousVersion(dbSession, analysisProjectVersion, versions, mostRecentVersion);
  }

  private Period resolveByDays(DbSession dbSession, String rootUuid, Integer days, String propertyValue) {
    checkPeriodProperty(days > 0, propertyValue, "number of days is <= 0");
    long analysisDate = analysisMetadataHolder.getAnalysisDate();
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectAnalysesByQuery(dbSession, createCommonQuery(rootUuid)
      .setCreatedBefore(analysisDate).setSort(BY_DATE, ASC));

    ensureNotOnFirstAnalysis(!snapshots.isEmpty());
    Instant targetDate = DateUtils.addDays(Instant.ofEpochMilli(analysisDate), -days);
    LOG.debug("Resolving new code period by {} days: {}", days, supplierToString(() -> logDate(targetDate)));
    SnapshotDto snapshot = findNearestSnapshotToTargetDate(snapshots, targetDate);
    return newPeriod(NewCodePeriodType.NUMBER_OF_DAYS, String.valueOf((int) days), Instant.ofEpochMilli(snapshot.getCreatedAt()));
  }

  private Period resolvePreviousVersion(DbSession dbSession, String currentVersion, List<EventDto> versions, String mostRecentVersion) {
    EventDto previousVersion = versions.get(currentVersion.equals(mostRecentVersion) ? 1 : 0);
    LOG.debug("Resolving new code period by previous version: {}", previousVersion.getName());
    return newPeriod(dbSession, previousVersion);
  }

  private Period findOldestAnalysis(DbSession dbSession, String projectUuid) {
    LOG.debug("Resolving first analysis as new code period as there is only one existing version");
    Optional<Period> period = dbClient.snapshotDao().selectOldestSnapshot(dbSession, projectUuid)
      .map(dto -> newPeriod(NewCodePeriodType.PREVIOUS_VERSION, null, Instant.ofEpochMilli(dto.getCreatedAt())));
    ensureNotOnFirstAnalysis(period.isPresent());
    return period.get();
  }

  private Period newPeriod(DbSession dbSession, EventDto previousVersion) {
    Optional<Period> period = dbClient.snapshotDao().selectByUuid(dbSession, previousVersion.getAnalysisUuid())
      .map(dto -> newPeriod(NewCodePeriodType.PREVIOUS_VERSION, dto.getProjectVersion(), Instant.ofEpochMilli(dto.getCreatedAt())));
    if (!period.isPresent()) {
      throw new IllegalStateException(format("Analysis '%s' for version event '%s' has been deleted",
        previousVersion.getAnalysisUuid(), previousVersion.getName()));
    }
    return period.get();
  }

  private static Period newPeriod(NewCodePeriodType type, @Nullable String value, Instant instant) {
    return new Period(type.name(), value, instant.toEpochMilli());
  }

  private static Object supplierToString(Supplier<String> s) {
    return new Object() {
      @Override
      public String toString() {
        return s.get();
      }
    };
  }

  private static SnapshotQuery createCommonQuery(String projectUuid) {
    return new SnapshotQuery().setComponentUuid(projectUuid).setStatus(STATUS_PROCESSED);
  }

  private static SnapshotDto findNearestSnapshotToTargetDate(List<SnapshotDto> snapshots, Instant targetDate) {
    // FIXME shouldn't this be the first analysis after targetDate?
    Duration bestDuration = null;
    SnapshotDto nearest = null;
    for (SnapshotDto snapshot : snapshots) {
      Instant createdAt = Instant.ofEpochMilli(snapshot.getCreatedAt());
      Duration duration = Duration.between(targetDate, createdAt).abs();
      if (bestDuration == null || duration.compareTo(bestDuration) <= 0) {
        bestDuration = duration;
        nearest = snapshot;
      }
    }
    return nearest;
  }

  private static void checkPeriodProperty(boolean test, String propertyValue, String testDescription, Object... args) {
    if (!test) {
      LOG.debug("Invalid code period '{}': {}", propertyValue, supplierToString(() -> format(testDescription, args)));
      throw MessageException.of(format("Invalid new code period. '%s' is not one of: " +
        "integer > 0, date before current analysis j, \"previous_version\", or version string that exists in the project' \n" +
        "Please contact a project administrator to correct this setting", propertyValue));
    }
  }

  private static void ensureNotOnFirstAnalysis(boolean expression) {
    checkState(expression, "Attempting to resolve period while no analysis exist for project");
  }

  private static void checkNotNullValue(@Nullable String value, NewCodePeriodType type) {
    checkNotNull(value, "Value can't be null with type %s", type);
  }

  private static String logDate(Instant instant) {
    return DateUtils.formatDate(instant.truncatedTo(ChronoUnit.SECONDS));
  }
}
