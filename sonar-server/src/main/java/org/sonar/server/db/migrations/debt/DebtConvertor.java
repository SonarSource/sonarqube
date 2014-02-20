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

package org.sonar.server.db.migrations.debt;

import org.sonar.api.config.Settings;

class DebtConvertor {

  static final String HOURS_IN_DAY_PROPERTY = "sonar.technicalDebt.hoursInDay";
  private final Settings settings;

  DebtConvertor(Settings settings) {
    this.settings = settings;
  }

  long createFromLong(long durationInLong) {
    int hoursInDay = hoursInDay();

    long durationInSeconds = 0;

    long remainingTime = durationInLong;
    Long currentTime = remainingTime / 10000;
    if (currentTime > 0) {
      durationInSeconds = currentTime.intValue() * hoursInDay * 3600;
      remainingTime = remainingTime - (currentTime * 10000);
    }

    currentTime = remainingTime / 100;
    if (currentTime > 0) {
      durationInSeconds += currentTime.intValue() * 3600;
      remainingTime = remainingTime - (currentTime * 100);
    }

    currentTime = remainingTime;
    if (currentTime > 0) {
      durationInSeconds += currentTime.intValue() * 60;
    }

    return durationInSeconds;
  }

  private int hoursInDay() {
    int hoursInDay = settings.getInt(HOURS_IN_DAY_PROPERTY);
    if (hoursInDay < 0) {
      throw new IllegalArgumentException(String.format("Bad value of %s: %d", HOURS_IN_DAY_PROPERTY, hoursInDay));
    }
    if (hoursInDay == 0) {
      hoursInDay = 8;
    }
    return hoursInDay;
  }

}
