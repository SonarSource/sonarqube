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

package org.sonar.api.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.Nullable;

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

  private static final short MINUTES_IN_ONE_HOUR = 60;

  private final long durationInMinutes;

  private Duration(long durationInMinutes) {
    this.durationInMinutes = durationInMinutes;
  }

  private Duration(int days, int hours, int minutes, int hoursInDay) {
    this(((long) days * hoursInDay * MINUTES_IN_ONE_HOUR) + (hours * MINUTES_IN_ONE_HOUR) + minutes);
  }

  public static Duration create(long durationInMinutes) {
    return new Duration(durationInMinutes);
  }

  public static Duration decode(String text, int hoursInDay) {
    int days = 0, hours = 0, minutes = 0;
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
    return stringBuilder.toString();
  }

  public long toMinutes() {
    return durationInMinutes;
  }

  public boolean isGreaterThan(Duration other) {
    return toMinutes() > other.toMinutes();
  }

  public Duration add(Duration with) {
    return Duration.create(durationInMinutes + with.durationInMinutes);
  }

  public Duration subtract(Duration with) {
    return Duration.create(durationInMinutes - with.durationInMinutes);
  }

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
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
