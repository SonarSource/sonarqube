/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.db.event.EventDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodParser;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;

public class NewCodePeriodResolver {
  private static final Logger LOG = LoggerFactory.getLogger(NewCodePeriodResolver.class);

  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public NewCodePeriodResolver(DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @CheckForNull
  public Period resolve(DbSession dbSession, String branchUuid, NewCodePeriodDto newCodePeriodDto, String projectVersion) {
    return toPeriod(newCodePeriodDto.getType(), newCodePeriodDto.getValue(), dbSession, projectVersion, branchUuid);
  }

  @CheckForNull
  private Period toPeriod(NewCodePeriodType type, @Nullable String value, DbSession dbSession, String projectVersion, String rootUuid) {
    switch (type) {
      case NUMBER_OF_DAYS:
        checkNotNullValue(value, type);
        Integer days = NewCodePeriodParser.parseDays(value);
        return resolveByDays(dbSession, rootUuid, days, value, analysisMetadataHolder.getAnalysisDate());
      case PREVIOUS_VERSION:
        return resolveByPreviousVersion(dbSession, rootUuid, projectVersion);
      case SPECIFIC_ANALYSIS:
        checkNotNullValue(value, type);
        return resolveBySpecificAnalysis(dbSession, rootUuid, value);
      case REFERENCE_BRANCH:
        checkNotNullValue(value, type);
        return resolveByReferenceBranch(value);
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  private static Period resolveByReferenceBranch(String value) {
    return newPeriod(NewCodePeriodType.REFERENCE_BRANCH, value, null);
  }

  private Period resolveBySpecificAnalysis(DbSession dbSession, String rootUuid, String value) {
    SnapshotDto baseline = dbClient.snapshotDao().selectByUuid(dbSession, value)
      .filter(t -> t.getRootComponentUuid().equals(rootUuid))
      .orElseThrow(() -> new IllegalStateException("Analysis '" + value + "' of project '" + rootUuid
        + "' defined as the baseline does not exist"));
    LOG.debug("Resolving new code period with a specific analysis");
    return newPeriod(NewCodePeriodType.SPECIFIC_ANALYSIS, value, baseline.getCreatedAt());
  }

  private Period resolveByPreviousVersion(DbSession dbSession, String rootComponentUuid, String projectVersion) {
    List<EventDto> versions = dbClient.eventDao().selectVersionsByMostRecentFirst(dbSession, rootComponentUuid);
    if (versions.isEmpty()) {
      return findOldestAnalysis(dbSession, rootComponentUuid);
    }

    String mostRecentVersion = Optional.ofNullable(versions.iterator().next().getName())
      .orElseThrow(() -> new IllegalStateException("selectVersionsByMostRecentFirst returned a DTO which didn't have a name"));

    if (versions.size() == 1 && projectVersion.equals(mostRecentVersion)) {
      return findOldestAnalysis(dbSession, rootComponentUuid);
    }

    return resolvePreviousVersion(dbSession, projectVersion, versions, mostRecentVersion);
  }

  private Period resolveByDays(DbSession dbSession, String rootUuid, Integer days, String value, long referenceDate) {
    checkPeriodProperty(days > 0, value, "number of days is <= 0");
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectAnalysesByQuery(dbSession, createCommonQuery(rootUuid)
      .setCreatedBefore(referenceDate).setSort(BY_DATE, ASC));

    Instant targetDate = DateUtils.addDays(Instant.ofEpochMilli(referenceDate), -days);
    LOG.debug("Resolving new code period by {} days: {}", days, supplierToString(() -> logDate(targetDate)));
    SnapshotDto snapshot = findNearestSnapshotToTargetDate(snapshots, targetDate);
    return newPeriod(NewCodePeriodType.NUMBER_OF_DAYS, String.valueOf((int) days), snapshot.getCreatedAt());
  }

  private Period resolvePreviousVersion(DbSession dbSession, String currentVersion, List<EventDto> versions, String mostRecentVersion) {
    EventDto previousVersion = versions.get(currentVersion.equals(mostRecentVersion) ? 1 : 0);
    LOG.debug("Resolving new code period by previous version: {}", previousVersion.getName());
    return newPeriod(dbSession, previousVersion);
  }

  private Period findOldestAnalysis(DbSession dbSession, String rootComponentUuid) {
    LOG.debug("Resolving first analysis as new code period as there is only one existing version");
    Optional<Period> period = dbClient.snapshotDao().selectOldestAnalysis(dbSession, rootComponentUuid)
      .map(dto -> newPeriod(NewCodePeriodType.PREVIOUS_VERSION, null, dto.getCreatedAt()));
    ensureNotOnFirstAnalysis(period.isPresent());
    return period.get();
  }

  private Period newPeriod(DbSession dbSession, EventDto previousVersion) {
    Optional<Period> period = dbClient.snapshotDao().selectByUuid(dbSession, previousVersion.getAnalysisUuid())
      .map(dto -> newPeriod(NewCodePeriodType.PREVIOUS_VERSION, dto.getProjectVersion(), dto.getCreatedAt()));
    if (!period.isPresent()) {
      throw new IllegalStateException(format("Analysis '%s' for version event '%s' has been deleted",
        previousVersion.getAnalysisUuid(), previousVersion.getName()));
    }
    return period.get();
  }

  private static Period newPeriod(NewCodePeriodType type, @Nullable String value, @Nullable Long date) {
    return new Period(type.name(), value, date);
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
    return new SnapshotQuery().setRootComponentUuid(projectUuid).setStatus(STATUS_PROCESSED);
  }

  private static SnapshotDto findNearestSnapshotToTargetDate(List<SnapshotDto> snapshots, Instant targetDate) {
    // FIXME shouldn't this be the first analysis after targetDate?
    Duration bestDuration = null;
    SnapshotDto nearest = null;

    ensureNotOnFirstAnalysis(!snapshots.isEmpty());

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
