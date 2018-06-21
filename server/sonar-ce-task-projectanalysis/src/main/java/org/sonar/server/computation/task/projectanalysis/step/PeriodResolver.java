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
package org.sonar.server.computation.task.projectanalysis.step;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.server.computation.task.projectanalysis.period.Period;

import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.DESC;

public class PeriodResolver {
  private static final Logger LOG = Loggers.get(PeriodResolver.class);

  private final DbClient dbClient;
  private final DbSession session;
  private final String projectUuid;
  private final long analysisDate;
  @CheckForNull
  private final String currentVersion;

  public PeriodResolver(DbClient dbClient, DbSession session, String projectUuid, long analysisDate, @Nullable String currentVersion) {
    this.dbClient = dbClient;
    this.session = session;
    this.projectUuid = projectUuid;
    this.analysisDate = analysisDate;
    this.currentVersion = currentVersion;
  }

  @CheckForNull
  public Period resolve(Configuration config) {
    String propertyValue = getPropertyValue(config);
    if (StringUtils.isBlank(propertyValue)) {
      return null;
    }
    Period period = resolve(propertyValue);
    if (period == null && StringUtils.isNotBlank(propertyValue)) {
      LOG.debug("Property " + LEAK_PERIOD + " is not valid: " + propertyValue);
    }
    return period;
  }

  @CheckForNull
  private Period resolve(String property) {
    Integer days = tryToResolveByDays(property);
    if (days != null) {
      return findByDays(days);
    }
    Date date = DateUtils.parseDateQuietly(property);
    if (date != null) {
      return findByDate(date);
    }
    if (StringUtils.equals(LEAK_PERIOD_MODE_PREVIOUS_VERSION, property)) {
      return findByPreviousVersion();
    }
    return findByVersion(property);
  }

  private Period findByDate(Date date) {
    SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectUuid).setCreatedAfter(date.getTime()).setSort(BY_DATE, ASC));
    if (snapshot == null) {
      return null;
    }
    LOG.debug("Compare to date {} (analysis of {})", formatDate(date.getTime()), formatDate(snapshot.getCreatedAt()));
    return new Period(LEAK_PERIOD_MODE_DATE, DateUtils.formatDate(date), snapshot.getCreatedAt(), snapshot.getUuid());
  }

  @CheckForNull
  private Period findByDays(int days) {
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectAnalysesByQuery(session, createCommonQuery(projectUuid).setCreatedBefore(analysisDate).setSort(BY_DATE, ASC));
    long targetDate = DateUtils.addDays(new Date(analysisDate), -days).getTime();
    SnapshotDto snapshot = findNearestSnapshotToTargetDate(snapshots, targetDate);
    if (snapshot == null) {
      return null;
    }
    LOG.debug("Compare over {} days ({}, analysis of {})", String.valueOf(days), formatDate(targetDate), formatDate(snapshot.getCreatedAt()));
    return new Period(LEAK_PERIOD_MODE_DAYS, String.valueOf(days), snapshot.getCreatedAt(), snapshot.getUuid());
  }

  @CheckForNull
  private Period findByPreviousVersion() {
    if (currentVersion == null) {
      return null;
    }
    List<SnapshotDto> snapshotDtos = dbClient.snapshotDao().selectPreviousVersionSnapshots(session, projectUuid, currentVersion);
    if (snapshotDtos.isEmpty()) {
      // If no previous version is found, the first analysis is returned
      return findByFirstAnalysis();
    }
    SnapshotDto snapshotDto = snapshotDtos.get(0);
    LOG.debug("Compare to previous version ({})", formatDate(snapshotDto.getCreatedAt()));
    return new Period(LEAK_PERIOD_MODE_PREVIOUS_VERSION, snapshotDto.getVersion(), snapshotDto.getCreatedAt(), snapshotDto.getUuid());
  }

  @CheckForNull
  private Period findByFirstAnalysis() {
    SnapshotDto snapshotDto = dbClient.snapshotDao().selectOldestSnapshot(session, projectUuid);
    if (snapshotDto == null) {
      return null;
    }
    LOG.debug("Compare to first analysis ({})", formatDate(snapshotDto.getCreatedAt()));
    return new Period(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, snapshotDto.getCreatedAt(), snapshotDto.getUuid());
  }

  @CheckForNull
  private Period findByVersion(String version) {
    SnapshotDto snapshot = findFirstSnapshot(session, createCommonQuery(projectUuid).setVersion(version).setSort(BY_DATE, DESC));
    if (snapshot == null) {
      return null;
    }
    LOG.debug("Compare to version ({}) ({})", version, formatDate(snapshot.getCreatedAt()));
    return new Period(LEAK_PERIOD_MODE_VERSION, version, snapshot.getCreatedAt(), snapshot.getUuid());
  }

  @CheckForNull
  private SnapshotDto findFirstSnapshot(DbSession session, SnapshotQuery query) {
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectAnalysesByQuery(session, query);
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

  private static SnapshotQuery createCommonQuery(String projectUuid) {
    return new SnapshotQuery().setComponentUuid(projectUuid).setStatus(STATUS_PROCESSED);
  }

  private static String formatDate(long date) {
    return DateUtils.formatDate(Date.from(new Date(date).toInstant().truncatedTo(ChronoUnit.SECONDS)));
  }

  private static String getPropertyValue(Configuration config) {
    return config.get(LEAK_PERIOD).get();
  }
}
