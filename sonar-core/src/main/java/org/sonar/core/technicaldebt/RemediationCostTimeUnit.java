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
package org.sonar.core.technicaldebt;

import javax.annotation.CheckForNull;

import java.util.concurrent.TimeUnit;

public class RemediationCostTimeUnit {

  private TimeUnitValue days;
  private TimeUnitValue hours;
  private TimeUnitValue minutes;

  private static final int ONE_HOUR_IN_MINUTE = 60;

  private RemediationCostTimeUnit(long input, int hoursInDay) {
    int oneWorkingDay = hoursInDay * ONE_HOUR_IN_MINUTE;
    if (input >= oneWorkingDay) {
      long nbDays = input / oneWorkingDay;
      this.days = new TimeUnitValue(nbDays, TimeUnit.DAYS);
      input = input - (nbDays * oneWorkingDay);
    } else {
      this.days = new TimeUnitValue(0L, TimeUnit.DAYS);
    }

    if (input >= ONE_HOUR_IN_MINUTE) {
      long nbHours = input / ONE_HOUR_IN_MINUTE;
      this.hours = new TimeUnitValue(nbHours, TimeUnit.HOURS);
      input = input - (nbHours * ONE_HOUR_IN_MINUTE);
    } else {
      this.hours = new TimeUnitValue(0L, TimeUnit.HOURS);
    }

    this.minutes = new TimeUnitValue(input, TimeUnit.MINUTES);
  }

  public static RemediationCostTimeUnit of(Long time, int hoursInDay) {
    return new RemediationCostTimeUnit(time, hoursInDay);
  }

  @CheckForNull
  public TimeUnitValue days() {
    return days;
  }

  @CheckForNull
  public TimeUnitValue hours() {
    return hours;
  }

  @CheckForNull
  public TimeUnitValue minutes() {
    return minutes;
  }

  public static class TimeUnitValue {

    private long value;
    private TimeUnit unit;

    private TimeUnitValue(long value, TimeUnit unit) {
      this.value = value;
      this.unit = unit;
    }

    public long value() {
      return value;
    }

    public TimeUnit unit() {
      return unit;
    }
  }
}
