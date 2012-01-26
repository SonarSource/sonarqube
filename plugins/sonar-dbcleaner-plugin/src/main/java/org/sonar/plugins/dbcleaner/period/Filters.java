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

import com.google.common.collect.Lists;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Settings;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

class Filters {
  private final List<Filter> filters = Lists.newArrayList();

  Filters(Settings settings) {
    Date dateToStartKeepingOneSnapshotByWeek = getDate(settings, DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK);
    Date dateToStartKeepingOneSnapshotByMonth = getDate(settings, DbCleanerConstants.MONTHS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH);
    Date dateToStartDeletingAllSnapshots = getDate(settings, DbCleanerConstants.MONTHS_BEFORE_DELETING_ALL_SNAPSHOTS);

    filters.add(new KeepOneFilter(dateToStartKeepingOneSnapshotByWeek, new Date(), Calendar.DAY_OF_YEAR, "day"));
    filters.add(new KeepOneFilter(dateToStartKeepingOneSnapshotByMonth, dateToStartKeepingOneSnapshotByWeek, Calendar.WEEK_OF_YEAR, "week"));
    filters.add(new KeepOneFilter(dateToStartDeletingAllSnapshots, dateToStartKeepingOneSnapshotByMonth, Calendar.MONTH, "month"));
    filters.add(new DeleteAllFilter(dateToStartDeletingAllSnapshots));
  }

  List<Filter> getFilters() {
    return filters;
  }

  static Date getDate(Settings settings, String propertyKey) {
    int months = settings.getInt(propertyKey);
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.add(GregorianCalendar.MONTH, -months);
    return calendar.getTime();
  }
}
