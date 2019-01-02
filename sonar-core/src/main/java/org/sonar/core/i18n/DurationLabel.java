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
package org.sonar.core.i18n;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class DurationLabel {

  private DurationLabel() {
    // Utility class
  }

  public static Result label(long durationInMillis) {
    double nbSeconds = durationInMillis / 1000.0;
    double nbMinutes = nbSeconds / 60;
    double nbHours = nbMinutes / 60;
    double nbDays = nbHours / 24;
    double nbYears = nbDays / 365;
    return getMessage(nbSeconds, nbMinutes, nbHours, nbDays, nbYears);
  }

  private static Result getMessage(double nbSeconds, double nbMinutes, double nbHours, double nbDays, double nbYears) {
    if (nbSeconds < 45) {
      return message("seconds");
    } else if (nbSeconds < 90) {
      return message("minute");
    } else if (nbMinutes < 45) {
      return message("minutes", Math.round(nbMinutes));
    } else if (nbMinutes < 90) {
      return message("hour");
    } else if (nbHours < 24) {
      return message("hours", Math.round(nbHours));
    } else if (nbHours < 48) {
      return message("day");
    } else if (nbDays < 30) {
      return message("days", (long) (Math.floor(nbDays)));
    } else if (nbDays < 60) {
      return message("month");
    } else if (nbDays < 365) {
      return message("months", (long) (Math.floor(nbDays / 30)));
    } else if (nbYears < 2) {
      return message("year");
    }
    return message("years", (long) (Math.floor(nbYears)));
  }

  private static Result message(String key) {
    return message(key, null);
  }

  private static Result message(String key, @Nullable Long value) {
    String durationPrefix = "duration.";
    return new Result(durationPrefix + key, value);
  }

  static class Result {
    private String key;
    private Long value;

    public Result(String key, @Nullable Long value) {
      this.key = key;
      this.value = value;
    }

    public String key() {
      return key;
    }

    @CheckForNull
    public Long value() {
      return value;
    }
  }

}
