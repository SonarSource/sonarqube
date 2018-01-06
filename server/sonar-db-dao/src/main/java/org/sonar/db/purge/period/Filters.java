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
package org.sonar.db.purge.period;

import com.google.common.collect.Lists;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Configuration;
import org.sonar.core.config.PurgeConstants;

class Filters {
  private final List<Filter> all = Lists.newArrayList();

  Filters(Configuration config) {
    Date dateToStartKeepingOneSnapshotByDay = getDateFromHours(config, PurgeConstants.HOURS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_DAY);
    Date dateToStartKeepingOneSnapshotByWeek = getDateFromWeeks(config, PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK);
    Date dateToStartKeepingOneSnapshotByMonth = getDateFromWeeks(config, PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH);
    Date dateToStartKeepingOnlyAnalysisWithVersion = getDateFromWeeks(config, PurgeConstants.WEEKS_BEFORE_KEEPING_ONLY_ANALYSES_WITH_VERSION);
    Date dateToStartDeletingAllSnapshots = getDateFromWeeks(config, PurgeConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS);

    all.add(new KeepOneFilter(dateToStartKeepingOneSnapshotByWeek, dateToStartKeepingOneSnapshotByDay, Calendar.DAY_OF_YEAR, "day"));
    all.add(new KeepOneFilter(dateToStartKeepingOneSnapshotByMonth, dateToStartKeepingOneSnapshotByWeek, Calendar.WEEK_OF_YEAR, "week"));
    all.add(new KeepOneFilter(dateToStartDeletingAllSnapshots, dateToStartKeepingOneSnapshotByMonth, Calendar.MONTH, "month"));
    all.add(new KeepWithVersionFilter(dateToStartKeepingOnlyAnalysisWithVersion));
    all.add(new DeleteAllFilter(dateToStartDeletingAllSnapshots));
  }

  static Date getDateFromWeeks(Configuration config, String propertyKey) {
    int weeks = config.getInt(propertyKey).get();
    return DateUtils.addWeeks(new Date(), -weeks);
  }

  static Date getDateFromHours(Configuration config, String propertyKey) {
    int hours = config.getInt(propertyKey).get();
    return DateUtils.addHours(new Date(), -hours);
  }

  List<Filter> all() {
    return all;
  }
}
