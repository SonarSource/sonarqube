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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * @since 4.0
 */
public final class WorkUnit implements Serializable {

  public static final String DAYS = "d";
  public static final String MINUTES = "mn";
  public static final String HOURS = "h";
  public static final String DEFAULT_UNIT = DAYS;

  public static final double DEFAULT_VALUE = 0.0;

  private static final String[] UNITS = {DAYS, MINUTES, HOURS};

  private static final int DAY = 10000;
  private static final int HOUR = 100;
  private static final int MINUTE = 1;

  private int days;
  private int hours;
  private int minutes;

  private WorkUnit(int days, int hours, int minutes) {
    this.minutes = minutes;
    this.hours = hours;
    this.days = days;
  }

  /**
   * @deprecated since 4.2.
   */
  @Deprecated
  public static WorkUnit create() {
    return create(0d, DEFAULT_UNIT);
  }

  public static WorkUnit create(@Nullable Double value, @Nullable String unit) {
    String defaultIfEmptyUnit = StringUtils.defaultIfEmpty(unit, DEFAULT_UNIT);
    if (!ArrayUtils.contains(UNITS, defaultIfEmptyUnit)) {
      throw new IllegalArgumentException("Unit can not be: " + defaultIfEmptyUnit + ". Possible values are " + ArrayUtils.toString(UNITS));
    }
    Double d = value != null ? value : DEFAULT_VALUE;
    if (d < 0.0) {
      throw new IllegalArgumentException("Value can not be negative: " + d);
    }

    int days = 0;
    int hours = 0;
    int minutes = 0;
    if (DAYS.equals(unit)) {
      days = d.intValue();
    } else if (HOURS.equals(unit)) {
      hours = d.intValue();
    } else if (MINUTES.equals(unit)) {
      minutes = d.intValue();
    }
    return new WorkUnit(days, hours, minutes);
  }

  public double getValue() {
    if (days > 0) {
      return days + (hours / 24) + (minutes / 60 / 24);
    } else if (hours > 0) {
      return hours + (minutes / 60);
    } else {
      return minutes;
    }
  }

  public String getUnit() {
    if (days > 0) {
      return DAYS;
    } else if (hours > 0) {
      return HOURS;
    } else {
      return MINUTES;
    }
  }

  /**
   * @since 4.2
   */
  public int days() {
    return days;
  }

  /**
   * @since 4.2
   */
  public int hours() {
    return hours;
  }

  /**
   * @since 4.2
   */
  public int minutes() {
    return minutes;
  }

  /**
   *
   * @since 4.2
   */
  public static WorkUnit fromLong(long durationInLong) {
    Builder builder = new Builder();

    long time = durationInLong;
    Long currentTime = time / DAY;
    if (currentTime > 0) {
      builder.setDays(currentTime.intValue());
      time = time - (currentTime * DAY);
    }

    currentTime = time / HOUR;
    if (currentTime > 0) {
      builder.setHours(currentTime.intValue());
      time = time - (currentTime * HOUR);
    }

    currentTime = time / MINUTE;
    if (currentTime > 0) {
      builder.setMinutes(currentTime.intValue());
    }

    return builder.build();
  }

  /**
   * Return the duration using the following format DDHHMM, where DD is the number of days, HH is the number of months, and MM the number of minutes.
   * For instance, 5 days and 2 hours will return 050200.
   *
   * @since 4.2
   */
  public long toLong() {
    return days * DAY + hours * HOUR + minutes * MINUTE;
  }

  /**
   * Return the duration in number of days.
   * For instance, 5 days and 4 hours will return 5.5 hours (if hoursIndDay is 8).
   *
   * @since 4.2
   */
  public double toDays(int hoursInDay) {
    double resultDays = days;
    resultDays += (double) hours / hoursInDay;
    resultDays += (double) minutes / (hoursInDay * 60.0);
    return resultDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkUnit workDayDuration = (WorkUnit) o;
    if (days != workDayDuration.days) {
      return false;
    }
    if (hours != workDayDuration.hours) {
      return false;
    }
    if (minutes != workDayDuration.minutes) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = Integer.valueOf(days).hashCode();
    result = 29 * result + Integer.valueOf(hours).hashCode();
    result = 27 * result + Integer.valueOf(minutes).hashCode();
    return result;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  /**
   * @since 4.2
   */
  public static class Builder {
    private int days;
    private int hours;
    private int minutes;

    public Builder setDays(int days) {
      this.days = days;
      return this;
    }

    public Builder setHours(int hours) {
      this.hours = hours;
      return this;
    }

    public Builder setMinutes(int minutes) {
      this.minutes = minutes;
      return this;
    }

    public WorkUnit build() {
      return new WorkUnit(days, hours, minutes);
    }
  }
}
