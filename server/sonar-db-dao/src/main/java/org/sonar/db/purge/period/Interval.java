/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.db.purge.PurgeableAnalysisDto;

final class Interval {
  List<PurgeableAnalysisDto> snapshots = Lists.newArrayList();

  void add(PurgeableAnalysisDto snapshot) {
    snapshots.add(snapshot);
  }

  List<PurgeableAnalysisDto> get() {
    return snapshots;
  }

  int count() {
    return snapshots.size();
  }

  static List<Interval> group(List<PurgeableAnalysisDto> snapshots, Date start, Date end, int calendarField) {
    List<Interval> intervals = Lists.newArrayList();

    GregorianCalendar calendar = new GregorianCalendar();
    int lastYear = -1;
    int lastFieldValue = -1;
    Interval currentInterval = null;

    for (PurgeableAnalysisDto snapshot : snapshots) {
      if (!DateUtils.isSameDay(start, snapshot.getDate()) && snapshot.getDate().after(start) &&
        (snapshot.getDate().before(end) || DateUtils.isSameDay(end, snapshot.getDate()))) {
        calendar.setTime(snapshot.getDate());
        int currentFieldValue = calendar.get(calendarField);
        int currentYear = calendar.get(Calendar.YEAR);
        if (lastYear != currentYear || lastFieldValue != currentFieldValue) {
          currentInterval = new Interval();
          intervals.add(currentInterval);
        }
        lastFieldValue = currentFieldValue;
        lastYear = currentYear;
        if (currentInterval != null) {
          currentInterval.add(snapshot);
        }
      }
    }
    return intervals;
  }
}
