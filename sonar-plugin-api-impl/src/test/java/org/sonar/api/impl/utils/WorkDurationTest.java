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
package org.sonar.api.impl.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkDurationTest {

  static final int HOURS_IN_DAY = 8;

  static final Long ONE_MINUTE = 1L;
  static final Long ONE_HOUR_IN_MINUTES = ONE_MINUTE * 60;
  static final Long ONE_DAY_IN_MINUTES = ONE_HOUR_IN_MINUTES * HOURS_IN_DAY;

  @Test
  public void create_from_days_hours_minutes() {
    WorkDuration workDuration = WorkDuration.create(1, 1, 1, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(1);
    assertThat(workDuration.minutes()).isEqualTo(1);
    assertThat(workDuration.toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES + ONE_HOUR_IN_MINUTES + ONE_MINUTE);
    assertThat(workDuration.hoursInDay()).isEqualTo(HOURS_IN_DAY);
  }

  @Test
  public void create_from_value_and_unit() {
    WorkDuration result = WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, HOURS_IN_DAY);
    assertThat(result.days()).isEqualTo(1);
    assertThat(result.hours()).isEqualTo(0);
    assertThat(result.minutes()).isEqualTo(0);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);
    assertThat(result.toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES);

    assertThat(WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toMinutes()).isEqualTo(ONE_DAY_IN_MINUTES);
    assertThat(WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toMinutes()).isEqualTo(ONE_HOUR_IN_MINUTES);
    assertThat(WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toMinutes()).isEqualTo(ONE_MINUTE);
  }

  @Test
  public void create_from_minutes() {
    WorkDuration workDuration = WorkDuration.createFromMinutes(ONE_MINUTE, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(1);

    workDuration = WorkDuration.createFromMinutes(ONE_HOUR_IN_MINUTES, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(1);
    assertThat(workDuration.minutes()).isEqualTo(0);

    workDuration = WorkDuration.createFromMinutes(ONE_DAY_IN_MINUTES, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(0);
  }

  @Test
  public void create_from_working_long() {
    // 1 minute
    WorkDuration workDuration = WorkDuration.createFromLong(1L, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(1);

    // 1 hour
    workDuration = WorkDuration.createFromLong(100L, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(1);
    assertThat(workDuration.minutes()).isEqualTo(0);

    // 1 day
    workDuration = WorkDuration.createFromLong(10000L, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(0);
  }

  @Test
  public void convert_to_seconds() {
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toMinutes()).isEqualTo(2L * ONE_MINUTE);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toMinutes()).isEqualTo(2L * ONE_HOUR_IN_MINUTES);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toMinutes()).isEqualTo(2L * ONE_DAY_IN_MINUTES);
  }

  @Test
  public void convert_to_working_days() {
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toWorkingDays()).isEqualTo(2d / 60d / 8d);
    assertThat(WorkDuration.createFromValueAndUnit(240, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toWorkingDays()).isEqualTo(0.5);
    assertThat(WorkDuration.createFromValueAndUnit(4, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(0.5);
    assertThat(WorkDuration.createFromValueAndUnit(8, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(1d);
    assertThat(WorkDuration.createFromValueAndUnit(16, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(2d);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(2d);
  }

  @Test
  public void convert_to_working_long() {
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toLong()).isEqualTo(2l);
    assertThat(WorkDuration.createFromValueAndUnit(4, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toLong()).isEqualTo(400l);
    assertThat(WorkDuration.createFromValueAndUnit(10, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toLong()).isEqualTo(10200l);
    assertThat(WorkDuration.createFromValueAndUnit(8, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toLong()).isEqualTo(10000l);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toLong()).isEqualTo(20000l);
  }

  @Test
  public void add() {
    // 4h + 5h = 1d 1h
    WorkDuration result = WorkDuration.createFromValueAndUnit(4, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).add(WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.HOURS, HOURS_IN_DAY));
    assertThat(result.days()).isEqualTo(1);
    assertThat(result.hours()).isEqualTo(1);
    assertThat(result.minutes()).isEqualTo(0);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);

    // 40 m + 30m = 1h 10m
    result = WorkDuration.createFromValueAndUnit(40, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).add(WorkDuration.createFromValueAndUnit(30, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY));
    assertThat(result.days()).isEqualTo(0);
    assertThat(result.hours()).isEqualTo(1);
    assertThat(result.minutes()).isEqualTo(10);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);

    // 10 m + 20m = 30m
    assertThat(WorkDuration.createFromValueAndUnit(10, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).add(
      WorkDuration.createFromValueAndUnit(20, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY)
    ).minutes()).isEqualTo(30);

    assertThat(WorkDuration.createFromValueAndUnit(10, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).add(null).minutes()).isEqualTo(10);
  }

  @Test
  public void subtract() {
    // 1d 1h - 5h = 4h
    WorkDuration result = WorkDuration.create(1, 1, 0, HOURS_IN_DAY).subtract(WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.HOURS, HOURS_IN_DAY));
    assertThat(result.days()).isEqualTo(0);
    assertThat(result.hours()).isEqualTo(4);
    assertThat(result.minutes()).isEqualTo(0);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);

    // 1h 10m - 30m = 40m
    result = WorkDuration.create(0, 1, 10, HOURS_IN_DAY).subtract(WorkDuration.createFromValueAndUnit(30, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY));
    assertThat(result.days()).isEqualTo(0);
    assertThat(result.hours()).isEqualTo(0);
    assertThat(result.minutes()).isEqualTo(40);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);

    // 30m - 20m = 10m
    assertThat(WorkDuration.createFromValueAndUnit(30, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).subtract(WorkDuration.createFromValueAndUnit(20, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY))
      .minutes()).isEqualTo(10);

    assertThat(WorkDuration.createFromValueAndUnit(10, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).subtract(null).minutes()).isEqualTo(10);
  }

  @Test
  public void multiply() {
    // 5h * 2 = 1d 2h
    WorkDuration result = WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).multiply(2);
    assertThat(result.days()).isEqualTo(1);
    assertThat(result.hours()).isEqualTo(2);
    assertThat(result.minutes()).isEqualTo(0);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    WorkDuration duration = WorkDuration.createFromLong(28800, HOURS_IN_DAY);
    WorkDuration durationWithSameValue = WorkDuration.createFromLong(28800, HOURS_IN_DAY);
    WorkDuration durationWithDifferentValue = WorkDuration.createFromLong(14400, HOURS_IN_DAY);

    assertThat(duration).isEqualTo(duration);
    assertThat(durationWithSameValue).isEqualTo(duration);
    assertThat(durationWithDifferentValue).isNotEqualTo(duration);
    assertThat(duration).isNotEqualTo(null);

    assertThat(duration.hashCode()).isEqualTo(duration.hashCode());
    assertThat(durationWithSameValue.hashCode()).isEqualTo(duration.hashCode());
    assertThat(durationWithDifferentValue.hashCode()).isNotEqualTo(duration.hashCode());
  }

  @Test
  public void test_toString() throws Exception {
    assertThat(WorkDuration.createFromLong(28800, HOURS_IN_DAY).toString()).isNotNull();
  }
}
