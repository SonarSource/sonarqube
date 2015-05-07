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

import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerSide;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;

import javax.annotation.CheckForNull;

import java.util.Locale;

/**
 * Used through ruby code <pre>Internal.durations</pre>
 *
 * @since 4.3
 */
@BatchSide
@ServerSide
public class Durations {

  public enum DurationFormat {
    /**
     * Display duration with only one or two members.
     * For instance, Duration.decode("1d 1h 10min", 8) will return "1d 1h" and Duration.decode("12d 5h", 8) will return "12d"
     */
    SHORT
  }

  private final Settings settings;
  private final I18n i18n;

  public Durations(Settings settings, I18n i18n) {
    this.settings = settings;
    this.i18n = i18n;
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
   * Example : decode("9d 10 h") -> Duration.encode("10d2h") (if sonar.technicalDebt.hoursInDay property is set to 8)
   */
  public Duration decode(String duration) {
    return Duration.decode(duration, hoursInDay());
  }

  /**
   * Return the string value of the Duration.
   * <br>
   * Example : encode(Duration.encode("9d 10h")) -> "10d2h" (if sonar.technicalDebt.hoursInDay property is set to 8)
   */
  public String encode(Duration duration) {
    return duration.encode(hoursInDay());
  }

  /**
   * Return the formatted work duration.
   * <br>
   * Example : format(Locale.FRENCH, Duration.encode("9d 10h"), DurationFormat.SHORT) -> 10j 2h (if sonar.technicalDebt.hoursInDay property is set to 8)
   *
   */
  public String format(Locale locale, Duration duration, DurationFormat format) {
    return format(locale, duration);
  }

  /**
   * Return the formatted work duration.
   * <br>
   * Example : format(Locale.FRENCH, Duration.encode("9d 10h"), DurationFormat.SHORT) -> 10j 2h (if sonar.technicalDebt.hoursInDay property is set to 8)
   *
   */
  public String format(Locale locale, Duration duration) {
    Long durationInMinutes = duration.toMinutes();
    if (durationInMinutes == 0) {
      return "0";
    }
    boolean isNegative = durationInMinutes < 0;
    Long absDuration = Math.abs(durationInMinutes);

    int days = ((Double) ((double) absDuration / hoursInDay() / 60)).intValue();
    Long remainingDuration = absDuration - (days * hoursInDay() * 60);
    int hours = ((Double) (remainingDuration.doubleValue() / 60)).intValue();
    remainingDuration = remainingDuration - (hours * 60);
    int minutes = remainingDuration.intValue();

    return format(locale, days, hours, minutes, isNegative);
  }

  private String format(Locale locale, int days, int hours, int minutes, boolean isNegative) {
    StringBuilder message = new StringBuilder();
    if (days > 0) {
      message.append(message(locale, "work_duration.x_days", isNegative ? -1 * days : days));
    }
    if (displayHours(days, hours)) {
      addSpaceIfNeeded(message);
      message.append(message(locale, "work_duration.x_hours", isNegative && message.length() == 0 ? -1 * hours : hours));
    }
    if (displayMinutes(days, hours, minutes)) {
      addSpaceIfNeeded(message);
      message.append(message(locale, "work_duration.x_minutes", isNegative && message.length() == 0 ? -1 * minutes : minutes));
    }
    return message.toString();
  }

  private String message(Locale locale, String key, @CheckForNull Object parameter) {
    return i18n.message(locale, key, null, parameter);
  }

  private boolean displayHours(int days, int hours) {
    return hours > 0 && days < 10;
  }

  private boolean displayMinutes(int days, int hours, int minutes) {
    return minutes > 0 && hours < 10 && days == 0;
  }

  private void addSpaceIfNeeded(StringBuilder message) {
    if (message.length() > 0) {
      message.append(" ");
    }
  }

  private int hoursInDay() {
    return settings.getInt(CoreProperties.HOURS_IN_DAY);
  }

}
