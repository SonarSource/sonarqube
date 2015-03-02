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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.purge.PurgeableSnapshotDto;

import java.util.Date;
import java.util.List;

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
  public List<PurgeableSnapshotDto> filter(List<PurgeableSnapshotDto> history) {
    List<Interval> intervals = Interval.group(history, start, end, dateField);
    List<PurgeableSnapshotDto> result = Lists.newArrayList();
    for (Interval interval : intervals) {
      appendSnapshotsToDelete(interval, result);
    }

    return result;
  }

  @Override
  public void log() {
    Loggers.get(getClass()).debug("-> Keep one snapshot per {} between {} and {}", label, DateUtils.formatDate(start), DateUtils.formatDate(end));
  }

  private void appendSnapshotsToDelete(Interval interval, List<PurgeableSnapshotDto> toDelete) {
    if (interval.count() > 1) {
      List<PurgeableSnapshotDto> deletables = Lists.newArrayList();
      List<PurgeableSnapshotDto> toKeep = Lists.newArrayList();
      for (PurgeableSnapshotDto snapshot : interval.get()) {
        if (isDeletable(snapshot)) {
          deletables.add(snapshot);
        } else {
          toKeep.add(snapshot);
        }
      }

      if (!toKeep.isEmpty()) {
        toDelete.addAll(deletables);

      } else if (deletables.size() > 1) {
        // keep one snapshot
        toDelete.addAll(deletables.subList(1, deletables.size()));
      }
    }
  }

  @VisibleForTesting
  static boolean isDeletable(PurgeableSnapshotDto snapshot) {
    return !snapshot.isLast() && !snapshot.hasEvents();
  }

}
