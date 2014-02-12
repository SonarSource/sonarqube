/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.api.utils;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class WorkDurationTest {

  private static final int HOURS_IN_DAY = 8;

  @Test
  public void create_from_days_hours_minutes() throws Exception {
    WorkDuration workDuration = WorkDuration.create(1, 1, 1, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(1);
    assertThat(workDuration.minutes()).isEqualTo(1);
    assertThat(workDuration.toSeconds()).isEqualTo(1 * HOURS_IN_DAY * 60 * 60 + 1 * 60 * 60 + 60);
    assertThat(workDuration.hoursInDay()).isEqualTo(HOURS_IN_DAY);
  }

  @Test
  public void create_from_value_and_unit() throws Exception {
    WorkDuration result = WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, HOURS_IN_DAY);
    assertThat(result.days()).isEqualTo(1);
    assertThat(result.hours()).isEqualTo(0);
    assertThat(result.minutes()).isEqualTo(0);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);
    assertThat(result.toSeconds()).isEqualTo(1 * HOURS_IN_DAY * 60 * 60);

    assertThat(WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toSeconds()).isEqualTo(1 * HOURS_IN_DAY * 60 * 60);
    assertThat(WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toSeconds()).isEqualTo(1 * 60 * 60);
    assertThat(WorkDuration.createFromValueAndUnit(1, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toSeconds()).isEqualTo(60);
  }

  @Test
  public void create_from_seconds() throws Exception {
    WorkDuration workDuration = WorkDuration.createFromSeconds(60, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(1);

    workDuration = WorkDuration.createFromSeconds(60 * 60, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(1);
    assertThat(workDuration.minutes()).isEqualTo(0);

    workDuration = WorkDuration.createFromSeconds(HOURS_IN_DAY * 60 * 60, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(0);
  }

  @Test
  public void create_from_working_long() throws Exception {
    // 1 minute
    WorkDuration workDuration = WorkDuration.createFromLong(1l, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(1);

    // 1 hour
    workDuration = WorkDuration.createFromLong(100l, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(1);
    assertThat(workDuration.minutes()).isEqualTo(0);

    // 1 day
    workDuration = WorkDuration.createFromLong(10000l, HOURS_IN_DAY);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(0);
  }

  @Test
  public void convert_to_seconds() throws Exception {
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toSeconds()).isEqualTo(2 * 60);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toSeconds()).isEqualTo(2 * 60 * 60);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toSeconds()).isEqualTo(2 * HOURS_IN_DAY * 60 * 60);
  }

  @Test
  public void convert_to_working_days() throws Exception {
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toWorkingDays()).isEqualTo(2d / 60d / 8d);
    assertThat(WorkDuration.createFromValueAndUnit(240, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toWorkingDays()).isEqualTo(0.5);
    assertThat(WorkDuration.createFromValueAndUnit(4, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(0.5);
    assertThat(WorkDuration.createFromValueAndUnit(8, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(1d);
    assertThat(WorkDuration.createFromValueAndUnit(16, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(2d);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toWorkingDays()).isEqualTo(2d);
  }

  @Test
  public void convert_to_working_long() throws Exception {
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY).toLong()).isEqualTo(2l);
    assertThat(WorkDuration.createFromValueAndUnit(4, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toLong()).isEqualTo(400l);
    assertThat(WorkDuration.createFromValueAndUnit(10, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toLong()).isEqualTo(10200l);
    assertThat(WorkDuration.createFromValueAndUnit(8, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).toLong()).isEqualTo(10000l);
    assertThat(WorkDuration.createFromValueAndUnit(2, WorkDuration.UNIT.DAYS, HOURS_IN_DAY).toLong()).isEqualTo(20000l);
  }

  @Test
  public void add() throws Exception {
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
  }

  @Test
  public void subtract() throws Exception {
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
  }

  @Test
  public void multiply() throws Exception {
    // 5h * 2 = 1d 2h
    WorkDuration result = WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.HOURS, HOURS_IN_DAY).multiply(2);
    assertThat(result.days()).isEqualTo(1);
    assertThat(result.hours()).isEqualTo(2);
    assertThat(result.minutes()).isEqualTo(0);
    assertThat(result.hoursInDay()).isEqualTo(HOURS_IN_DAY);
  }
}
