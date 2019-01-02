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
package org.sonar.api.utils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @since 4.3
 */
public class Duration implements Serializable {

  public static final String DAY = "d";
  public static final String HOUR = "h";
  public static final String MINUTE = "min";

  private static final short MINUTES_IN_ONE_HOUR = 60;

  private final long durationInMinutes;

  private Duration(long durationInMinutes) {
    this.durationInMinutes = durationInMinutes;
  }

  private Duration(int days, int hours, int minutes, int hoursInDay) {
    this(((long) days * hoursInDay * MINUTES_IN_ONE_HOUR) + (hours * MINUTES_IN_ONE_HOUR) + minutes);
  }

  /**
   * Create a Duration from a number of minutes.
   */
  public static Duration create(long durationInMinutes) {
    return new Duration(durationInMinutes);
  }

  /**
   * Create a Duration from a text duration and the number of hours in a day.
   * <br>
   * For instance, Duration.decode("1d 1h", 8) will have a number of minutes of 540 (1*8*60 + 60).
   * */
  public static Duration decode(String text, int hoursInDay) {
    int days = 0;
    int hours = 0;
    int minutes = 0;
    String sanitizedText = StringUtils.deleteWhitespace(text);
    Pattern pattern = Pattern.compile("\\s*+(?:(\\d++)\\s*+" + DAY + ")?+\\s*+(?:(\\d++)\\s*+" + HOUR + ")?+\\s*+(?:(\\d++)\\s*+" + MINUTE + ")?+\\s*+");
    Matcher matcher = pattern.matcher(text);

    try {
      if (matcher.find()) {
        String daysDuration = matcher.group(1);
        if (daysDuration != null) {
          days = Integer.parseInt(daysDuration);
          sanitizedText = sanitizedText.replace(daysDuration + DAY, "");
        }
        String hoursText = matcher.group(2);
        if (hoursText != null) {
          hours = Integer.parseInt(hoursText);
          sanitizedText = sanitizedText.replace(hoursText + HOUR, "");
        }
        String minutesText = matcher.group(3);
        if (minutesText != null) {
          minutes = Integer.parseInt(minutesText);
          sanitizedText = sanitizedText.replace(minutesText + MINUTE, "");
        }
        if (sanitizedText.isEmpty()) {
          return new Duration(days, hours, minutes, hoursInDay);
        }
      }
      throw invalid(text, null);
    } catch (NumberFormatException e) {
      throw invalid(text, e);
    }
  }

  /**
   * Return the duration in text, by using the given hours in day parameter to process hours.
   * <br>
   * Duration.decode("1d 1h", 8).encode(8) will return "1d 1h"
   * Duration.decode("2d", 8).encode(16) will return "1d"
   */
  public String encode(int hoursInDay) {
    int days = ((Double) ((double) durationInMinutes / hoursInDay / MINUTES_IN_ONE_HOUR)).intValue();
    Long remainingDuration = durationInMinutes - (days * hoursInDay * MINUTES_IN_ONE_HOUR);
    int hours = ((Double) (remainingDuration.doubleValue() / MINUTES_IN_ONE_HOUR)).intValue();
    remainingDuration = remainingDuration - (hours * MINUTES_IN_ONE_HOUR);
    int minutes = remainingDuration.intValue();

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
    return stringBuilder.length() == 0 ? ("0" + MINUTE) : stringBuilder.toString();
  }

  /**
   * Return the duration in minutes.
   * <br>
   * For instance, Duration.decode(1h, 24).toMinutes() will return 60.
   */
  public long toMinutes() {
    return durationInMinutes;
  }

  /**
   * Return true if the given duration is greater than the current one.
   */
  public boolean isGreaterThan(Duration other) {
    return toMinutes() > other.toMinutes();
  }

  /**
   * Add the given duration to the current one.
   */
  public Duration add(Duration with) {
    return Duration.create(durationInMinutes + with.durationInMinutes);
  }

  /**
   * Subtract the given duration to the current one.
   */
  public Duration subtract(Duration with) {
    return Duration.create(durationInMinutes - with.durationInMinutes);
  }

  /**
   * Multiply the duration with the given factor.
   */
  public Duration multiply(int factor) {
    return Duration.create(durationInMinutes * factor);
  }

  private static IllegalArgumentException invalid(String text, @Nullable Exception e) {
    throw new IllegalArgumentException(String.format("Duration '%s' is invalid, it should use the following sample format : 2d 10h 15min", text), e);
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
