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

package org.sonar.core.computation.dbcleaner.period;

import com.google.common.collect.Lists;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Settings;
import org.sonar.core.computation.dbcleaner.DbCleanerConstants;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

class Filters {
  private final List<Filter> all = Lists.newArrayList();

  Filters(Settings settings) {
    Date dateToStartKeepingOneSnapshotByDay = getDateFromHours(settings, DbCleanerConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY);
    Date dateToStartKeepingOneSnapshotByWeek = getDateFromWeeks(settings, DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK);
    Date dateToStartKeepingOneSnapshotByMonth = getDateFromWeeks(settings, DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH);
    Date dateToStartDeletingAllSnapshots = getDateFromWeeks(settings, DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS);

    all.add(new KeepOneFilter(dateToStartKeepingOneSnapshotByWeek, dateToStartKeepingOneSnapshotByDay, Calendar.DAY_OF_YEAR, "day"));
    all.add(new KeepOneFilter(dateToStartKeepingOneSnapshotByMonth, dateToStartKeepingOneSnapshotByWeek, Calendar.WEEK_OF_YEAR, "week"));
    all.add(new KeepOneFilter(dateToStartDeletingAllSnapshots, dateToStartKeepingOneSnapshotByMonth, Calendar.MONTH, "month"));
    all.add(new DeleteAllFilter(dateToStartDeletingAllSnapshots));
  }

  static Date getDateFromWeeks(Settings settings, String propertyKey) {
    int weeks = settings.getInt(propertyKey);
    return DateUtils.addWeeks(new Date(), -weeks);
  }

  static Date getDateFromHours(Settings settings, String propertyKey) {
    int hours = settings.getInt(propertyKey);
    return DateUtils.addHours(new Date(), -hours);
  }

  List<Filter> all() {
    return all;
  }
}
