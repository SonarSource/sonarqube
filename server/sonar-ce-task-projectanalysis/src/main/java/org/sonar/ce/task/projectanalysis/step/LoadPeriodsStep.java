/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;
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
  private final TreeRootHolder treeRootHolder;
  private final PeriodHolderImpl periodsHolder;
  private final System2 system2;
  private final DbClient dbClient;
  private final ConfigurationRepository configRepository;

  public LoadPeriodsStep(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder treeRootHolder, PeriodHolderImpl periodsHolder,
    System2 system2, DbClient dbClient, ConfigurationRepository configRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.system2 = system2;
    this.dbClient = dbClient;
    this.configRepository = configRepository;
  }

  @Override
  public String getDescription() {
    return "Load new code period";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (analysisMetadataHolder.isFirstAnalysis()) {
      periodsHolder.setPeriod(null);
      return;
    }

    periodsHolder.setPeriod(resolvePeriod(treeRootHolder.getRoot()).orElse(null));
  }

  private Optional<Period> resolvePeriod(Component projectOrView) {
    String currentVersion = projectOrView.getProjectAttributes().getVersion();
    Optional<String> propertyValue = configRepository.getConfiguration().get(LEAK_PERIOD)
      .filter(t -> !t.isEmpty());
    checkPeriodProperty(propertyValue.isPresent(), "", "property is undefined or value is empty");

    try (DbSession dbSession = dbClient.openSession(false)) {
      return resolve(dbSession, projectOrView.getUuid(), currentVersion, propertyValue.get());
    }
  }

  public Optional<Period> resolve(String projectUuid, String analysisProjectVersion) {
    Optional<String> propertyValue = configRepository.getConfiguration().get(LEAK_PERIOD)
      .filter(t -> !t.isEmpty());
    checkPeriodProperty(propertyValue.isPresent(), "", "property is undefined or value is empty");

    try (DbSession dbSession = dbClient.openSession(false)) {
      return resolve(dbSession, projectUuid, analysisProjectVersion, propertyValue.get());
    }
  }

  private Optional<Period> resolve(DbSession dbSession, String projectUuid, String analysisProjectVersion, String propertyValue) {
    Integer days = parseDaysQuietly(propertyValue);
    if (days != null) {
      return resolveByDays(dbSession, projectUuid, days, propertyValue);
    }
    Date date = parseDate(propertyValue);
    if (date != null) {
      return resolveByDate(dbSession, projectUuid, date, propertyValue);
    }

    List<EventDto> versions = dbClient.eventDao().selectVersionsByMostRecentFirst(dbSession, projectUuid);
    if (versions.isEmpty()) {
      return resolveWhenNoExistingVersion(dbSession, projectUuid, analysisProjectVersion, propertyValue);
    }

    String mostRecentVersion = Optional.ofNullable(versions.iterator().next().getName())
      .orElseThrow(() -> new IllegalStateException("selectVersionsByMostRecentFirst returned a DTO which didn't have a name"));
    if (versions.size() == 1) {
      return resolveWhenOnlyOneExistingVersion(dbSession, projectUuid, mostRecentVersion, propertyValue);
    }

    boolean previousVersionPeriod = LEAK_PERIOD_MODE_PREVIOUS_VERSION.equals(propertyValue);
    if (previousVersionPeriod) {
      return resolvePreviousVersion(dbSession, analysisProjectVersion, versions, mostRecentVersion);
    }

    return resolveVersion(dbSession, versions, propertyValue);
  }

  @CheckForNull
  private static Date parseDate(String propertyValue) {
    try {
      LocalDate localDate = LocalDate.parse(propertyValue);
      return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    } catch (DateTimeParseException e) {
      boolean invalidDate = e.getCause() == null || e.getCause() == e || !e.getCause().getMessage().contains("Invalid date");
      checkPeriodProperty(invalidDate, propertyValue, "Invalid date");
      return null;
    }
  }

  private Optional<Period> resolveByDays(DbSession dbSession, String projectUuid, Integer days, String propertyValue) {
    checkPeriodProperty(days > 0, propertyValue, "number of days is <= 0");
    long analysisDate = analysisMetadataHolder.getAnalysisDate();
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectAnalysesByQuery(dbSession, createCommonQuery(projectUuid).setCreatedBefore(analysisDate).setSort(BY_DATE, ASC));
    ensureNotOnFirstAnalysis(!snapshots.isEmpty());

    long targetDate = DateUtils.addDays(new Date(analysisDate), -days).getTime();
    LOG.debug("Resolving new code period by {} days: {}", days, async(() -> logDate(targetDate)));
    SnapshotDto snapshot = findNearestSnapshotToTargetDate(snapshots, targetDate);

    return Optional.of(newPeriod(LEAK_PERIOD_MODE_DAYS, String.valueOf((int) days), snapshot));
  }

  private Optional<Period> resolveByDate(DbSession dbSession, String projectUuid, Date date, String propertyValue) {
    long now = system2.now();
    checkPeriodProperty(date.compareTo(new Date(now)) <= 0, propertyValue,
      "date is in the future (now: '%s')", async(() -> logDate(now)));

    LOG.debug("Resolving new code period by date: {}", async(() -> logDate(date)));
    Optional<Period> period = findFirstSnapshot(dbSession, createCommonQuery(projectUuid).setCreatedAfter(date.getTime()).setSort(BY_DATE, ASC))
      .map(dto -> newPeriod(LEAK_PERIOD_MODE_DATE, DateUtils.formatDate(date), dto));

    checkPeriodProperty(period.isPresent(), propertyValue, "No analysis found created after date '%s'", async(() -> logDate(date)));

    return period;
  }

  private Optional<Period> resolveWhenNoExistingVersion(DbSession dbSession, String projectUuid, String currentVersion, String propertyValue) {
    LOG.debug("Resolving first analysis as new code period as there is no existing version");

    boolean previousVersionPeriod = LEAK_PERIOD_MODE_PREVIOUS_VERSION.equals(propertyValue);
    boolean currentVersionPeriod = currentVersion.equals(propertyValue);
    checkPeriodProperty(previousVersionPeriod || currentVersionPeriod, propertyValue,
      "No existing version. Property should be either '%s' or the current version '%s' (actual: '%s')",
      LEAK_PERIOD_MODE_PREVIOUS_VERSION, currentVersion, propertyValue);

    String periodMode = previousVersionPeriod ? LEAK_PERIOD_MODE_PREVIOUS_VERSION : LEAK_PERIOD_MODE_VERSION;
    return findOldestAnalysis(dbSession, periodMode, projectUuid);
  }

  private Optional<Period> resolveWhenOnlyOneExistingVersion(DbSession dbSession, String projectUuid, String mostRecentVersion, String propertyValue) {
    boolean previousVersionPeriod = LEAK_PERIOD_MODE_PREVIOUS_VERSION.equals(propertyValue);
    LOG.debug("Resolving first analysis as new code period as there is only one existing version");

    // only one existing version. Period must either be PREVIOUS_VERSION or the only valid version: the only existing one
    checkPeriodProperty(previousVersionPeriod || propertyValue.equals(mostRecentVersion), propertyValue,
      "Only one existing version, but period is neither %s nor this one version '%s' (actual: '%s')",
      LEAK_PERIOD_MODE_PREVIOUS_VERSION, mostRecentVersion, propertyValue);

    return findOldestAnalysis(dbSession, LEAK_PERIOD_MODE_PREVIOUS_VERSION, projectUuid);
  }

  private Optional<Period> findOldestAnalysis(DbSession dbSession, String periodMode, String projectUuid) {
    Optional<Period> period = dbClient.snapshotDao().selectOldestSnapshot(dbSession, projectUuid)
      .map(dto -> newPeriod(periodMode, null, dto));
    ensureNotOnFirstAnalysis(period.isPresent());
    return period;
  }

  private Optional<Period> resolvePreviousVersion(DbSession dbSession, String currentVersion, List<EventDto> versions, String mostRecentVersion) {
    EventDto previousVersion = versions.get(currentVersion.equals(mostRecentVersion) ? 1 : 0);
    LOG.debug("Resolving new code period by previous version: {}", previousVersion.getName());
    return newPeriod(dbSession, LEAK_PERIOD_MODE_PREVIOUS_VERSION, previousVersion);
  }

  private Optional<Period> resolveVersion(DbSession dbSession, List<EventDto> versions, String propertyValue) {
    LOG.debug("Resolving new code period by version: {}", propertyValue);
    Optional<EventDto> version = versions.stream().filter(t -> propertyValue.equals(t.getName())).findFirst();
    checkPeriodProperty(version.isPresent(), propertyValue,
      "version is none of the existing ones: %s", async(() -> toVersions(versions)));

    return newPeriod(dbSession, LEAK_PERIOD_MODE_VERSION, version.get());
  }

  private Optional<Period> newPeriod(DbSession dbSession, String periodMode, EventDto previousVersion) {
    Optional<Period> period = dbClient.snapshotDao().selectByUuid(dbSession, previousVersion.getAnalysisUuid())
      .map(dto -> newPeriod(periodMode, previousVersion.getName(), dto));
    if (!period.isPresent()) {
      throw new IllegalStateException(format("Analysis '%s' for version event '%s' has been deleted",
        previousVersion.getAnalysisUuid(), previousVersion.getName()));
    }
    return period;
  }

  private static String toVersions(List<EventDto> versions) {
    return Arrays.toString(versions.stream().map(EventDto::getName).toArray(String[]::new));
  }

  public static Object async(Supplier<String> s) {
    return new Object() {
      @Override
      public String toString() {
        return s.get();
      }
    };
  }

  private static Period newPeriod(String mode, @Nullable String modeParameter, SnapshotDto dto) {
    return new Period(mode, modeParameter, dto.getCreatedAt(), dto.getUuid());
  }

  private static void checkPeriodProperty(boolean test, String propertyValue, String testDescription, Object... args) {
    if (!test) {
      LOG.debug("Invalid code period '{}': {}", propertyValue, async(() -> format(testDescription, args)));
      throw MessageException.of(format("Invalid new code period. '%s' is not one of: " +
        "integer > 0, date before current analysis j, \"previous_version\", or version string that exists in the project' \n" +
        "Please contact a project administrator to correct this setting", propertyValue));
    }
  }

  private Optional<SnapshotDto> findFirstSnapshot(DbSession session, SnapshotQuery query) {
    return dbClient.snapshotDao().selectAnalysesByQuery(session, query)
      .stream()
      .findFirst();
  }

  private static void ensureNotOnFirstAnalysis(boolean expression) {
    checkState(expression, "Attempting to resolve period while no analysis exist for project");
  }

  @CheckForNull
  private static Integer parseDaysQuietly(String property) {
    try {
      return Integer.parseInt(property);
    } catch (NumberFormatException e) {
      // Nothing to, it means that the property is not a number of days
      return null;
    }
  }

  private static SnapshotDto findNearestSnapshotToTargetDate(List<SnapshotDto> snapshots, Long targetDate) {
    long bestDistance = Long.MAX_VALUE;
    SnapshotDto nearest = null;
    for (SnapshotDto snapshot : snapshots) {
      long distance = Math.abs(snapshot.getCreatedAt() - targetDate);
      if (distance <= bestDistance) {
        bestDistance = distance;
        nearest = snapshot;
      }
    }
    return nearest;
  }

  private static SnapshotQuery createCommonQuery(String projectUuid) {
    return new SnapshotQuery().setComponentUuid(projectUuid).setStatus(STATUS_PROCESSED);
  }

  private static String logDate(long date) {
    return logDate(new Date(date));
  }

  private static String logDate(Date date1) {
    return DateUtils.formatDate(Date.from(date1.toInstant().truncatedTo(ChronoUnit.SECONDS)));
  }
}
