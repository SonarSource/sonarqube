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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.purge.PurgeableAnalysisDto;

class KeepOneFilter implements Filter {

  private final Date start;
  private final Date end;
  private final int dateField;
  private final String label;

  KeepOneFilter(Date start, Date end, int calendarField, String label) {
    this.start = start;
    this.end = end;
    this.dateField = calendarField;
    this.label = label;
  }

  @Override
  public List<PurgeableAnalysisDto> filter(List<PurgeableAnalysisDto> history) {
    List<Interval> intervals = Interval.group(history, start, end, dateField);
    List<PurgeableAnalysisDto> result = Lists.newArrayList();
    for (Interval interval : intervals) {
      appendSnapshotsToDelete(interval, result);
    }

    return result;
  }

  @Override
  public void log() {
    Loggers.get(getClass()).debug("-> Keep one snapshot per {} between {} and {}", label, DateUtils.formatDate(start), DateUtils.formatDate(end));
  }

  private static void appendSnapshotsToDelete(Interval interval, List<PurgeableAnalysisDto> toDelete) {
    if (interval.count() > 1) {
      List<PurgeableAnalysisDto> deletables = Lists.newArrayList();
      List<PurgeableAnalysisDto> toKeep = Lists.newArrayList();
      for (PurgeableAnalysisDto snapshot : interval.get()) {
        if (isDeletable(snapshot)) {
          deletables.add(snapshot);
        } else {
          toKeep.add(snapshot);
        }
      }

      if (!toKeep.isEmpty()) {
        toDelete.addAll(deletables);

      } else if (deletables.size() > 1) {
        // keep last snapshot
        toDelete.addAll(deletables.subList(0, deletables.size() - 1));
      }
    }
  }

  @VisibleForTesting
  static boolean isDeletable(PurgeableAnalysisDto snapshot) {
    return !snapshot.isLast() && !snapshot.hasEvents();
  }

}
