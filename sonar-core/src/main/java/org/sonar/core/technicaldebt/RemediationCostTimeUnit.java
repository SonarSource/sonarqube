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
package org.sonar.core.technicaldebt;

public class RemediationCostTimeUnit {

  private long days;
  private long hours;
  private long minutes;

  private static final int ONE_HOUR_IN_MINUTE = 60;

  private RemediationCostTimeUnit(long input, int hoursInDay) {
    int oneWorkingDay = hoursInDay * ONE_HOUR_IN_MINUTE;
    if (input >= oneWorkingDay) {
      long nbDays = input / oneWorkingDay;
      this.days = nbDays;
      input = input - (nbDays * oneWorkingDay);
    }

    if (input >= ONE_HOUR_IN_MINUTE) {
      long nbHours = input / ONE_HOUR_IN_MINUTE;
      this.hours = nbHours;
      input = input - (nbHours * ONE_HOUR_IN_MINUTE);
    }

    this.minutes = input;
  }

  public static RemediationCostTimeUnit of(Long time, int hoursInDay) {
    return new RemediationCostTimeUnit(time, hoursInDay);
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

}
