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
package org.sonar.api.utils;

import java.util.Locale;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @since 4.3
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class Durations {

  private static final String MINUTES_FORMAT = "%smin";
  private static final String HOURS_FORMAT = "%sh";
  private static final String DAYS_FORMAT = "%sd";

  private static final int HOURS_IN_DAY = 8;

  /**
   * @deprecated since 6.3, only one format is available
   */
  @Deprecated
  public enum DurationFormat {
    /**
     * Display duration with only one or two members.
     * For instance, Duration.decode("1d 1h 10min", 8) will return "1d 1h" and Duration.decode("12d 5h", 8) will return "12d"
     */
    SHORT
  }

  /**
   * Create a Duration object from a number of minutes
   */
  public Duration create(long minutes) {
    return Duration.create(minutes);
  }

  /**
   * Convert the text to a Duration
   * <br>
   * Example : decode("9d 10 h") -&gt; Duration.encode("10d2h")
   * <br>
   * @throws IllegalArgumentException
   */
  public Duration decode(String duration) {
    return Duration.decode(duration, HOURS_IN_DAY);
  }

  /**
   * Return the string value of the Duration.
   * <br>
   * Example : encode(Duration.encode("9d 10h")) -&gt; "10d2h"
   */
  public String encode(Duration duration) {
    return duration.encode(HOURS_IN_DAY);
  }

  /**
   * Return the formatted work duration.
   *
   * @deprecated since 6.3 as the {@link Locale#ENGLISH} is always used. Use {@link #format(Duration)} instead
   */
  @Deprecated
  public String format(Locale locale, Duration duration, DurationFormat format) {
    return format(duration);
  }

  /**
   * Return the formatted work duration.
   * <br>
   * Example : format(Locale.FRENCH, Duration.encode("9d 10h"), DurationFormat.SHORT) -&gt; 10d 2d
   *
   * @deprecated since 6.3 as the {@link Locale#ENGLISH} is always used. Use {@link #format(Duration)} instead
   */
  @Deprecated
  public String format(Locale locale, Duration duration) {
    return format(duration);
  }

  /**
   * Return the formatted work duration using the english bundles.
   * <br>
   * Example : format(Duration.encode("9d 10h")) -&gt; 10d 2h
   *
   */
  public String format(Duration duration) {
    Long durationInMinutes = duration.toMinutes();
    if (durationInMinutes == 0) {
      return "0";
    }
    boolean isNegative = durationInMinutes < 0;
    Long absDuration = Math.abs(durationInMinutes);

    int days = ((Double) ((double) absDuration / HOURS_IN_DAY / 60)).intValue();
    Long remainingDuration = absDuration - (days * HOURS_IN_DAY * 60);
    int hours = ((Double) (remainingDuration.doubleValue() / 60)).intValue();
    remainingDuration = remainingDuration - (hours * 60);
    int minutes = remainingDuration.intValue();

    return format(days, hours, minutes, isNegative);
  }

  private static String format(int days, int hours, int minutes, boolean isNegative) {
    StringBuilder message = new StringBuilder();
    if (days > 0) {
      message.append(String.format(DAYS_FORMAT, isNegative ? (-1 * days) : days));
    }
    if (displayHours(days, hours)) {
      addSpaceIfNeeded(message);
      message.append(String.format(HOURS_FORMAT, isNegative && message.length() == 0 ? (-1 * hours) : hours));
    }
    if (displayMinutes(days, hours, minutes)) {
      addSpaceIfNeeded(message);
      message.append(String.format(MINUTES_FORMAT, isNegative && message.length() == 0 ? (-1 * minutes) : minutes));
    }
    return message.toString();
  }

  private static boolean displayHours(int days, int hours) {
    return hours > 0 && days < 10;
  }

  private static boolean displayMinutes(int days, int hours, int minutes) {
    return minutes > 0 && hours < 10 && days == 0;
  }

  private static void addSpaceIfNeeded(StringBuilder message) {
    if (message.length() > 0) {
      message.append(" ");
    }
  }

}
