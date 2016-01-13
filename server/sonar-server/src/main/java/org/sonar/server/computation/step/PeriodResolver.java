/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.base.Strings;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.server.computation.period.Period;

import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_PERIOD_PREFIX;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.DESC;

public class PeriodResolver {
  private static final Logger LOG = Loggers.get(PeriodResolver.class);

  private final DbClient dbClient;
  private final DbSession session;
  private final long projectId;
  private final long analysisDate;
  @CheckForNull
  private final String currentVersion;
  private final String qualifier;

  public PeriodResolver(DbClient dbClient, DbSession session, long projectId, long analysisDate, @Nullable String currentVersion, String qualifier) {
    this.dbClient = dbClient;
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
      LOG.debug("Property " + TIMEMACHINE_PERIOD_PREFIX + index + " is not valid: " + propertyValue);
    }
    return period;
  }

  @CheckForNull
  private Period resolve(int index, String property) {
    Integer days = tryToResolveByDays(property);
    if (days != null) {
      return findByDays(index, days);
    }
    Date date = DateUtils.parseDateQuietly(property);
    if (date != null) {
      return findByDate(index, date);
    }
    if (StringUtils.equals(TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, property)) {
      return findByPreviousAnalysis(index);
    }
    if (StringUtils.equals(TIMEMACHINE_MODE_PREVIOUS_VERSION, property)) {
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
    return new Period(index, TIMEMACHINE_MODE_DATE, DateUtils.formatDate(date), snapshot.getCreatedAt(), snapshot.getId());
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
    return new Period(index, TIMEMACHINE_MODE_DAYS, String.valueOf(days), snapshot.getCreatedAt(), snapshot.getId());
  }

  @CheckForNull
  private Period findByPreviousAnalysis(int index) {
    SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectId).setCreatedBefore(analysisDate).setIsLast(true).setSort(BY_DATE, DESC));
    if (snapshot == null) {
      return null;
    }
    LOG.debug("Compare to previous analysis ({})", formatDate(snapshot.getCreatedAt()));
    return new Period(index, TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, formatDate(snapshot.getCreatedAt()), snapshot.getCreatedAt(), snapshot.getId());
  }

  @CheckForNull
  private Period findByPreviousVersion(int index) {
    if (currentVersion == null) {
      return null;
    }
    List<SnapshotDto> snapshotDtos = dbClient.snapshotDao().selectPreviousVersionSnapshots(session, projectId, currentVersion);
    if (snapshotDtos.isEmpty()) {
      // If no previous version is found, the first analysis is returned
      return findByFirstAnalysis(index);
    }
    SnapshotDto snapshotDto = snapshotDtos.get(0);
    LOG.debug("Compare to previous version ({})", formatDate(snapshotDto.getCreatedAt()));
    return new Period(index, TIMEMACHINE_MODE_PREVIOUS_VERSION, snapshotDto.getVersion(), snapshotDto.getCreatedAt(), snapshotDto.getId());
  }

  @CheckForNull
  private Period findByFirstAnalysis(int index) {
    SnapshotDto snapshotDto = dbClient.snapshotDao().selectOldestSnapshot(session, projectId);
    if (snapshotDto == null) {
      return null;
    }
    LOG.debug("Compare to first analysis ({})", formatDate(snapshotDto.getCreatedAt()));
    return new Period(index, TIMEMACHINE_MODE_PREVIOUS_VERSION, null, snapshotDto.getCreatedAt(), snapshotDto.getId());
  }

  @CheckForNull
  private Period findByVersion(int index, String version) {
    SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectId).setVersion(version).setSort(BY_DATE, DESC));
    if (snapshot == null) {
      return null;
    }
    LOG.debug("Compare to version ({}) ({})", version, formatDate(snapshot.getCreatedAt()));
    return new Period(index, TIMEMACHINE_MODE_VERSION, version, snapshot.getCreatedAt(), snapshot.getId());
  }

  @CheckForNull
  private SnapshotDto findFirstSnapshot(DbSession session, SnapshotQuery query) {
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectSnapshotsByQuery(session, query);
    if (!snapshots.isEmpty()) {
      return snapshots.get(0);
    }
    return null;
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
    return new SnapshotQuery().setComponentId(projectId).setStatus(STATUS_PROCESSED);
  }

  private static String formatDate(long date) {
    return DateUtils.formatDate(org.apache.commons.lang.time.DateUtils.truncate(new Date(date), Calendar.SECOND));
  }

  private static String getPropertyValue(@Nullable String qualifier, Settings settings, int index) {
    String value = settings.getString(TIMEMACHINE_PERIOD_PREFIX + index);
    // For periods 4 and 5 we're also searching for a property prefixed by the qualifier
    if (index > 3 && Strings.isNullOrEmpty(value)) {
      value = settings.getString(TIMEMACHINE_PERIOD_PREFIX + index + "." + qualifier);
    }
    return value;
  }
}
