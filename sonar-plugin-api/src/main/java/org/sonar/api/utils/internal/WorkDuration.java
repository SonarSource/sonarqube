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
package org.sonar.api.utils.internal;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @since 4.2
 */
public class WorkDuration implements Serializable {

  static final int DAY_POSITION_IN_LONG = 10_000;
  static final int HOUR_POSITION_IN_LONG = 100;
  static final int MINUTE_POSITION_IN_LONG = 1;

  public enum UNIT {
    DAYS, HOURS, MINUTES
  }

  private int hoursInDay;

  private long durationInMinutes;
  private int days;
  private int hours;
  private int minutes;

  private WorkDuration(long durationInMinutes, int days, int hours, int minutes, int hoursInDay) {
    this.durationInMinutes = durationInMinutes;
    this.days = days;
    this.hours = hours;
    this.minutes = minutes;
    this.hoursInDay = hoursInDay;
  }

  public static WorkDuration create(int days, int hours, int minutes, int hoursInDay) {
    long durationInSeconds = 60L * days * hoursInDay;
    durationInSeconds += 60L * hours;
    durationInSeconds += minutes;
    return new WorkDuration(durationInSeconds, days, hours, minutes, hoursInDay);
  }

  public static WorkDuration createFromValueAndUnit(int value, UNIT unit, int hoursInDay) {
    switch (unit) {
      case DAYS:
        return create(value, 0, 0, hoursInDay);
      case HOURS:
        return create(0, value, 0, hoursInDay);
      case MINUTES:
        return create(0, 0, value, hoursInDay);
      default:
        throw new IllegalStateException("Cannot create work duration");
    }
  }

  static WorkDuration createFromLong(long duration, int hoursInDay) {
    int days = 0;
    int hours = 0;
    int minutes = 0;

    long time = duration;
    Long currentTime = time / WorkDuration.DAY_POSITION_IN_LONG;
    if (currentTime > 0) {
      days = currentTime.intValue();
      time = time - (currentTime * WorkDuration.DAY_POSITION_IN_LONG);
    }

    currentTime = time / WorkDuration.HOUR_POSITION_IN_LONG;
    if (currentTime > 0) {
      hours = currentTime.intValue();
      time = time - (currentTime * WorkDuration.HOUR_POSITION_IN_LONG);
    }

    currentTime = time / WorkDuration.MINUTE_POSITION_IN_LONG;
    if (currentTime > 0) {
      minutes = currentTime.intValue();
    }
    return WorkDuration.create(days, hours, minutes, hoursInDay);
  }

  static WorkDuration createFromMinutes(long duration, int hoursInDay) {
    int days = (int)(duration / (double)hoursInDay / 60.0);
    Long currentDurationInMinutes = duration - (60L * days * hoursInDay);
    int hours = (int)(currentDurationInMinutes / 60.0);
    currentDurationInMinutes = currentDurationInMinutes - (60L * hours);
    return new WorkDuration(duration, days, hours, currentDurationInMinutes.intValue(), hoursInDay);
  }

  /**
   * Return the duration in number of working days.
   * For instance, 3 days and 4 hours will return 3.5 days (if hoursIndDay is 8).
   */
  public double toWorkingDays() {
    return durationInMinutes / 60d / hoursInDay;
  }

  /**
   * Return the duration using the following format DDHHMM, where DD is the number of days, HH is the number of months, and MM the number of minutes.
   * For instance, 3 days and 4 hours will return 030400 (if hoursIndDay is 8).
   */
  public long toLong() {
    int workingDays = days;
    int workingHours = hours;
    if (hours >= hoursInDay) {
      int nbAdditionalDays = hours / hoursInDay;
      workingDays += nbAdditionalDays;
      workingHours = hours - (nbAdditionalDays * hoursInDay);
    }
    return 1L * workingDays * DAY_POSITION_IN_LONG + workingHours * HOUR_POSITION_IN_LONG + minutes * MINUTE_POSITION_IN_LONG;
  }

  public long toMinutes() {
    return durationInMinutes;
  }

  public WorkDuration add(@Nullable WorkDuration with) {
    if (with != null) {
      return WorkDuration.createFromMinutes(this.toMinutes() + with.toMinutes(), this.hoursInDay);
    } else {
      return this;
    }
  }

  public WorkDuration subtract(@Nullable WorkDuration with) {
    if (with != null) {
      return WorkDuration.createFromMinutes(this.toMinutes() - with.toMinutes(), this.hoursInDay);
    } else {
      return this;
    }
  }

  public WorkDuration multiply(int factor) {
    return WorkDuration.createFromMinutes(this.toMinutes() * factor, this.hoursInDay);
  }

  public int days() {
    return days;
  }

  public int hours() {
    return hours;
  }

  public int minutes() {
    return minutes;
  }

  int hoursInDay() {
    return hoursInDay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WorkDuration that = (WorkDuration) o;
    return durationInMinutes == that.durationInMinutes;

  }

  @Override
  public int hashCode() {
    return (int) (durationInMinutes ^ (durationInMinutes >>> 32));
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
