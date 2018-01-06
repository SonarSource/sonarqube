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

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.purge.DbCleanerTestUtils;
import org.sonar.db.purge.PurgeableAnalysisDto;

import static org.assertj.core.api.Assertions.assertThat;

public class IntervalTest {
  static int calendarField(Interval interval, int field) {
    if (interval.count() == 0) {
      return -1;
    }

    PurgeableAnalysisDto first = interval.get().iterator().next();
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(first.getDate());
    return cal.get(field);
  }

  @Test
  public void shouldGroupByIntervals() {
    List<PurgeableAnalysisDto> snapshots = Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDate("u1", "2011-04-03"),

      DbCleanerTestUtils.createAnalysisWithDate("u2", "2011-05-01"),
      DbCleanerTestUtils.createAnalysisWithDate("u3", "2011-05-19"),

      DbCleanerTestUtils.createAnalysisWithDate("u4", "2011-06-02"),
      DbCleanerTestUtils.createAnalysisWithDate("u5", "2011-06-20"),

      DbCleanerTestUtils.createAnalysisWithDate("u6", "2012-06-29") // out of scope
      );

    List<Interval> intervals = Interval.group(snapshots, DateUtils.parseDate("2010-01-01"), DateUtils.parseDate("2011-12-31"), Calendar.MONTH);
    assertThat(intervals.size()).isEqualTo(3);

    assertThat(intervals.get(0).count()).isEqualTo(1);
    assertThat(calendarField(intervals.get(0), Calendar.MONTH)).isEqualTo((Calendar.APRIL));

    assertThat(intervals.get(1).count()).isEqualTo(2);
    assertThat(calendarField(intervals.get(1), Calendar.MONTH)).isEqualTo((Calendar.MAY));

    assertThat(intervals.get(2).count()).isEqualTo(2);
    assertThat(calendarField(intervals.get(2), Calendar.MONTH)).isEqualTo((Calendar.JUNE));
  }

  @Test
  public void shouldNotJoinMonthsOfDifferentYears() {
    List<PurgeableAnalysisDto> snapshots = Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDate("u1", "2010-04-03"),
      DbCleanerTestUtils.createAnalysisWithDate("u2", "2011-04-13")
      );

    List<Interval> intervals = Interval.group(snapshots,
      DateUtils.parseDateTime("2010-01-01T00:00:00+0100"), DateUtils.parseDateTime("2011-12-31T00:00:00+0100"), Calendar.MONTH);
    assertThat(intervals.size()).isEqualTo(2);

    assertThat(intervals.get(0).count()).isEqualTo(1);
    assertThat(calendarField(intervals.get(0), Calendar.MONTH)).isEqualTo((Calendar.APRIL));
    assertThat(calendarField(intervals.get(0), Calendar.YEAR)).isEqualTo((2010));

    assertThat(intervals.get(1).count()).isEqualTo(1);
    assertThat(calendarField(intervals.get(1), Calendar.MONTH)).isEqualTo((Calendar.APRIL));
    assertThat(calendarField(intervals.get(1), Calendar.YEAR)).isEqualTo((2011));
  }

  @Test
  public void shouldIgnoreTimeWhenGroupingByIntervals() {
    List<PurgeableAnalysisDto> snapshots = Arrays.asList(
      DbCleanerTestUtils.createAnalysisWithDateTime("u1", "2011-05-25T00:16:48+0100"),
      DbCleanerTestUtils.createAnalysisWithDateTime("u2", "2012-01-26T00:16:48+0100"),
      DbCleanerTestUtils.createAnalysisWithDateTime("u3", "2012-01-27T00:16:48+0100")
      );

    List<Interval> intervals = Interval.group(snapshots,
      DateUtils.parseDateTime("2011-05-25T00:00:00+0100"),
      DateUtils.parseDateTime("2012-01-26T00:00:00+0100"), Calendar.MONTH);
    assertThat(intervals.size()).isEqualTo(1);
    assertThat(intervals.get(0).count()).isEqualTo(1);
    assertThat(intervals.get(0).get().get(0).getAnalysisUuid()).isEqualTo(("u2"));
  }
}
