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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.technicaldebt.TechnicalDebt;

public class TechnicalDebtConverter implements BatchComponent, ServerComponent {

  public static final String PROPERTY_HOURS_IN_DAY = "sonar.technicalDebt.hoursInDay";

  private int hoursInDay;

  public TechnicalDebtConverter(Settings settings) {
    this.hoursInDay = settings.getInt(PROPERTY_HOURS_IN_DAY);
  }

  public double toDays(WorkUnit factor) {
    if (StringUtils.equals(WorkUnit.DAYS, factor.getUnit())) {
      return factor.getValue();

    } else if (StringUtils.equals(WorkUnit.HOURS, factor.getUnit())) {
      return factor.getValue() / hoursInDay;

    } else if (StringUtils.equals(WorkUnit.MINUTES, factor.getUnit())) {
      return factor.getValue() / (hoursInDay * 60.0);

    } else {
      throw new IllegalArgumentException("Unknown remediation factor unit: " + factor.getUnit());
    }
  }

  public long toMinutes(WorkUnit factor) {
    if (StringUtils.equals(WorkUnit.DAYS, factor.getUnit())) {
      return Double.valueOf(factor.getValue() * hoursInDay * 60d).longValue();

    } else if (StringUtils.equals(WorkUnit.HOURS, factor.getUnit())) {
      return Double.valueOf(factor.getValue() * 60d).longValue();

    } else if (StringUtils.equals(WorkUnit.MINUTES, factor.getUnit())) {
      return Double.valueOf(factor.getValue()).longValue();

    } else {
      throw new IllegalArgumentException("Unknown remediation factor unit: " + factor.getUnit());
    }
  }

  public double toDays(TechnicalDebt technicalDebt) {
    double resultDays = technicalDebt.days();
    resultDays += Double.valueOf(technicalDebt.hours()) / hoursInDay;
    resultDays += Double.valueOf(technicalDebt.minutes()) / (hoursInDay * 60.0);
    return resultDays;
  }

  public TechnicalDebt fromMinutes(Long inMinutes){
    int oneHourInMinute = 60;
    int days = 0;
    int hours = 0;
    int minutes = 0;

    int oneWorkingDay = hoursInDay * oneHourInMinute;
    if (inMinutes >= oneWorkingDay) {
      Long nbDays = inMinutes / oneWorkingDay;
      days = nbDays.shortValue();
      inMinutes = inMinutes - (nbDays * oneWorkingDay);
    }

    if (inMinutes >= oneHourInMinute) {
      Long nbHours = inMinutes / oneHourInMinute;
      hours = nbHours.shortValue();
      inMinutes = inMinutes - (nbHours * oneHourInMinute);
    }

    minutes = inMinutes.shortValue();

    return TechnicalDebt.of(minutes, hours, days);
  }

}
