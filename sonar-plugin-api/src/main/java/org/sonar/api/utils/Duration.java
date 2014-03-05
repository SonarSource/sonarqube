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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 4.3
 */
public class Duration implements Serializable {

  public static final String DAY = "d";
  public static final String HOUR = "h";
  public static final String MINUTE = "min";

  private static final short HOURS_IN_ONE_DAY = 24;
  private static final short MINUTES_IN_ONE_HOUR = 60;

  private final long durationInMinutes;

  private int days;
  private int hours;
  private int minutes;

  private Duration(long durationInMinutes) {
    this.durationInMinutes = durationInMinutes;
    this.days = ((Double) ((double) durationInMinutes / HOURS_IN_ONE_DAY / MINUTES_IN_ONE_HOUR)).intValue();
    Long currentDurationInMinutes = durationInMinutes - (days * HOURS_IN_ONE_DAY * MINUTES_IN_ONE_HOUR);
    this.hours = ((Double) (currentDurationInMinutes.doubleValue() / MINUTES_IN_ONE_HOUR)).intValue();
    currentDurationInMinutes = currentDurationInMinutes - (hours * MINUTES_IN_ONE_HOUR);
    this.minutes = currentDurationInMinutes.intValue();
  }

  private Duration(int days, int hours, int minutes) {
    this(calculateDurationInMinutes(days, hours, minutes, HOURS_IN_ONE_DAY));
  }

  public static Duration ofDays(int days) {
    return new Duration(days, 0, 0);
  }

  public static Duration ofHours(int hours) {
    return new Duration(0, hours, 0);
  }

  public static Duration ofMinutes(int minutes) {
    return new Duration(0, 0, minutes);
  }

  public static Duration create(long durationInMinutes) {
    return new Duration(durationInMinutes);
  }

  public static Duration decode(String text) {
    return new Duration(extractValue(text, DAY), extractValue(text, HOUR), extractValue(text, MINUTE));
  }

  private static int extractValue(String text, String unit) {
    try {
      Pattern pattern = Pattern.compile("(\\d*?)\\D*" + unit);
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        String daysString = matcher.group(1);
        return Integer.parseInt(daysString);
      }
      return 0;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format("'%s' is invalid, it should use the following sample format : 2d 10h 15min", text), e);
    }
  }

  public String encode() {
    return toString();
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

  public long toMinutes() {
    return durationInMinutes;
  }

  public long toMinutes(int hoursInDay) {
    return calculateDurationInMinutes(days, hours, minutes, hoursInDay);
  }

  private static long calculateDurationInMinutes(int days, int hours, int minutes, int hoursInDay){
    return ((long) days * hoursInDay * MINUTES_IN_ONE_HOUR) + (hours * MINUTES_IN_ONE_HOUR) + minutes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Duration that = (Duration) o;
    if (durationInMinutes != that.durationInMinutes) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (int) (durationInMinutes ^ (durationInMinutes >>> 32));
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    if (days > 0) {
      stringBuilder.append(days);
      stringBuilder.append(DAY);
    }
    if (hours > 0) {
      stringBuilder.append(hours);
      stringBuilder.append(HOUR);
    }
    if (minutes > 0) {
      stringBuilder.append(minutes);
      stringBuilder.append(MINUTE);
    }
    return stringBuilder.toString();
  }
}
