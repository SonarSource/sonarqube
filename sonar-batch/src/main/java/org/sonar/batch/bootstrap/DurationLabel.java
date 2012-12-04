/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import java.text.MessageFormat;

public class DurationLabel {

  private String prefixAgo = null;
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

  public String label(long durationInMillis) {
    double seconds = durationInMillis / 1000;
    double minutes = seconds / 60;
    double hours = minutes / 60;
    double days = hours / 24;
    double years = days / 365;

    String time = MessageFormat.format(this.years, Math.floor(years));
    if (seconds < 45) {
      time = this.seconds;
    } else if (seconds < 90) {
      time = this.minute;
    } else if (minutes < 45) {
      time = MessageFormat.format(this.minutes, Math.round(minutes));
    } else if (minutes < 90) {
      time = this.hour;
    } else if (hours < 24) {
      time = MessageFormat.format(this.hours, Math.round(hours));
    } else if (hours < 48) {
      time = this.day;
    } else if (days < 30) {
      time = MessageFormat.format(this.days, Math.floor(days));
    } else if (days < 60) {
      time = this.month;
    } else if (days < 365) {
      time = MessageFormat.format(this.months, Math.floor(days / 30));
    } else if (years < 2) {
      time = this.year;
    }

    return join(prefixAgo, time, suffixAgo);
  }

  public String join(String prefix, String time, String suffix) {
    StringBuilder joined = new StringBuilder();
    if (prefix != null && prefix.length() > 0) {
      joined.append(prefix).append(' ');
    }
    joined.append(time);
    if (suffix != null && suffix.length() > 0) {
      joined.append(' ').append(suffix);
    }
    return joined.toString();
  }

  public String getPrefixAgo() {
    return prefixAgo;
  }

  public String getSuffixAgo() {
    return suffixAgo;
  }

  public String getSeconds() {
    return seconds;
  }

  public String getMinute() {
    return minute;
  }

  public String getMinutes() {
    return minutes;
  }

  public String getHour() {
    return hour;
  }

  public String getHours() {
    return hours;
  }

  public String getDay() {
    return day;
  }

  public String getDays() {
    return days;
  }

  public String getMonth() {
    return month;
  }

  public String getMonths() {
    return months;
  }

  public String getYear() {
    return year;
  }

  public String getYears() {
    return years;
  }

}
