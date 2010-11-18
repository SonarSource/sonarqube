/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;
import org.sonar.plugins.dbcleaner.util.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.util.PurgeUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public final class PeriodCleaner extends Purge {

  private static final Logger LOG = LoggerFactory.getLogger(PeriodCleaner.class);
  private final SQLRequests sql;
  private final Project project;

  Date dateToStartKeepingOneSnapshotByWeek;
  Date dateToStartKeepingOneSnapshotByMonth;
  Date dateToStartDeletingAllSnapshots;

  public PeriodCleaner(DatabaseSession session, Project project) {
    super(session);
    this.sql = new SQLRequests(session);
    this.project = project;
    initMilestones();
  }

  public void purge(PurgeContext context) {
    purge(context.getSnapshotId());
  }

  public void purge(int snapshotId) {
    List<SnapshotFilter> filters = initDbCleanerFilters();
    List<Snapshot> snapshotHistory = getAllProjectSnapshots(snapshotId);
    applyFilters(snapshotHistory, filters);
    deleteSnapshotsAndAllRelatedData(snapshotHistory);
  }

  private List<Snapshot> getAllProjectSnapshots(int snapshotId) {
    List<Snapshot> snapshotHistory = Lists.newLinkedList(sql.getProjectSnapshotsOrderedByCreatedAt(snapshotId));
    LOG.info("The project '" + project.getName() + "' has " + snapshotHistory.size() + " snapshots.");
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
    PurgeUtils.deleteSnapshotsData(getSession(), ids);
  }

  private void applyFilters(List<Snapshot> snapshotHistory, List<SnapshotFilter> filters) {
    for (SnapshotFilter filter : filters) {
      filter.filter(snapshotHistory);
    }
  }

  private void initMilestones() {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateToStartKeepingOneSnapshotByWeek = getDate(project.getConfiguration(),
        DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK, DbCleanerConstants.ONE_MONTH);
    LOG.debug("Keep only one snapshot by week after : " + dateFormat.format(dateToStartKeepingOneSnapshotByWeek));
    dateToStartKeepingOneSnapshotByMonth = getDate(project.getConfiguration(),
        DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH, DbCleanerConstants.ONE_YEAR);
    LOG.debug("Keep only one snapshot by month after : " + dateFormat.format(dateToStartKeepingOneSnapshotByMonth));
    dateToStartDeletingAllSnapshots = getDate(project.getConfiguration(), DbCleanerConstants.MONTHS_BEFORE_DELETING_ALL_SNAPSHOTS,
        DbCleanerConstants.THREE_YEARS);
    LOG.debug("Delete all snapshots after : " + dateFormat.format(dateToStartDeletingAllSnapshots));
  }

  private List<SnapshotFilter> initDbCleanerFilters() {
    List<SnapshotFilter> filters = Lists.newArrayList();
    filters.add(new KeepLibrarySnapshotFilter());
    filters.add(new KeepSnapshotsBetweenTwoDatesFilter(new Date(), dateToStartKeepingOneSnapshotByWeek));
    filters.add(new KeepOneSnapshotByPeriodBetweenTwoDatesFilter(GregorianCalendar.WEEK_OF_YEAR, dateToStartKeepingOneSnapshotByWeek,
        dateToStartKeepingOneSnapshotByMonth));
    filters.add(new KeepOneSnapshotByPeriodBetweenTwoDatesFilter(GregorianCalendar.MONTH, dateToStartKeepingOneSnapshotByMonth,
        dateToStartDeletingAllSnapshots));
    filters.add(new KeepLastSnapshotFilter());
    return filters;
  }

  protected Date getDate(Configuration conf, String propertyKey, String defaultNumberOfMonths) {
    int months = conf.getInt(propertyKey, Integer.parseInt(defaultNumberOfMonths));
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.add(GregorianCalendar.MONTH, -months);
    return calendar.getTime();
  }
}
