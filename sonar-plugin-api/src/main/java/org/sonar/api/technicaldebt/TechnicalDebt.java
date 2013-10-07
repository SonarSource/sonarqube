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
package org.sonar.api.technicaldebt;

import java.io.Serializable;

public class TechnicalDebt implements Serializable {

  private static final int DAY = 10000;
  private static final int HOUR = 100;
  private static final int MINUTE = 1;

  private int days;
  private int hours;
  private int minutes;

  private TechnicalDebt(int minutes, int hours, int days) {
    this.minutes = minutes;
    this.hours = hours;
    this.days = days;
  }

  private TechnicalDebt(long technicalDebtInLong) {
    long time = technicalDebtInLong;
    Long currentTime = time / DAY;
    if (currentTime > 0) {
      this.days = currentTime.intValue();
      time = time - (currentTime * DAY);
    }

    currentTime = time / HOUR;
    if (currentTime > 0) {
      this.hours = currentTime.intValue();
      time = time - (currentTime * HOUR);
    }

    currentTime = time / MINUTE;
    if (currentTime > 0) {
      this.minutes = currentTime.intValue();
    }
  }

  public static TechnicalDebt of(int minutes, int hours, int days) {
    return new TechnicalDebt(minutes, hours, days);
  }

  public static TechnicalDebt fromLong(long technicalDebtInLong) {
    return new TechnicalDebt(technicalDebtInLong);
  }

  public long toLong() {
    return days * DAY + hours * HOUR + minutes * MINUTE;
  }

  public long days() {
    return days;
  }

  public long hours() {
    return hours;
  }

  public long minutes() {
    return minutes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TechnicalDebt technicalDebt = (TechnicalDebt) o;
    if (days != technicalDebt.days) {
      return false;
    }
    if (hours != technicalDebt.hours) {
      return false;
    }
    if (minutes != technicalDebt.minutes) {
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
    return super.toString();
  }
}
