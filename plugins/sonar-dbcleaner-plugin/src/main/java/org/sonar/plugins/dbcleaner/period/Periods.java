/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public final class Periods {

  Date dateToStartKeepingOneSnapshotByWeek;
  Date dateToStartKeepingOneSnapshotByMonth;
  Date dateToStartDeletingAllSnapshots;

  public Periods(Date dateToStartKeepingOneSnapshotByWeek, Date dateToStartKeepingOneSnapshotByMonth, Date dateToStartDeletingAllSnapshots) {
    this.dateToStartKeepingOneSnapshotByWeek = dateToStartKeepingOneSnapshotByWeek;
    this.dateToStartKeepingOneSnapshotByMonth = dateToStartKeepingOneSnapshotByMonth;
    this.dateToStartDeletingAllSnapshots = dateToStartDeletingAllSnapshots;
  }

  public Periods(Project project) {
    dateToStartKeepingOneSnapshotByWeek = getDate(project.getConfiguration(),
        DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK, DbCleanerConstants.ONE_MONTH);
    dateToStartKeepingOneSnapshotByMonth = getDate(project.getConfiguration(),
        DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH, DbCleanerConstants.ONE_YEAR);
    dateToStartDeletingAllSnapshots = getDate(project.getConfiguration(), DbCleanerConstants.MONTHS_BEFORE_DELETING_ALL_SNAPSHOTS,
        DbCleanerConstants.FIVE_YEARS);
  }

  void log() {
    Logger logger = LoggerFactory.getLogger(getClass());
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    logger.debug("Keep only one snapshot by week after : " + dateFormat.format(dateToStartKeepingOneSnapshotByWeek));
    logger.debug("Keep only one snapshot by month after : " + dateFormat.format(dateToStartKeepingOneSnapshotByMonth));
    logger.debug("Delete all snapshots after : " + dateFormat.format(dateToStartDeletingAllSnapshots));
  }

  static Date getDate(Configuration conf, String propertyKey, String defaultNumberOfMonths) {
    int months = conf.getInt(propertyKey, Integer.parseInt(defaultNumberOfMonths));
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.add(GregorianCalendar.MONTH, -months);
    return calendar.getTime();
  }
}
