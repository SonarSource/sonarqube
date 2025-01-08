/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.DateUtils;
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
    List<PurgeableAnalysisDto> result = new ArrayList<>();
    for (Interval interval : intervals) {
      appendSnapshotsToDelete(interval, result);
    }

    return result;
  }

  @Override
  public void log() {
    LoggerFactory.getLogger(getClass()).atDebug()
      .addArgument(label)
      .addArgument(() -> DateUtils.formatDate(start))
      .addArgument(() -> DateUtils.formatDate(end))
      .log("-> Keep one snapshot per {} between {} and {}");
  }

  private static void appendSnapshotsToDelete(Interval interval, List<PurgeableAnalysisDto> toDelete) {
    if (interval.count() > 1) {
      List<PurgeableAnalysisDto> deletables = new ArrayList<>();
      List<PurgeableAnalysisDto> toKeep = new ArrayList<>();
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
