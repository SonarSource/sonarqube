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

package org.sonar.server.db.migrations.v43;

import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

class WorkDurationConvertor {

  static final long ONE_HOUR = 60L;

  static final String HOURS_IN_DAY_PROPERTY = "sonar.technicalDebt.hoursInDay";

  private Integer hoursInDay;

  private final PropertiesDao propertiesDao;

  WorkDurationConvertor(PropertiesDao propertiesDao) {
    this.propertiesDao = propertiesDao;
  }

  long createFromLong(long durationInLong) {
    checkHoursInDay();

    long durationInMinutes = 0L;

    long remainingTime = durationInLong;
    Long currentTime = remainingTime / 10000;
    if (currentTime > 0) {
      durationInMinutes = currentTime.intValue() * hoursInDay * ONE_HOUR;
      remainingTime = remainingTime - (currentTime * 10000);
    }

    currentTime = remainingTime / 100;
    if (currentTime > 0) {
      durationInMinutes += currentTime.intValue() * ONE_HOUR;
      remainingTime = remainingTime - (currentTime * 100);
    }

    currentTime = remainingTime;
    if (currentTime > 0) {
      durationInMinutes += currentTime.intValue();
    }

    return durationInMinutes;
  }

  long createFromDays(double days) {
    checkHoursInDay();

    return ((Double) (days * hoursInDay * ONE_HOUR)).longValue();
  }

  void init() {
    PropertyDto propertyDto = propertiesDao.selectGlobalProperty(HOURS_IN_DAY_PROPERTY);
    String value = propertyDto != null ? propertyDto.getValue() : "8";
    hoursInDay = Integer.valueOf(value);
    if (hoursInDay < 0) {
      throw new IllegalArgumentException(String.format("Bad value of %s: %d", HOURS_IN_DAY_PROPERTY, hoursInDay));
    }
  }

  private void checkHoursInDay() {
    if (hoursInDay == null) {
      throw new IllegalStateException("init() has not been called");
    }
  }
}
