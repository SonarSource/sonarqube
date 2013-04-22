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
package org.sonar.batch.scan;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

class DurationLabel {

  private String suffixAgo = "ago";
  private String seconds = "less than a minute";
  private String minute = "about a minute";
  private String minutes = "{0} minutes";
  private String hour = "about an hour";
  private String hours = "{0} hours";
  private String day = "a day";
  private String days = "{0} days";
  private String month = "about a month";
  private String months = "{0} months";
  private String year = "about a year";
  private String years = "{0} years";

  String label(long durationInMillis) {
    double nbSeconds = durationInMillis / 1000.0;
    double nbMinutes = nbSeconds / 60;
    double nbHours = nbMinutes / 60;
    double nbDays = nbHours / 24;
    double nbYears = nbDays / 365;
    String message = getMessage(nbSeconds, nbMinutes, nbHours, nbDays, nbYears);
    return join(message, suffixAgo);
  }

  private String getMessage(double nbSeconds, double nbMinutes, double nbHours, double nbDays, double nbYears) {
    String message = MessageFormat.format(this.years, Math.floor(nbYears));
    if (nbSeconds < 45) {
      message = this.seconds;
    } else if (nbSeconds < 90) {
      message = this.minute;
    } else if (nbMinutes < 45) {
      message = MessageFormat.format(this.minutes, Math.round(nbMinutes));
    } else if (nbMinutes < 90) {
      message = this.hour;
    } else if (nbHours < 24) {
      message = MessageFormat.format(this.hours, Math.round(nbHours));
    } else if (nbHours < 48) {
      message = this.day;
    } else if (nbDays < 30) {
      message = MessageFormat.format(this.days, Math.floor(nbDays));
    } else if (nbDays < 60) {
      message = this.month;
    } else if (nbDays < 365) {
      message = MessageFormat.format(this.months, Math.floor(nbDays / 30));
    } else if (nbYears < 2) {
      message = this.year;
    }
    return message;
  }

  @VisibleForTesting
  String join(String time, String suffix) {
    StringBuilder joined = new StringBuilder();
    joined.append(time);
    if (StringUtils.isNotBlank(suffix)) {
      joined.append(' ').append(suffix);
    }
    return joined.toString();
  }

  String getSuffixAgo() {
    return suffixAgo;
  }

  String getSeconds() {
    return seconds;
  }

  String getMinute() {
    return minute;
  }

  String getMinutes() {
    return minutes;
  }

  String getHour() {
    return hour;
  }

  String getHours() {
    return hours;
  }

  String getDay() {
    return day;
  }

  String getDays() {
    return days;
  }

  String getMonth() {
    return month;
  }

  String getMonths() {
    return months;
  }

  String getYear() {
    return year;
  }

  String getYears() {
    return years;
  }

}
