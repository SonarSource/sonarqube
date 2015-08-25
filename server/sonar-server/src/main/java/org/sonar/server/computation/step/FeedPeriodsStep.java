/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderImpl;

import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.DESC;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.server.computation.component.CrawlerDepthLimit.reportMaxDepth;

/**
 * Populates the {@link org.sonar.server.computation.period.PeriodsHolder}
 * <p/>
 * Here is how these periods are computed :
 * - Read the 5 period properties ${@link CoreProperties#TIMEMACHINE_PERIOD_PREFIX}
 * - Try to find the matching snapshots from the properties
 * - If a snapshot is found, a new period is added to the repository
 */
public class FeedPeriodsStep implements ComputationStep {

  private static final Logger LOG = Loggers.get(FeedPeriodsStep.class);

  private static final int NUMBER_OF_PERIODS = 5;

  private final DbClient dbClient;
  private final SettingsRepository settingsRepository;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodsHolderImpl periodsHolder;

  public FeedPeriodsStep(DbClient dbClient, SettingsRepository settingsRepository, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    PeriodsHolderImpl periodsHolder) {
    this.dbClient = dbClient;
    this.settingsRepository = settingsRepository;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(reportMaxDepth(PROJECT).withViewsMaxDepth(VIEW), PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          execute(project);
        }

        @Override
        public void visitView(Component view) {
          execute(view);
        }
      }).visit(treeRootHolder.getRoot());
  }

  public void execute(Component projectOrView) {
    DbSession session = dbClient.openSession(false);
    try {
      periodsHolder.setPeriods(buildPeriods(projectOrView, session));
    } finally {
      dbClient.closeSession(session);
    }
  }

  private List<Period> buildPeriods(Component projectOrView, DbSession session) {
    Optional<ComponentDto> projectDto = dbClient.componentDao().selectByKey(session, projectOrView.getKey());
    // No project on first analysis, no period
    if (!projectDto.isPresent()) {
      return Collections.emptyList();
    }

    boolean isReportType = projectOrView.getType().isReportType();
    PeriodResolver periodResolver = new PeriodResolver(session, projectDto.get().getId(), analysisMetadataHolder.getAnalysisDate().getTime(),
      isReportType ? projectOrView.getReportAttributes().getVersion() : null,
      isReportType ? Qualifiers.PROJECT : Qualifiers.VIEW);

    Settings settings = settingsRepository.getSettings(projectOrView);
    List<Period> periods = new ArrayList<>(5);
    for (int index = 1; index <= NUMBER_OF_PERIODS; index++) {
      Period period = periodResolver.resolve(index, settings);
      // SONAR-4700 Add a past snapshot only if it exists
      if (period != null) {
        periods.add(period);
      }
    }
    return periods;
  }

  private class PeriodResolver {

    private final DbSession session;
    private final long projectId;
    private final long analysisDate;
    @CheckForNull
    private final String currentVersion;
    private final String qualifier;

    public PeriodResolver(DbSession session, long projectId, long analysisDate, @Nullable String currentVersion, String qualifier) {
      this.session = session;
      this.projectId = projectId;
      this.analysisDate = analysisDate;
      this.currentVersion = currentVersion;
      this.qualifier = qualifier;
    }

    @CheckForNull
    public Period resolve(int index, Settings settings) {
      String propertyValue = getPropertyValue(qualifier, settings, index);
      if (StringUtils.isBlank(propertyValue)) {
        return null;
      }
      Period period = resolve(index, propertyValue);
      if (period == null && StringUtils.isNotBlank(propertyValue)) {
        LOG.debug("Property " + CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + " is not valid: " + propertyValue);
      }
      return period;
    }

    @CheckForNull
    private Period resolve(int index, String property) {
      Integer days = tryToResolveByDays(property);
      if (days != null) {
        return findByDays(index, days);
      }
      Date date = tryToResolveByDate(property);
      if (date != null) {
        return findByDate(index, date);
      }
      if (StringUtils.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, property)) {
        return findByPreviousAnalysis(index);
      }
      if (StringUtils.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, property)) {
        return findByPreviousVersion(index);
      }
      return findByVersion(index, property);
    }

    private Period findByDate(int index, Date date) {
      SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectId).setCreatedAfter(date.getTime()).setSort(BY_DATE, ASC));
      if (snapshot == null) {
        return null;
      }
      LOG.debug("Compare to date {} (analysis of {})", formatDate(date.getTime()), formatDate(snapshot.getCreatedAt()));
      return new Period(index, CoreProperties.TIMEMACHINE_MODE_DATE, DateUtils.formatDate(date), snapshot.getCreatedAt(), snapshot.getId());
    }

    @CheckForNull
    private Period findByDays(int index, int days) {
      List<SnapshotDto> snapshots = dbClient.snapshotDao().selectSnapshotsByQuery(session, createCommonQuery(projectId).setCreatedBefore(analysisDate).setSort(BY_DATE, ASC));
      long targetDate = DateUtils.addDays(new Date(analysisDate), -days).getTime();
      SnapshotDto snapshot = findNearestSnapshotToTargetDate(snapshots, targetDate);
      if (snapshot == null) {
        return null;
      }
      LOG.debug("Compare over {} days ({}, analysis of {})", String.valueOf(days), formatDate(targetDate), formatDate(snapshot.getCreatedAt()));
      return new Period(index, CoreProperties.TIMEMACHINE_MODE_DAYS, String.valueOf(days), snapshot.getCreatedAt(), snapshot.getId());
    }

    @CheckForNull
    private Period findByPreviousAnalysis(int index) {
      SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectId).setCreatedBefore(analysisDate).setIsLast(true).setSort(BY_DATE, DESC));
      if (snapshot == null) {
        return null;
      }
      LOG.debug("Compare to previous analysis ({})", formatDate(snapshot.getCreatedAt()));
      return new Period(index, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, formatDate(snapshot.getCreatedAt()), snapshot.getCreatedAt(), snapshot.getId());
    }

    @CheckForNull
    private Period findByPreviousVersion(int index) {
      if (currentVersion == null) {
        return null;
      }
      List<SnapshotDto> snapshotDtos = dbClient.snapshotDao().selectPreviousVersionSnapshots(session, projectId, currentVersion);
      if (snapshotDtos.isEmpty()) {
        return null;
      }
      SnapshotDto snapshotDto = snapshotDtos.get(0);
      LOG.debug("Compare to previous version ({})", formatDate(snapshotDto.getCreatedAt()));
      return new Period(index, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, snapshotDto.getVersion(), snapshotDto.getCreatedAt(), snapshotDto.getId());
    }

    @CheckForNull
    private Period findByVersion(int index, String version) {
      SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectId).setVersion(version).setSort(BY_DATE, DESC));
      if (snapshot == null) {
        return null;
      }
      LOG.debug("Compare to version ({}) ({})", version, formatDate(snapshot.getCreatedAt()));
      return new Period(index, CoreProperties.TIMEMACHINE_MODE_VERSION, version, snapshot.getCreatedAt(), snapshot.getId());
    }

    @CheckForNull
    private SnapshotDto findFirstSnapshot(DbSession session, SnapshotQuery query) {
      List<SnapshotDto> snapshots = dbClient.snapshotDao().selectSnapshotsByQuery(session, query);
      if (!snapshots.isEmpty()) {
        return snapshots.get(0);
      }
      return null;
    }
  }

  @CheckForNull
  private static Integer tryToResolveByDays(String property) {
    try {
      return Integer.parseInt(property);
    } catch (NumberFormatException e) {
      // Nothing to, it means that the property is not a number of days
      return null;
    }
  }

  @CheckForNull
  private static Date tryToResolveByDate(String property) {
    try {
      return DateUtils.parseDate(property);
    } catch (Exception e) {
      // Nothing to, it means that the property is not a date
      return null;
    }
  }

  @CheckForNull
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

  private static SnapshotQuery createCommonQuery(Long projectId) {
    return new SnapshotQuery().setComponentId(projectId).setStatus(SnapshotDto.STATUS_PROCESSED);
  }

  private static String formatDate(long date) {
    return DateUtils.formatDate(org.apache.commons.lang.time.DateUtils.truncate(new Date(date), Calendar.SECOND));
  }

  private static String getPropertyValue(@Nullable String qualifier, Settings settings, int index) {
    String value = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index);
    // For periods 4 and 5 we're also searching for a property prefixed by the qualifier
    if (index > 3 && Strings.isNullOrEmpty(value)) {
      value = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + "." + qualifier);
    }
    return value;
  }

  @Override
  public String getDescription() {
    return "Feed differential periods";
  }
}
