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

package org.sonar.db.purge.period;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableSnapshotDto;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IntervalTest {
  static int calendarField(Interval interval, int field) {
    if (interval.count() == 0) {
      return -1;
    }

    PurgeableSnapshotDto first = interval.get().iterator().next();
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(first.getDate());
    return cal.get(field);
  }

  @Test
  public void shouldGroupByIntervals() {
    List<PurgeableSnapshotDto> snapshots = Arrays.asList(
      DbCleanerTestUtils.createSnapshotWithDate(1L, "2011-04-03"),

      DbCleanerTestUtils.createSnapshotWithDate(2L, "2011-05-01"),
      DbCleanerTestUtils.createSnapshotWithDate(3L, "2011-05-19"),

      DbCleanerTestUtils.createSnapshotWithDate(4L, "2011-06-02"),
      DbCleanerTestUtils.createSnapshotWithDate(5L, "2011-06-20"),

      DbCleanerTestUtils.createSnapshotWithDate(6L, "2012-06-29") // out of scope
      );

    List<Interval> intervals = Interval.group(snapshots, DateUtils.parseDate("2010-01-01"), DateUtils.parseDate("2011-12-31"), Calendar.MONTH);
    assertThat(intervals.size(), is(3));

    assertThat(intervals.get(0).count(), is(1));
    assertThat(calendarField(intervals.get(0), Calendar.MONTH), is(Calendar.APRIL));

    assertThat(intervals.get(1).count(), is(2));
    assertThat(calendarField(intervals.get(1), Calendar.MONTH), is(Calendar.MAY));

    assertThat(intervals.get(2).count(), is(2));
    assertThat(calendarField(intervals.get(2), Calendar.MONTH), is(Calendar.JUNE));
  }

  @Test
  public void shouldNotJoinMonthsOfDifferentYears() {
    List<PurgeableSnapshotDto> snapshots = Arrays.asList(
      DbCleanerTestUtils.createSnapshotWithDate(1L, "2010-04-03"),
      DbCleanerTestUtils.createSnapshotWithDate(2L, "2011-04-13")
      );

    List<Interval> intervals = Interval.group(snapshots,
      DateUtils.parseDateTime("2010-01-01T00:00:00+0100"), DateUtils.parseDateTime("2011-12-31T00:00:00+0100"), Calendar.MONTH);
    assertThat(intervals.size(), is(2));

    assertThat(intervals.get(0).count(), is(1));
    assertThat(calendarField(intervals.get(0), Calendar.MONTH), is(Calendar.APRIL));
    assertThat(calendarField(intervals.get(0), Calendar.YEAR), is(2010));

    assertThat(intervals.get(1).count(), is(1));
    assertThat(calendarField(intervals.get(1), Calendar.MONTH), is(Calendar.APRIL));
    assertThat(calendarField(intervals.get(1), Calendar.YEAR), is(2011));
  }

  @Test
  public void shouldIgnoreTimeWhenGroupingByIntervals() {
    List<PurgeableSnapshotDto> snapshots = Arrays.asList(
      DbCleanerTestUtils.createSnapshotWithDateTime(1L, "2011-05-25T00:16:48+0100"),
      DbCleanerTestUtils.createSnapshotWithDateTime(2L, "2012-01-26T00:16:48+0100"),
      DbCleanerTestUtils.createSnapshotWithDateTime(3L, "2012-01-27T00:16:48+0100")
      );

    List<Interval> intervals = Interval.group(snapshots,
      DateUtils.parseDateTime("2011-05-25T00:00:00+0100"),
      DateUtils.parseDateTime("2012-01-26T00:00:00+0100"), Calendar.MONTH);
    assertThat(intervals.size(), is(1));
    assertThat(intervals.get(0).count(), is(1));
    assertThat(intervals.get(0).get().get(0).getSnapshotId(), is(2L));
  }
}
