/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner.period;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.plugins.dbcleaner.api.PeriodCleaner;
import org.sonar.plugins.dbcleaner.api.PurgeUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public final class DefaultPeriodCleaner implements PeriodCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultPeriodCleaner.class);
  private final SQLRequests sql;
  private DatabaseSession session;

  public DefaultPeriodCleaner(DatabaseSession session) {
    this.session = session;
    this.sql = new SQLRequests(session);
  }

  public void purge(Project project, int projectSnapshotId) {
    Periods periods = new Periods(project);
    periods.log();
    purge(project, projectSnapshotId, periods);
  }

  void purge(Project project, int projectSnapshotId, Periods periods) {
    List<SnapshotFilter> filters = newFilters(periods);
    List<Snapshot> snapshotHistory = selectProjectSnapshots(project, projectSnapshotId);
    applyFilters(snapshotHistory, filters);
    deleteSnapshotsAndAllRelatedData(snapshotHistory);
  }

  private List<Snapshot> selectProjectSnapshots(Project project, int snapshotId) {
    List<Snapshot> snapshotHistory = Lists.newLinkedList(sql.getProjectSnapshotsOrderedByCreatedAt(snapshotId));
    LOG.debug("The project '" + project.getName() + "' has " + snapshotHistory.size() + " snapshots.");
    return snapshotHistory;
  }

  private void deleteSnapshotsAndAllRelatedData(List<Snapshot> snapshotHistory) {
    if (snapshotHistory.isEmpty()) {
      LOG.info("There are no snapshots to purge");
      return;
    }

    List<Integer> ids = Lists.newArrayList();
    for (Snapshot snapshot : snapshotHistory) {
      ids.addAll(sql.getChildIds(snapshot));
    }
    LOG.info("There are " + snapshotHistory.size() + " snapshots and " + (ids.size() - snapshotHistory.size())
        + " children snapshots which are obsolete and are going to be deleted.");
    if (LOG.isDebugEnabled()) {
      DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
      for (Snapshot snapshot : snapshotHistory) {
        LOG.debug("Delete snapshot created at " + format.format(snapshot.getCreatedAt()));
      }
    }
    PurgeUtils.deleteSnapshotsData(session, ids);
  }

  private void applyFilters(List<Snapshot> snapshotHistory, List<SnapshotFilter> filters) {
    for (SnapshotFilter filter : filters) {
      filter.filter(snapshotHistory);
    }
  }

  private List<SnapshotFilter> newFilters(Periods periods) {
    List<SnapshotFilter> filters = Lists.newArrayList();
    filters.add(new KeepLibrarySnapshotFilter());
    filters.add(new KeepSnapshotsBetweenTwoDatesFilter(new Date(), periods.dateToStartKeepingOneSnapshotByWeek));
    filters.add(new KeepOneSnapshotByPeriodBetweenTwoDatesFilter(GregorianCalendar.WEEK_OF_YEAR,
        periods.dateToStartKeepingOneSnapshotByWeek,
        periods.dateToStartKeepingOneSnapshotByMonth));
    filters.add(new KeepOneSnapshotByPeriodBetweenTwoDatesFilter(GregorianCalendar.MONTH,
        periods.dateToStartKeepingOneSnapshotByMonth,
        periods.dateToStartDeletingAllSnapshots));
    filters.add(new KeepLastSnapshotFilter());
    return filters;
  }
}
